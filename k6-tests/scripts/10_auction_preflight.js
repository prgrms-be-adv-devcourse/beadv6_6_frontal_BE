/**
 * 관점: AWS 배포·API 계약·인증 경로가 부하 테스트 전에 올바른지 검증한다.
 * 이유: 경로 또는 JWT 설정 오류를 성능 문제로 오해하면 이후 측정이 모두 무효가 된다.
 * 개선 대상: API Gateway optional-auth 규칙, JWT 전달, Auction 테스트 데이터 준비 절차.
 */
import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate } from 'k6/metrics';

import {
  authParams,
  configuration,
  getAuction,
  placeBid,
  printTestIntent,
  publicParams,
  recordReadResponse,
  resolveAuthUsers,
  safeJson,
  validateUsers,
  verifyConsistency,
} from './lib/auction-test-utils.js';

const authMode = __ENV.AUTH_MODE || 'token';
const authFile = authMode === 'credentials'
  ? (__ENV.CREDENTIALS_FILE || '../data/auction-users.credentials.example.json')
  : (__ENV.TOKENS_FILE || '../data/auction-users.example.json');
const authEntries = new SharedArray('preflight auction auth entries', () => JSON.parse(open(authFile)));
const preflightFailures = new Rate('auction_preflight_failures');

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    auction_preflight_failures: ['rate==0'],
    auction_system_failures: ['rate==0'],
    auction_auth_failures: ['rate==0'],
    auction_consistency_failures: ['rate==0'],
  },
};

function verify(name, response, predicate) {
  const passed = check(response, { [name]: predicate }, { viewpoint: 'preflight' });
  preflightFailures.add(!passed, { check_name: name });
  if (!passed) {
    console.error(`${name} failed: status=${response.status}, body=${String(response.body).slice(0, 300)}`);
  }
  return passed;
}
export function setup() {
  const config = configuration({ write: true });
  printTestIntent({
    name: 'Auction AWS Preflight',
    viewpoint: '배포·API 계약·인증 정확성',
    risk: '잘못된 경로, 공개 조회 차단, JWT 미전달, 부적절한 테스트 데이터',
    improvement: 'Gateway 인증 규칙과 API 계약을 먼저 고쳐 성능 결과의 전제 조건을 확보',
  });

  const auctionResponse = getAuction(config.baseUrl, config.auctionId, null, 'preflight_setup');
  if (auctionResponse.status !== 200) throw new Error(`Auction detail preflight failed: ${auctionResponse.status}`);
  const auction = safeJson(auctionResponse);
  if (!auction || auction.status !== 'LIVE') throw new Error(`Preflight auction must be LIVE; status=${auction?.status}`);

  const users = resolveAuthUsers(config.baseUrl, authEntries, authMode);
  validateUsers(users, 1, auction.sellerId);
  return { ...config, auction, users };
}

export default function (data) {
  const feedResponse = http.get(
    `${data.baseUrl}/api/v1/auctions?status=LIVE&page=0&size=5`,
    publicParams('auction_feed', 'preflight'),
  );
  recordReadResponse(feedResponse);
  verify('public auction feed returns 200', feedResponse, (r) => r.status === 200);

  const publicDetailResponse = getAuction(data.baseUrl, data.auctionId, null, 'preflight');
  if (!verify('public auction detail returns 200', publicDetailResponse, (r) => r.status === 200)) return;

  const auction = safeJson(publicDetailResponse);
  if (!auction || auction.status !== 'LIVE') {
    preflightFailures.add(true, { check_name: 'auction_is_live' });
    throw new Error(`Preflight auction must be LIVE; status=${auction?.status}`);
  }
  const minimumBid = Number(auction.currentBid) + Number(auction.minIncrement);
  const unauthenticatedBid = http.post(
    `${data.baseUrl}/api/v1/auctions/${data.auctionId}/bids`,
    JSON.stringify({ amount: minimumBid }),
    {
      ...publicParams('place_bid_without_token', 'preflight'),
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    },
  );
  verify('bid without JWT returns 401', unauthenticatedBid, (r) => r.status === 401);

  const user = data.users[0];
  const authenticatedDetail = http.get(
    `${data.baseUrl}/api/v1/auctions/${data.auctionId}`,
    authParams(user.token, 'auction_detail_with_token', 'preflight'),
  );
  recordReadResponse(authenticatedDetail);
  verify('authenticated auction detail returns 200', authenticatedDetail, (r) => r.status === 200);

  const bidResponse = placeBid(
    data.baseUrl,
    data.auctionId,
    minimumBid,
    user.token,
    'preflight',
  );
  verify('valid minimum bid returns 201', bidResponse, (r) => r.status === 201);

  const bidResult = safeJson(bidResponse);
  if (bidResponse.status === 201) {
    const bodyPassed = check(bidResult, {
      'bid response currentBid equals requested amount': (body) => Number(body?.currentBid) === minimumBid,
      'bid response has bidId': (body) => Number(body?.bidId) > 0,
    }, { viewpoint: 'preflight' });
    preflightFailures.add(!bodyPassed, { check_name: 'bid_response_contract' });
  }

  const consistent = verifyConsistency(data.baseUrl, data.auctionId, 'preflight');
  preflightFailures.add(!consistent, { check_name: 'post_bid_consistency' });
  console.log(`Preflight completed: runId=${data.runId}, auctionId=${data.auctionId}, authMode=${authMode}`);
}
