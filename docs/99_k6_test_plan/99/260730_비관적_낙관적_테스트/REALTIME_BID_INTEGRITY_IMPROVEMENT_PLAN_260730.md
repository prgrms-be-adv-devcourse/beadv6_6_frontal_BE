# 실시간 입찰 가격·순서·DB 정합성 개선 계획

작성일: 2026-07-30 (Asia/Seoul)

기준 브랜치: `restore/optimistic-lock-260730`

기준 SHA: `74b92ec`

## 1. 목적

경매 상품 상세에서 PC와 모바일이 동일한 실시간 가격을 표시하고, 동시 입찰에서 다음 조건을 보장한다.

- 한 경매의 성공 입찰에 중복이 없는 순서를 부여한다.
- 사용자가 확인한 최대 금액을 초과해 서버가 자동 입찰하지 않는다.
- `201 Created`는 Bid, Auction, Outbox가 하나의 DB 트랜잭션으로 커밋된 경우에만 반환한다.
- 커밋된 이벤트는 다중 Auction Pod의 WebSocket 구독자에게 전달된다.
- 소켓 이벤트가 누락·중복·역순으로 도착해도 클라이언트가 DB 최신 상태로 복구한다.

## 2. 최신 소스 기준점

| 구분 | 현재 구현 | 한계 |
|---|---|---|
| 입찰 API | `amount`만 수신 | 멱등성 키와 사용자가 본 상태 버전이 없음 |
| 가격 계산 | 클라이언트가 `currentBid + minIncrement` 계산 | 스냅샷이 오래되면 동일 금액 경쟁 발생 |
| 동시성 | Auction `@Version`, 최대 3회 재시도 | 고경합에서 롤백·재시도 비용 발생 |
| DB 경계 | Bid INSERT + Auction UPDATE + flush | 입찰 결과 Outbox는 같이 저장하지 않음 |
| WebSocket | 커밋 후 현재 Pod의 `SimpleBroker`에 직접 발행 | HTTP 처리 Pod와 소켓 연결 Pod가 다르면 누락 가능 |
| WebSocket payload | `type`, `currentBid`, `bidCount`, `bidderId` | `eventId`, `auctionId`, 순서, 커밋 시각이 없음 |
| Outbox | 경매 종료 이벤트에 사용 | 입찰 성공 이벤트는 적용되지 않음 |
| Outbox relay | 5초 주기, PESSIMISTIC_WRITE | 실시간 지연이 크고 Pod별 스케줄러 직렬 대기 가능 |
| Redis | 관심 경매 자료구조에 사용 | 입찰 순서·현재가 Projection이 없음 |

### 2.1 현재 정합성에서 유지할 부분

- `REQUIRES_NEW`, `READ_COMMITTED`에서 Bid와 Auction을 같이 처리하는 트랜잭션 경계
- 성공 응답 전 `flush`로 낙관적 락 충돌을 확정하는 계약
- 업무 검증 실패는 재시도하지 않고, 낙관적 락 충돌만 제한적으로 재시도하는 분리
- `201 성공 수 = Bid 행 증가 = bidCount 증가`를 확인한 기존 테스트
- 기존 Kafka, Outbox, Redis, STOMP 의존성과 운영 인프라

## 3. 선택 아키텍처

이번 개선에서는 Redis를 입찰의 최종 원본으로 사용하지 않는다. PostgreSQL 커밋이 최종 사실이고 Kafka는 내구성 있는 이벤트 경로, Redis는 조회와 실시간 팬아웃을 위한 Projection으로 사용한다.

```text
POST /bids
  requestId, observedSequence, maxAcceptableAmount
        |
        v
Auction SELECT FOR UPDATE
        |
        v
서버가 nextAmount = currentBid + minIncrement 계산
        |
        v
Bid INSERT + Auction UPDATE + Outbox INSERT
        |
        v
PostgreSQL COMMIT -> 201 Created
        |
        v
Outbox Relay -> Kafka (key = auctionId)
        |
        v
Redis Lua: ZSET + HASH + PUBLISH
        |
        v
모든 Auction Pod Redis Subscriber
        |
        v
STOMP /topic/auctions/{auctionId}
        |
        v
PC·모바일 sequence 검증 후 화면 반영
```

### 3.1 선택 이유

- 한 경매의 현재가는 앞선 커밋 결과에 종속되므로 본질적으로 직렬 처리 대상이다.
- `SELECT FOR UPDATE`는 한 Auction 행만 잠그고 서로 다른 Auction은 병렬 처리한다.
- 서버가 다음 입찰가를 계산하되 `maxAcceptableAmount`로 사용자의 금액 동의 범위를 보장한다.
- Outbox는 DB 커밋 성공과 Kafka 발행 사이의 dual-write 공백을 제거한다.
- Kafka는 `auctionId`를 key로 사용해 동일 경매 이벤트를 동일 partition으로 전달한다.
- Redis Pub/Sub이 놓쳐진 경우는 sequence gap과 REST snapshot으로 복구하며, 내구성의 원본은 DB와 Kafka에 남는다.

