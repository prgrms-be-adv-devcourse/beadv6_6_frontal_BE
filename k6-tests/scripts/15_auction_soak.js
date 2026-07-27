/**
 * 관점: 안전 부하를 오래 유지할 때 Auction 서비스의 자원 사용과 지연이 누적되는지 본다.
 * 이유: 짧은 Stress에서 보이지 않는 메모리, 커넥션, 스레드, 이벤트 적체가 운영 장애를 만든다.
 * 개선 대상: JVM 메모리, Hikari 반환, WebSocket/Kafka 처리, 스케줄러와 NAS 장시간 안정성.
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
const users = new SharedArray('soak auction users', () => JSON.parse(open(tokenFile)));
const soakVus = Number(__ENV.SOAK_VUS || 10);
const soakDuration = __ENV.SOAK_DURATION || '10m';

export const options = {
  scenarios: {
    auction_soak: {
      executor: 'constant-vus',
      vus: soakVus,
      duration: soakDuration,
      gracefulStop: '15s',
      tags: { scenario_name: 'auction_soak' },
    },
  },
  thresholds: {
    auction_auth_failures: ['rate==0'],
    auction_system_failures: [
      { threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '1m' },
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
    name: 'Auction Soak',
    viewpoint: '안전 부하를 10분에서 60분 이상 유지하는 장시간 안정성',
    risk: '시간이 지나며 증가하는 지연, 메모리·연결 누수, 이벤트 적체',
    improvement: 'JVM·Hikari·이벤트 처리 리소스의 반환과 운영 설정 개선',
  });

  const response = getAuction(config.baseUrl, config.auctionId, null, 'soak_setup');
  if (response.status !== 200) throw new Error(`Auction detail preflight failed: ${response.status}`);
  const auction = safeJson(response);
  assertAuctionCanRun(auction, Number(__ENV.MIN_REMAINING_SECONDS || 900));
  validateUsers(users, soakVus, auction.sellerId);

  return {
    ...config,
    currentBid: Number(auction.currentBid),
    minIncrement: Number(auction.minIncrement),
    maxVus: soakVus,
  };
}
export default function (data) {
  const selector = (__VU + __ITER) % 10;

  // 운영 화면에 가까운 읽기 80%, 입찰 20% 비율이다.
  if (selector < 8) {
    const endpointSelector = selector % 3;
    const url = endpointSelector === 0
      ? `${data.baseUrl}/api/v1/auctions?status=LIVE&sort=ending&page=0&size=20`
      : endpointSelector === 1
        ? `${data.baseUrl}/api/v1/auctions/${data.auctionId}`
        : `${data.baseUrl}/api/v1/auctions/${data.auctionId}/bids?page=0&size=20`;
    const endpoint = endpointSelector === 0 ? 'auction_feed' : endpointSelector === 1 ? 'auction_detail' : 'bid_history';
    const response = http.get(url, publicParams(endpoint, 'soak'));
    recordReadResponse(response);
  } else {
    const user = users[__VU - 1];
    const amount = candidateAmount(data.currentBid, data.minIncrement, data.maxVus);
    placeBid(data.baseUrl, data.auctionId, amount, user.token, 'soak');
  }

  sleep(Number(__ENV.THINK_TIME_SECONDS || 1));
}

export function teardown(data) {
  const consistent = verifyConsistency(data.baseUrl, data.auctionId, 'soak_teardown');
  console.log(`Soak completed: runId=${data.runId}, vus=${soakVus}, duration=${soakDuration}, consistency=${consistent}`);
}
