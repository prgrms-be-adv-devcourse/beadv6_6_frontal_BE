import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const acceptedBids = new Counter('auction_accepted_bids');
export const businessRejections = new Counter('auction_business_rejections');
export const authFailures = new Rate('auction_auth_failures');
export const systemFailures = new Rate('auction_system_failures');
export const readFailures = new Rate('auction_read_failures');
export const consistencyFailures = new Rate('auction_consistency_failures');
export const bidLatency = new Trend('auction_bid_latency', true);
export const readLatency = new Trend('auction_read_latency', true);

export function configuration({ write = false, destructive = false } = {}) {
  const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
  const auctionId = __ENV.AUCTION_ID || '';
  const runId = __ENV.RUN_ID || `local-${Date.now()}`;

  if (!baseUrl) {
    throw new Error('BASE_URL is required. Use the public AWS API Gateway URL.');
  }
  if (!baseUrl.startsWith('https://') && __ENV.ALLOW_INSECURE_HTTP !== 'true') {
    throw new Error('BASE_URL must use HTTPS. Set ALLOW_INSECURE_HTTP=true only for an approved environment.');
  }
  if (!auctionId) {
    throw new Error('AUCTION_ID is required.');
  }
  if (!auctionId.startsWith('A-K6-') && __ENV.ALLOW_NON_TEST_AUCTION !== 'true') {
    throw new Error('AUCTION_ID must start with A-K6-. This prevents writes to normal auctions.');
  }
  if (write && __ENV.ALLOW_AUCTION_WRITES !== 'true') {
    throw new Error('Write test blocked. Set ALLOW_AUCTION_WRITES=true after test-data approval.');
  }
  if (destructive && __ENV.ALLOW_HIGH_LOAD !== 'true') {
    throw new Error('High-load test blocked. Set ALLOW_HIGH_LOAD=true after team approval.');
  }

  return { baseUrl, auctionId, runId };
}
export function printTestIntent({ name, viewpoint, risk, improvement }) {
  console.log('============================================================');
  console.log(name);
  console.log(`관점: ${viewpoint}`);
  console.log(`발견하려는 위험: ${risk}`);
  console.log(`개선 판단: ${improvement}`);
  console.log('============================================================');
}

export function validateUsers(users, minimumCount, sellerId) {
  if (!Array.isArray(users) || users.length < minimumCount) {
    throw new Error(`At least ${minimumCount} token entries are required; received ${users?.length || 0}.`);
  }

  const memberIds = new Set();
  for (const user of users) {
    if (!user.memberId || !user.token || user.token.includes('REPLACE_')) {
      throw new Error('Token entries require a real memberId and token. Do not run with the example placeholders.');
    }
    if (sellerId !== undefined && Number(user.memberId) === Number(sellerId)) {
      throw new Error(`Seller memberId ${sellerId} must not be included in the bidder token pool.`);
    }
    memberIds.add(String(user.memberId));
  }

  if (memberIds.size < minimumCount) {
    throw new Error(`At least ${minimumCount} distinct bidder memberIds are required.`);
  }
}

export function resolveAuthUsers(baseUrl, entries, authMode) {
  if (authMode === 'token') return entries;
  if (authMode !== 'credentials') {
    throw new Error(`Unknown AUTH_MODE=${authMode}. Use token or credentials.`);
  }

  return entries.map((credential, index) => {
    if (!credential?.email || !credential?.password
      || credential.email.includes('REPLACE_') || credential.password.includes('REPLACE_')) {
      throw new Error(`Credential entry ${index} requires a real email and password.`);
    }

    const loginResponse = http.post(
      `${baseUrl}/api/members/login`,
      JSON.stringify({ email: credential.email, password: credential.password }),
      {
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        tags: { endpoint: 'auth_login', viewpoint: 'setup', test_scope: 'authentication' },
        timeout: __ENV.REQUEST_TIMEOUT || '10s',
      },
    );
    const loginBody = safeJson(loginResponse);
    if (loginResponse.status !== 200 || !loginBody?.accessToken) {
      authFailures.add(true, { endpoint: 'auth_login' });
      throw new Error(`Login failed for credential entry ${index}: status=${loginResponse.status}`);
    }

    const meResponse = http.get(
      `${baseUrl}/api/members/me`,
      authParams(loginBody.accessToken, 'member_me', 'setup'),
    );
    const member = safeJson(meResponse);
    if (meResponse.status !== 200 || !Number.isSafeInteger(Number(member?.id))) {
      authFailures.add(true, { endpoint: 'member_me' });
      throw new Error(`Member lookup failed for credential entry ${index}: status=${meResponse.status}`);
    }

    authFailures.add(false, { endpoint: 'auth_setup' });
    return { memberId: Number(member.id), token: loginBody.accessToken };
  });
}

export function publicParams(endpoint, viewpoint) {
  return {
    headers: { Accept: 'application/json' },
    tags: { endpoint, viewpoint, test_scope: 'auction' },
    timeout: __ENV.REQUEST_TIMEOUT || '10s',
  };
}

