/**
 * 관점: 경매 종료 시각 전후에 입찰이 급증할 때 종료와 입찰의 순서를 검증한다.
 * 이유: status가 아직 LIVE인 스케줄러 지연 구간에서 종료 후 입찰이 승인될 수 있다.
 * 개선 대상: 잠금 후 endsAt 재검증, 판매자/스케줄러 종료 락, 경매별 종료 트랜잭션.
 */
import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate } from 'k6/metrics';

import {
  candidateAmount,
  configuration,
  getAuction,
  placeBid,
  printTestIntent,
  safeJson,
  validateUsers,
  verifyConsistency,
} from './lib/auction-test-utils.js';

const tokenFile = __ENV.TOKENS_FILE || '../data/auction-users.example.json';
const users = new SharedArray('closing spike auction users', () => JSON.parse(open(tokenFile)));
const maxVus = Number(__ENV.SPIKE_MAX_VUS || 30);
const afterEndAccepted = new Counter('auction_after_end_accepted');
const closingStateFailures = new Rate('auction_closing_state_failures');

export const options = {
  scenarios: {
    closing_spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 2 },
        { duration: '20s', target: 2 },
        { duration: '10s', target: maxVus },
        { duration: '30s', target: maxVus },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
      tags: { scenario_name: 'auction_closing_spike' },
    },
  },
  thresholds: {
    auction_auth_failures: ['rate==0'],
    auction_system_failures: ['rate<0.01'],
    auction_consistency_failures: ['rate==0'],
    auction_closing_state_failures: ['rate==0'],
    auction_after_end_accepted: ['count==0'],
    auction_bid_latency: ['p(95)<1500', 'p(99)<4000'],
  },
};

export function setup() {
  const config = configuration({ write: true });
  printTestIntent({
    name: 'Auction Closing-time Spike',
    viewpoint: '종료 시각 경계의 정확성과 순간 입찰 급증',
    risk: 'endsAt 이후 입찰 승인, 종료·입찰 경쟁, 스케줄러 지연과 락 대기',
    improvement: '서버/DB 시각 검증과 모든 종료 경로의 동일 잠금 규칙 적용',
  });

  const response = getAuction(config.baseUrl, config.auctionId, null, 'closing_spike_setup');
  if (response.status !== 200) throw new Error(`Auction detail preflight failed: ${response.status}`);
  const auction = safeJson(response);
  if (auction?.status !== 'LIVE') throw new Error(`Auction must be LIVE; status=${auction?.status}`);

  const remainingSeconds = (Date.parse(auction.endsAt) - Date.now()) / 1000;
  const minimum = Number(__ENV.MIN_END_SECONDS || 35);
  const maximum = Number(__ENV.MAX_END_SECONDS || 75);
  if (remainingSeconds < minimum || remainingSeconds > maximum) {
    throw new Error(`Auction must end in ${minimum}~${maximum}s; actual=${remainingSeconds.toFixed(1)}s.`);
  }
  validateUsers(users, maxVus, auction.sellerId);
  afterEndAccepted.add(0);

  return {
    ...config,
    currentBid: Number(auction.currentBid),
    minIncrement: Number(auction.minIncrement),
    endsAtMillis: Date.parse(auction.endsAt),
    maxVus,
  };
}
export default function (data) {
  const user = users[__VU - 1];
  const amount = candidateAmount(data.currentBid, data.minIncrement, data.maxVus);
  const requestStartedAt = Date.now();
  const response = placeBid(data.baseUrl, data.auctionId, amount, user.token, 'closing_spike');

  // PC와 서버의 시계 차이 및 경계 요청을 피하기 위해 종료 2초 후 시작 요청만 위반으로 본다.
  const toleranceMs = Number(__ENV.CLOCK_SKEW_TOLERANCE_MS || 2000);
  const definitelyAfterEnd = requestStartedAt > data.endsAtMillis + toleranceMs;
  afterEndAccepted.add(definitelyAfterEnd && response.status === 201 ? 1 : 0, { viewpoint: 'closing_spike' });
  sleep(Number(__ENV.BID_THINK_TIME_SECONDS || 0.2));
}

export function teardown(data) {
  const detailResponse = getAuction(data.baseUrl, data.auctionId, null, 'closing_spike_teardown');
  const auction = safeJson(detailResponse);
  const ended = detailResponse.status === 200 && auction?.status === 'ENDED';
  closingStateFailures.add(!ended, { viewpoint: 'closing_spike_teardown' });
  if (!ended) console.error(`Auction was not ENDED at teardown: status=${auction?.status}`);

  const consistent = verifyConsistency(data.baseUrl, data.auctionId, 'closing_spike_teardown');
  console.log(`Closing spike completed: runId=${data.runId}, ended=${ended}, consistency=${consistent}`);
}
