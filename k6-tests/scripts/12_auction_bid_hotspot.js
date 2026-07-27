/**
 * 관점: 모든 사용자가 동일 Auction 행에 입찰할 때 정합성과 락 지연을 측정한다.
 * 이유: 평균 트래픽보다 종료 직전 인기 경매의 한 행 경합이 실제 위험이다.
 * 개선 대상: 비관적 락 범위, 트랜잭션 길이, Hikari 풀, 낙관적 락 또는 조건부 UPDATE 전환 판단.
 */
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

import {
  assertAuctionCanRun,
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
const users = new SharedArray('hotspot auction users', () => JSON.parse(open(tokenFile)));
const profile = __ENV.PROFILE || 'smoke';
const profiles = {
  smoke: {
    maxVus: 3,
    minimumRemainingSeconds: 30,
    stages: [
      { duration: '5s', target: 3 },
      { duration: '10s', target: 3 },
      { duration: '5s', target: 0 },
    ],
  },
  hotspot: {
    maxVus: 20,
    minimumRemainingSeconds: 240,
    stages: [
      { duration: '15s', target: 5 },
      { duration: '1m', target: 5 },
      { duration: '15s', target: 10 },
      { duration: '1m', target: 10 },
      { duration: '15s', target: 20 },
      { duration: '1m', target: 20 },
      { duration: '15s', target: 0 },
    ],
  },
};

if (!profiles[profile]) throw new Error(`Unknown PROFILE=${profile}. Use smoke or hotspot.`);
const selected = profiles[profile];

export const options = {
  scenarios: {
    same_auction_bids: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: selected.stages,
      gracefulRampDown: '10s',
      tags: { scenario_name: 'auction_bid_hotspot' },
    },
  },
  thresholds: {
    auction_auth_failures: ['rate==0'],
    auction_system_failures: ['rate<0.01'],
    auction_consistency_failures: ['rate==0'],
    auction_bid_latency: ['p(95)<1000', 'p(99)<3000'],
  },
};

export function setup() {
  const config = configuration({ write: true });
  printTestIntent({
    name: 'Auction Bid Hotspot',
    viewpoint: '동일 Auction 행에 집중되는 동시 입찰',
    risk: '락 대기 급증, timeout·5xx, 유실 갱신, Bid/Auction 집계 불일치',
    improvement: '비관적 락 유지·보완 또는 낙관적 락/조건부 UPDATE 전환을 실측으로 판단',
  });

  const response = getAuction(config.baseUrl, config.auctionId, null, 'hotspot_setup');
  if (response.status !== 200) throw new Error(`Auction detail preflight failed: ${response.status}`);
  const auction = safeJson(response);
  assertAuctionCanRun(auction, selected.minimumRemainingSeconds);
  validateUsers(users, selected.maxVus, auction.sellerId);

  return {
    ...config,
    currentBid: Number(auction.currentBid),
    minIncrement: Number(auction.minIncrement),
    initialBidCount: Number(auction.bidCount),
    maxVus: selected.maxVus,
  };
}
export default function (data) {
  const user = users[__VU - 1];
  const amount = candidateAmount(data.currentBid, data.minIncrement, data.maxVus);
  const response = placeBid(data.baseUrl, data.auctionId, amount, user.token, 'bid_hotspot');

  if (response.status === 201) {
    check(response, {
      'accepted bid response contains requested amount': (r) => Number(safeJson(r)?.amount) === amount,
      'accepted bid response currentBid equals requested amount': (r) => Number(safeJson(r)?.currentBid) === amount,
    }, { viewpoint: 'bid_hotspot' });
  }

  // 로컬 PC가 무제한 루프로 먼저 포화되지 않도록 짧은 사용자 간격을 둔다.
  sleep(Number(__ENV.BID_THINK_TIME_SECONDS || 0.2));
}

export function teardown(data) {
  const passed = verifyConsistency(data.baseUrl, data.auctionId, 'bid_hotspot_teardown');
  console.log(`Hotspot completed: runId=${data.runId}, initialBidCount=${data.initialBidCount}, consistency=${passed}`);
}