export function authParams(token, endpoint, viewpoint) {
  return {
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { endpoint, viewpoint, test_scope: 'auction' },
    timeout: __ENV.REQUEST_TIMEOUT || '10s',
  };
}

export function safeJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

export function getAuction(baseUrl, auctionId, token, viewpoint = 'consistency') {
  const params = token
    ? authParams(token, 'auction_detail', viewpoint)
    : publicParams('auction_detail', viewpoint);
  const response = http.get(`${baseUrl}/api/v1/auctions/${auctionId}`, params);
  recordReadResponse(response);
  return response;
}

export function getBidHistory(baseUrl, auctionId, size = 1000, viewpoint = 'consistency') {
  const response = http.get(
    `${baseUrl}/api/v1/auctions/${auctionId}/bids?page=0&size=${size}`,
    publicParams('bid_history', viewpoint),
  );
  recordReadResponse(response);
  return response;
}

export function recordReadResponse(response) {
  const failed = response.status === 0 || response.status >= 500 || response.status !== 200;
  readFailures.add(failed, response.tags);
  systemFailures.add(response.status === 0 || response.status >= 500, response.tags);
  readLatency.add(response.timings.duration, response.tags);
  return !failed;
}

export function placeBid(baseUrl, auctionId, amount, token, viewpoint) {
  const response = http.post(
    `${baseUrl}/api/v1/auctions/${auctionId}/bids`,
    JSON.stringify({ amount }),
    authParams(token, 'place_bid', viewpoint),
  );
  recordBidResponse(response);
  return response;
}

export function recordBidResponse(response) {
  const accepted = response.status === 200 || response.status === 201;
  const businessRejected = [400, 409, 422].includes(response.status);
  const authFailed = response.status === 401 || response.status === 403;
  const systemFailed = response.status === 0 || response.status >= 500
    || (!accepted && !businessRejected && !authFailed);

  if (accepted) acceptedBids.add(1, response.tags);
  if (businessRejected) businessRejections.add(1, response.tags);
  authFailures.add(authFailed, response.tags);
  systemFailures.add(systemFailed, response.tags);
  bidLatency.add(response.timings.duration, response.tags);

  return { accepted, businessRejected, authFailed, systemFailed };
}

export function verifyConsistency(baseUrl, auctionId, viewpoint = 'consistency') {
  const detailResponse = getAuction(baseUrl, auctionId, null, viewpoint);
  const historyResponse = getBidHistory(baseUrl, auctionId, 1000, viewpoint);

  if (detailResponse.status !== 200 || historyResponse.status !== 200) {
    consistencyFailures.add(true, { viewpoint, reason: 'verification_api_failed' });
    console.error(`Consistency verification API failed: detail=${detailResponse.status}, history=${historyResponse.status}`);
    return false;
  }

  const auction = safeJson(detailResponse);
  const historyPage = safeJson(historyResponse);
  const bids = Array.isArray(historyPage?.content) ? historyPage.content : [];
  const actualBidCount = Number(historyPage?.totalElements ?? bids.length);
  const highestBid = bids.reduce((highest, bid) => {
    if (highest === null || Number(bid.amount) > Number(highest.amount)) return bid;
    return highest;
  }, null);

  const countMatches = Number(auction?.bidCount) === actualBidCount;
  const priceMatches = highestBid === null
    ? Number(auction?.currentBid) === Number(auction?.startPrice)
    : Number(auction?.currentBid) === Number(highestBid.amount);
  const highestBidderId = highestBid?.bidder?.collectorId;
  const bidderMatches = highestBid === null
    ? auction?.topBidder === null || auction?.topBidder === undefined
    : Number(auction?.topBidder?.bidderId) === Number(highestBidderId);

  const passed = check(auction, {
    'consistency: auction bidCount equals bid history count': () => countMatches,
    'consistency: currentBid equals highest bid': () => priceMatches,
    'consistency: current bidder equals highest bidder': () => bidderMatches,
  }, { viewpoint, test_scope: 'auction' });

  consistencyFailures.add(!passed, { viewpoint });
  console.log(JSON.stringify({
    consistency: passed ? 'PASS' : 'FAIL',
    auctionId,
    auctionBidCount: auction?.bidCount,
    actualBidCount,
    currentBid: auction?.currentBid,
    highestBid: highestBid?.amount ?? null,
    currentBidderId: auction?.topBidder?.bidderId ?? null,
    highestBidderId: highestBidderId ?? null,
  }));
  return passed;
}

export function candidateAmount(initialBid, minIncrement, maxVus) {
  const sequence = (__ITER * maxVus) + __VU;
  return Number(initialBid) + (Number(minIncrement) * sequence);
}

export function assertAuctionCanRun(auction, minimumRemainingSeconds = 30) {
  if (!auction || auction.status !== 'LIVE') {
    throw new Error(`Auction must be LIVE; current status=${auction?.status}`);
  }
  const remainingMs = Date.parse(auction.endsAt) - Date.now();
  if (!Number.isFinite(remainingMs) || remainingMs < minimumRemainingSeconds * 1000) {
    throw new Error(`Auction ends too soon. Required remaining time=${minimumRemainingSeconds}s.`);
  }
}
