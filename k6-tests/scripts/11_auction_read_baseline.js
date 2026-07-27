/**
 * 관점: 로그인하지 않은 사용자가 메인과 상세에서 느끼는 AWS E2E 조회 성능을 측정한다.
 * 이유: 조회 부하를 입찰 락 문제와 분리해야 느린 원인을 올바르게 찾을 수 있다.
 * 개선 대상: 조회 쿼리와 인덱스, 페이징, Gateway 조합, 캐시 및 네트워크 구간.
 */
import http from 'k6/http';
import { sleep } from 'k6';

import {
  assertAuctionCanRun,
  configuration,
  getAuction,
  printTestIntent,
  publicParams,
  recordReadResponse,
  safeJson,
} from './lib/auction-test-utils.js';

const profile = __ENV.PROFILE || 'baseline';
const profiles = {
  smoke: [
    { duration: '5s', target: 1 },
    { duration: '5s', target: 1 },
    { duration: '2s', target: 0 },
  ],
  baseline: [
    { duration: '1m', target: 5 },
    { duration: '3m', target: 5 },
    { duration: '2m', target: 10 },
    { duration: '1m', target: 0 },
  ],
};

if (!profiles[profile]) throw new Error(`Unknown PROFILE=${profile}. Use smoke or baseline.`);

export const options = {
  scenarios: {
    auction_reads: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: profiles[profile],
      gracefulRampDown: '10s',
      tags: { scenario_name: 'auction_read_baseline' },
    },
  },
  thresholds: {
    auction_read_failures: ['rate<0.01'],
    auction_system_failures: ['rate<0.01'],
    auction_read_latency: ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{endpoint:auction_detail}': ['p(95)<500'],
  },
};

export function setup() {
  const config = configuration();
  printTestIntent({
    name: 'Auction Read Baseline',
    viewpoint: 'PC에서 AWS에 접속하는 비로그인 사용자 E2E 조회',
    risk: '목록·상세·입찰내역의 기본 지연, Gateway 조합 비용, 조회 5xx',
    improvement: '입찰 락과 무관한 조회 병목을 찾아 쿼리·인덱스·캐시를 우선 개선',
  });
  const response = getAuction(config.baseUrl, config.auctionId, null, 'read_baseline_setup');
  if (response.status !== 200) throw new Error(`Auction detail preflight failed: ${response.status}`);
  assertAuctionCanRun(safeJson(response), profile === 'smoke' ? 15 : 450);
  return config;
}
export default function (config) {
  const selector = (__VU + __ITER) % 10;
  let response;

  // 실제 화면 접근 비율: 목록 30%, 상세 50%, 입찰내역 20%
  if (selector < 3) {
    response = http.get(
      `${config.baseUrl}/api/v1/auctions?status=LIVE&sort=ending&page=0&size=20`,
      publicParams('auction_feed', 'read_baseline'),
    );
  } else if (selector < 8) {
    response = http.get(
      `${config.baseUrl}/api/v1/auctions/${config.auctionId}`,
      publicParams('auction_detail', 'read_baseline'),
    );
  } else {
    response = http.get(
      `${config.baseUrl}/api/v1/auctions/${config.auctionId}/bids?page=0&size=20`,
      publicParams('bid_history', 'read_baseline'),
    );
  }

  recordReadResponse(response);
  sleep(Number(__ENV.THINK_TIME_SECONDS || 0.5));
}
