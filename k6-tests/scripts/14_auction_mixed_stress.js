/**
 * 관점: 조회와 입찰이 섞인 상태에서 Auction 시스템의 최초 한계 지점을 찾는다.
 * 이유: 평균 응답만으로는 Pod, Hikari, DB 락, NAS 중 어디서 무너지는지 알 수 없다.
 * 개선 대상: 확인된 최초 병목에 따라 Pod 자원, 풀 크기, 쿼리, 락 전략을 선택적으로 개선.
 */
import http from 'k6/http';
import { sleep } from 'k6';
import { SharedArray } from 'k6/data';

import {
  assertAuctionCanRun,
  candidateAmount,
  configuration,
  getAuction,
  placeBid,
  printTestIntent,
  publicParams,
  recordReadResponse,
  safeJson,
  validateUsers,
  verifyConsistency,
} from './lib/auction-test-utils.js';

const tokenFile = __ENV.TOKENS_FILE || '../data/auction-users.example.json';
const users = new SharedArray('stress auction users', () => JSON.parse(open(tokenFile)));
const profile = __ENV.PROFILE || 'smoke';
const profiles = {
  smoke: {
    maxVus: 10,
    minimumRemainingSeconds: 90,
    stages: [
      { duration: '10s', target: 5 },
      { duration: '20s', target: 5 },
      { duration: '10s', target: 10 },
      { duration: '20s', target: 10 },
      { duration: '10s', target: 0 },
    ],
  },
  stress: {
    maxVus: 100,
    minimumRemainingSeconds: 750,
    stages: [
      { duration: '30s', target: 10 },
      { duration: '2m', target: 10 },
      { duration: '30s', target: 25 },
      { duration: '2m', target: 25 },
      { duration: '30s', target: 50 },
      { duration: '2m', target: 50 },
      { duration: '30s', target: 100 },
      { duration: '2m', target: 100 },
      { duration: '30s', target: 0 },
    ],
  },
};

if (!profiles[profile]) throw new Error(`Unknown PROFILE=${profile}. Use smoke or stress.`);
const selected = profiles[profile];

export const options = {
  scenarios: {
    mixed_auction_stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: selected.stages,
      gracefulRampDown: '15s',
      tags: { scenario_name: 'auction_mixed_stress' },
    },
  },
  thresholds: {
    auction_auth_failures: ['rate==0'],
    auction_system_failures: [
      { threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' },
    ],
    auction_read_failures: ['rate<0.01'],
    auction_consistency_failures: ['rate==0'],
    auction_read_latency: ['p(95)<1000', 'p(99)<2500'],
    auction_bid_latency: ['p(95)<2000', 'p(99)<5000'],
  },
};

export function setup() {
  const config = configuration({ write: true, destructive: true });
  printTestIntent({
    name: 'Auction Mixed Stress',
    viewpoint: '외부 PC에서 조회 70%와 입찰 30%가 섞인 단계적 고부하',
    risk: '5xx·timeout과 p95 급증, Pod 또는 Hikari 포화, DB 락·NAS 지연',
    improvement: '최초 병목만 개선하고 같은 프로필로 전후 결과를 재측정',
  });

  const response = getAuction(config.baseUrl, config.auctionId, null, 'stress_setup');
  if (response.status !== 200) throw new Error(`Auction detail preflight failed: ${response.status}`);
  const auction = safeJson(response);
  assertAuctionCanRun(auction, selected.minimumRemainingSeconds);
  validateUsers(users, selected.maxVus, auction.sellerId);

  return {
    ...config,
    currentBid: Number(auction.currentBid),
    minIncrement: Number(auction.minIncrement),
    maxVus: selected.maxVus,
  };
}
export default function (data) {
  const selector = (__VU + __ITER) % 10;

  if (selector < 7) {
    let response;
    if (selector < 2) {
      response = http.get(
        `${data.baseUrl}/api/v1/auctions?status=LIVE&sort=ending&page=0&size=20`,
        publicParams('auction_feed', 'mixed_stress'),
      );
    } else if (selector < 6) {
      response = http.get(
        `${data.baseUrl}/api/v1/auctions/${data.auctionId}`,
        publicParams('auction_detail', 'mixed_stress'),
      );
    } else {
      response = http.get(
        `${data.baseUrl}/api/v1/auctions/${data.auctionId}/bids?page=0&size=20`,
        publicParams('bid_history', 'mixed_stress'),
      );
    }
    recordReadResponse(response);
  } else {
    const user = users[__VU - 1];
    const amount = candidateAmount(data.currentBid, data.minIncrement, data.maxVus);
    placeBid(data.baseUrl, data.auctionId, amount, user.token, 'mixed_stress');
  }

  sleep(Number(__ENV.THINK_TIME_SECONDS || 0.2));
}

export function teardown(data) {
  const consistent = verifyConsistency(data.baseUrl, data.auctionId, 'mixed_stress_teardown');
  console.log(`Mixed stress completed: runId=${data.runId}, profile=${profile}, consistency=${consistent}`);
}