### 3.2 이번 범위에서 제외하는 전면 개편

입찰 명령을 DB 전에 Kafka로 받는 완전 비동기 FIFO는 이번 범위에서 제외한다.

- API를 `201`에서 `202 + 처리 상태 조회`로 전면 변경해야 한다.
- Kafka 장애가 입찰 접수 장애로 즉시 확대된다.
- 엄격한 Gateway 도착 FIFO가 정식 요구사항으로 확정되거나 단일 경매의 DB lock wait가 운영 임계를 넘을 때 다음 단계로 검토한다.

## 4. 입찰 API 계약

### 4.1 요청

```http
POST /api/v1/auctions/{auctionId}/bids
Content-Type: application/json
X-Member-Id: 42
```

```json
{
  "requestId": "4d47e190-0402-4048-bc2c-89dd54343cdc",
  "observedSequence": 214,
  "maxAcceptableAmount": 111000
}
```

| 필드 | 의미 | 검증 |
|---|---|---|
| `requestId` | 클라이언트가 생성한 멱등성 키 | 입찰자 범위에서 중복 금지 |
| `observedSequence` | 사용자가 화면에서 확인한 성공 입찰 순서 | DB 최신 sequence와 다르면 거절 |
| `maxAcceptableAmount` | 사용자가 이번 요청에서 동의한 최대 금액 | 서버 계산 `nextAmount`가 크면 거절 |

### 4.2 서버 승인 규칙

```text
nextAmount = auction.currentBid + auction.minIncrement

requestId가 기존 성공 요청과 같음
  -> 기존 성공 결과를 반환하고 재처리하지 않음

observedSequence != auction.bidSequence
  -> 409 BID_STALE_STATE

nextAmount > maxAcceptableAmount
  -> 409 BID_PRICE_CHANGED

나머지 경매 상태·판매자 자기 입찰 검증 통과
  -> nextAmount로 커밋
```

`observedSequence` 또는 가격이 바뀐 요청을 서버가 자동으로 더 높은 금액에 입찰하면 안 된다. 클라이언트가 최신가를 확인한 후 새 요청을 보내야 한다.

### 4.3 성공 응답

```json
{
  "bidId": 9876,
  "requestId": "4d47e190-0402-4048-bc2c-89dd54343cdc",
  "sequence": 215,
  "amount": 111000,
  "currentBid": 111000,
  "nextMinimumBid": 111500,
  "bidCount": 215
}
```

### 4.4 오래된 가격 응답

```http
HTTP/1.1 409 Conflict
```

```json
{
  "code": "B006",
  "message": "입찰 가격이 변경되었습니다.",
  "auctionId": "A-E290D",
  "sequence": 215,
  "currentBid": 111000,
  "nextMinimumBid": 111500,
  "retryable": true
}
```

## 5. DB 모델과 트랜잭션

### 5.1 Auction

```text
bid_sequence BIGINT NOT NULL DEFAULT 0
current_bid BIGINT NOT NULL
current_bidder_id BIGINT NULL
bid_count INTEGER NOT NULL
```

`version`은 낙관적 락 복구 가능성을 위해 스키마에 유지할 수 있지만, 선택 아키텍처의 입찰 순서 계약에는 `bid_sequence`를 사용한다.

### 5.2 Bid

```text
sequence BIGINT NOT NULL
request_id UUID NOT NULL
```

필수 제약:

```sql
UNIQUE (auction_id, sequence)
UNIQUE (bidder_id, request_id)
```

### 5.3 Bid Outbox

Bid INSERT, Auction UPDATE와 같은 트랜잭션에서 Outbox를 INSERT한다.

```text
event_id UUID UNIQUE NOT NULL
aggregate_type = Bid
aggregate_id = auctionId
topic = auction.bid.accepted
payload = BidAcceptedEvent JSON
status = PENDING
created_at = DB commit 직전 생성 시각
```

### 5.4 트랜잭션 순서

```text
1. 요청 형식과 인증 정보 검증
2. Auction SELECT FOR UPDATE
3. requestId 중복 조회
4. LIVE, 시작·종료 시각, 자기 입찰 검증
5. observedSequence와 DB bidSequence 비교
6. nextAmount 계산과 maxAcceptableAmount 비교
7. nextSequence = bidSequence + 1
8. Bid INSERT
9. Auction currentBid, currentBidderId, bidCount, bidSequence UPDATE
10. BID_ACCEPTED Outbox INSERT
11. flush
12. COMMIT
13. 201 Created
```

## 6. 이벤트 계약

```json
{
  "eventId": "6f6ef3da-7177-4d2e-9173-a348494e7197",
  "eventType": "BID_ACCEPTED",
  "auctionId": "A-E290D",
  "bidId": 9876,
  "requestId": "4d47e190-0402-4048-bc2c-89dd54343cdc",
  "sequence": 215,
  "currentBid": 111000,
  "nextMinimumBid": 111500,
  "bidCount": 215,
  "bidderId": 42,
  "committedAt": "2026-07-30T18:30:00.123+09:00"
}
```

정렬 기준은 timestamp가 아니라 `sequence`다. timestamp는 관측과 지연시간 측정에만 사용한다.

Kafka:

```text
topic = auction.bid.accepted
key = auctionId
value = BidAcceptedEvent
```

## 7. Redis 자료구조

### 7.1 입찰 순서 Sorted Set

```text
key    = auction:{A-E290D}:bids
score  = sequence
member = eventId
```

```redis
ZADD auction:{A-E290D}:bids NX 215 6f6ef3da-7177-4d2e-9173-a348494e7197
```

`score`에 시간이나 금액을 사용하지 않는다. 시간은 충돌할 수 있고, 이 경매는 모든 성공 입찰이 이전 가격보다 높으므로 마지막 sequence가 최고가다.

### 7.2 현재 상태 Hash

```text
key = auction:{A-E290D}:state

sequence       = 215
currentBid     = 111000
nextMinimumBid = 111500
bidCount       = 215
highestBidder  = 42
eventId        = 6f6ef3da-7177-4d2e-9173-a348494e7197
```

### 7.3 Lua 적용 규칙

ZSET, Hash, Redis Pub/Sub 발행을 하나의 Lua script로 수행한다. Redis Cluster를 사용할 경우 관련 key는 `{auctionId}` hash tag와 같은 slot을 사용한다.

```text
incoming sequence <= current sequence
  -> 중복·역순, 변경과 재발행 없음

incoming sequence == current sequence + 1
  -> ZADD NX, HSET, PUBLISH

incoming sequence > current sequence + 1
  -> gap metric 기록, DB/Kafka snapshot으로 Projection 복구 후 재처리
```

Redis가 비어 있거나 장애인 경우에도 DB 입찰 커밋은 유지한다. Redis는 DB 또는 Kafka 이벤트로 재구축할 수 있어야 한다.

## 8. WebSocket과 클라이언트 계약

### 8.1 서버

- Redis subscriber는 모든 Auction Pod에서 동일 입찰 이벤트를 받는다.
- 각 Pod는 자신에게 연결된 STOMP 구독자에게 `/topic/auctions/{auctionId}`로 전달한다.
- 전환 완료 후 `BidService -> SimpleBroker` 직접 발행은 제거해 중복 경로를 막는다.

### 8.2 클라이언트

```text
1. STOMP CONNECTED 확인
2. 경매 topic SUBSCRIBE
3. 수신 이벤트 임시 버퍼링
4. REST 경매 상세 snapshot 조회
5. snapshot.sequence 이하 이벤트 폐기
6. sequence + 1인 이벤트만 반영
7. sequence gap, socket reconnect, visibilitychange 시 REST snapshot 재조회
```

```text
incoming.sequence <= local.sequence
  -> 중복·역순으로 무시

incoming.sequence == local.sequence + 1
  -> currentBid, nextMinimumBid, bidCount 반영

incoming.sequence > local.sequence + 1
  -> 화면 임시 보호, REST snapshot 재조회
```

## 9. 필수 불변식

각 경매별로 다음을 만족해야 한다.

```text
201 성공 수
= 신규 Bid 행 수
= Auction.bidCount 증가량
= BID_ACCEPTED Outbox 행 수

Auction.currentBid
= 가장 큰 Bid.sequence의 Bid.amount

Auction.bidSequence
= MAX(Bid.sequence)
= Auction.bidCount

중복 제거한 Kafka/Redis 이벤트 sequence
= DB의 성공 sequence 집합
```

합계만 맞고 개별 경매가 틀리면 실패다.

## 10. 성공 기준

- 동시에 같은 `observedSequence`, `maxAcceptableAmount`로 요청하면 한 건만 `201`이고 나머지는 최신 상태를 포함한 `409`다.
- 중복 `requestId`는 Bid나 bidCount를 추가로 증가시키지 않는다.
- DB 커밋 직후 Pod를 종료해도 Outbox가 남고 복구 후 이벤트가 전달된다.
- Auction Pod 3개 중 어느 Pod에 PC·모바일이 연결되어도 동일 sequence와 가격으로 수렴한다.
- Redis 중복 이벤트와 재시작이 ZSET 원소, Hash sequence, WebSocket 화면을 중복 증가시키지 않는다.
- 모바일 잠금·백그라운드 후 복귀하면 REST snapshot으로 최신 가격을 표시한다.

