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

### 3.3 1순위 입찰자의 정의

- WebSocket에 먼저 연결한 사용자는 입찰 우선권을 갖지 않는다.
- v2 구조에서 1순위는 해당 Auction 행 락을 획득한 후 `observedSequence`와 금액 상한 검증을 처음 통과해 커밋한 요청이다.
- 여러 Pod에 도착한 네트워크 요청의 엄격한 도착 시간 FIFO는 보장하지 않는다. 이 보장이 필수가 되면 Kafka 명령 큐의 offset을 순서 원본으로 사용하는 전면 개편으로 전환한다.

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
  -> 200 OK, 기존 성공 결과와 idempotentReplay=true를 반환하고 재처리하지 않음

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
  "bidCount": 215,
  "idempotentReplay": false
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

## 11. 단계별 구현 계획

각 단계는 독립 커밋과 독립 배포로 관리한다. 앞 단계의 정합성 검증이 통과하지 않으면 다음 단계로 진행하지 않는다.

### Phase 0. 기준선과 기능 플래그

목적: 동작을 바꾸지 않고 전환과 롤백 스위치를 먼저 준비한다.

작업:

- 배포 image SHA, Auction replica 수, Kafka·Redis·DB 상태를 기록한다.
- 기존 낙관적 락 k6 결과를 회귀 기준선으로 고정한다.
- `bid.execution-mode=optimistic|pessimistic`를 정의하고 기본값은 `optimistic`으로 둔다.
- `bid.websocket-source=direct|redis`를 정의하고 기본값은 `direct`로 둔다.
- API 전환 전에는 `bid.api-v2.enabled=false`, `bid.redis-projection.enabled=false`를 기본값으로 둔다.

검증:

- 모든 플래그가 기본값일 때 기존 75개 Auction 회귀 테스트와 k6 결과가 변하지 않는다.

롤백:

- 코드 롤백 없이 모든 플래그를 기본값으로 복구한다.

### Phase 1. 모바일 화면 즉시 복구

목적: 백엔드 이벤트 경로 전환 전에 현재 모바일 stale 화면을 우선 완화한다.

작업:

- STOMP `onWebSocketClose`, `onStompError`에서 연결 상태를 `OFF`로 변경한다.
- `onConnect`에서 topic을 재구독한 뒤 REST 상세를 재조회한다.
- `visibilitychange` 후 화면이 다시 보이면 REST 상세를 재조회한다.
- 임시 보호로 LIVE 상태에서만 낮은 빈도의 polling fallback을 적용하고 완전 전환 후 제거 여부를 재평가한다.

검증:

- 모바일 잠금, 백그라운드, 네트워크 전환 후 최신 DB 가격으로 복구한다.
- 소켓이 끊겼는데 `LIVE` 연결 표시가 남지 않는다.

롤백:

- 재연결 후 REST 조회와 polling을 각각 독립적으로 비활성화한다.

### Phase 2. 후방 호환 DB 스키마

목적: 기존 소스가 계속 동작하는 additive migration을 먼저 배포한다.

작업:

- Auction에 `bid_sequence BIGINT NOT NULL DEFAULT 0`을 추가한다.
- 기존 Auction의 `bid_sequence`는 `bid_count`로 백필한다.
- Bid에 우선 nullable `sequence`, `request_id` 컬럼을 추가한다.
- 기존 Bid는 `(bid_at, bid_id)` 순으로 경매별 `ROW_NUMBER()`를 계산해 `sequence`를 백필한다.
- 백필 정합성 검증 후 `sequence NOT NULL`과 `UNIQUE (auction_id, sequence)`를 적용한다.
- 기존 입찰은 `request_id` 값이 없으므로 `UNIQUE (bidder_id, request_id) WHERE request_id IS NOT NULL` 부분 인덱스를 적용한다.
- Outbox에 `event_id UUID`를 추가하고 신규 이벤트에만 필수로 생성한다.
- 운영 스키마는 `ddl-auto=update`에만 의존하지 않고 검토된 migration SQL로 적용한다.

검증:

```text
Auction.bidSequence = Auction.bidCount
Auction.bidSequence = MAX(Bid.sequence)
경매별 Bid.sequence 중복 = 0
```

롤백:

- 신규 컬럼은 기존 코드가 참조하지 않으므로 스키마에 남기고 애플리케이션만 롤백한다.
- 즉시 컬럼 DROP은 수행하지 않는다.

### Phase 3. 멱등성과 서버 가격 계약

목적: 기존 API를 깨뜨리지 않고 신규 입찰 계약을 도입한다.

작업:

- 기존 `amount` API는 전환 기간에 유지한다.
- 신규 v2 API 또는 명시적 버전 헤더로 `requestId`, `observedSequence`, `maxAcceptableAmount`를 받는다.
- 성공한 중복 `requestId`는 `200 OK`, `idempotentReplay=true`와 기존 Bid 결과를 반환하고 새 Bid를 생성하지 않는다.
- `BID_STALE_STATE`, `BID_PRICE_CHANGED`는 최신 sequence와 가격을 포함한 `409`를 반환한다.

검증:

- 동일 사용자가 동일 `requestId`를 여러 번 전송해도 Bid는 한 개다.
- v1 클라이언트와 v2 클라이언트가 전환 기간에 모두 정상 동작한다.

롤백:

- 전환 기간에는 `bid.api-v2.enabled=false`로 v2 진입을 차단하고 v1 계약으로 복구한다.

### Phase 4. 경매 행 직렬화

목적: 한 경매의 다음 가격 계산과 순서 부여를 하나의 DB 임계 구역으로 직렬화한다.

작업:

- `AuctionJpaRepository.findByIdForUpdate()`에 `PESSIMISTIC_WRITE`를 적용한다.
- v2 실행 경로에서만 `SELECT FOR UPDATE`를 사용한다.
- 락 획득 후 최신 상태에서 중복 요청, 경매 상태, sequence, 금액 상한을 검증한다.
- 신규 경로에서는 낙관적 재시도를 사용하지 않는다.
- DB lock timeout을 무한으로 두지 않고 초과 시 재시도 가능한 명시적 오류로 변환한다.

검증:

- 동일 sequence와 상한으로 동시 요청한 경우 한 건만 성공한다.
- 서로 다른 경매 A/B는 병렬로 처리된다.
- deadlock, lock timeout, Hikari pending을 함께 측정한다.

롤백:

- `bid.execution-mode=optimistic`으로 기존 `@Version` 경로를 즉시 복구한다.
- `version` 컬럼과 낙관적 코드는 안정화 기간에 제거하지 않는다.

### Phase 5. Bid Transactional Outbox와 Kafka

목적: DB에 없는 입찰 이벤트와 DB에는 있지만 영원히 발행되지 않는 입찰 이벤트를 막는다.

작업:

- Bid와 Auction을 저장하는 트랜잭션에서 `BID_ACCEPTED` Outbox를 같이 INSERT한다.
- relay 조회는 `(status, id)` 순서로 정렬하고 여러 Pod가 대기하지 않도록 `SKIP LOCKED` 배치 claim을 적용한다.
- Kafka `key=auctionId`를 고정하고 producer acknowledgment 성공 후 Outbox를 `PROCESSED`로 변경한다.
- 5초 polling을 실시간 목표에 맞는 짧은 주기로 조정하되 DB 쿼리율과 빈 배치 비율을 함께 측정한다.
- 운영 규모가 커지면 polling을 Debezium CDC로 대체하는 후속 개선을 검토한다.

검증:

- DB 커밋 직후 Pod 종료, Kafka 일시 장애, relay 타임아웃에서도 Outbox가 유실되지 않는다.
- 동일 경매의 Kafka 이벤트를 eventId로 중복 제거하면 sequence가 연속적이다.

롤백:

- Outbox 생성은 유지하고 relay를 중단한다. 복구 후 PENDING 이벤트를 재발행할 수 있다.
- 입찰 DB 커밋과 성공 응답은 이벤트 relay 장애로 되돌리지 않는다.

### Phase 6. Redis Projection과 다중 Pod 팬아웃

목적: 모든 Auction Pod에 연결된 소켓 구독자가 동일 입찰 결과를 받도록 한다.

작업:

- Kafka projection consumer는 하나의 consumer group으로 이벤트를 한 번 Projection한다.
- Lua script로 sequence 검증, `ZADD NX`, `HSET`, `PUBLISH`를 원자적으로 수행한다.
- Lua 성공 후에만 Kafka offset을 acknowledgment한다.
- 모든 Auction Pod의 Redis subscriber가 Pub/Sub 이벤트를 로컬 `SimpMessagingTemplate`로 전달한다.
- Redis gap은 metric으로 기록하고 DB snapshot 또는 Kafka replay로 해당 경매 Projection을 재구축한다.

검증:

- 동일 Kafka 이벤트를 두 번 입력해도 ZSET cardinality와 Hash sequence는 한 번만 증가한다.
- Redis를 비운 뒤 재구축하면 DB currentBid, bidCount, bidSequence와 일치한다.
- Auction Pod 3개에 각각 연결된 구독자가 같은 eventId와 sequence를 받는다.

롤백:

- `bid.redis-projection.enabled=false`, `bid.websocket-source=direct`로 복구한다.
- Redis ZSET과 Hash는 Projection이므로 삭제 대신 TTL 만료 또는 후속 재구축 대상으로 둔다.

### Phase 7. WebSocket 경로 전환

목적: Pod 로컬 직접 발행을 Redis 팬아웃 경로로 전환한다.

작업:

- 먼저 Redis 경로를 shadow mode로 실행해 direct 이벤트와 eventId, sequence, 가격을 비교한다.
- 비교 통과 후 `bid.websocket-source=redis`로 전환한다.
- 전환 중 두 경로를 동시 화면 발행으로 사용하지 않는다. 불가피한 경우는 같은 eventId로 클라이언트가 중복 제거해야 한다.
- 안정화 후 `BidService.publishCommittedBid()` 직접 경로를 제거한다.

검증:

- PC 입찰 직후 모바일이 새로고침 없이 같은 sequence와 가격을 표시한다.
- 소켓 전달 p95, p99와 sequence gap 횟수를 기록한다.

롤백:

- `bid.websocket-source=direct`로 즉시 복귀하고 클라이언트 REST 복구 경로를 유지한다.

### Phase 8. 정리와 운영 기준 확정

- v2 사용률과 구버전 호출을 확인한 후 v1 `amount` API 제거 일정을 잡는다.
- 비관적 경로 안정화 전에 `@Version`과 낙관적 복구 코드를 삭제하지 않는다.
- WebSocket direct 경로는 안정화 기간 이후 제거한다.
- Redis TTL, Outbox 보관 기간, Kafka retention을 경매 종료 후 재접속·감사 기간에 맞게 확정한다.
- 플래그를 제거하기 전 롤백 불가 승인을 별도로 받는다.

## 12. 커밋 체크포인트

구현 시 다음 범위로 커밋을 나눈다.

1. `test(auction): 실시간 입찰 기준선 테스트 추가`
2. `feat(auction): 입찰 전환 기능 플래그 추가`
3. `feat(auction): 입찰 sequence와 멱등성 스키마 추가`
4. `feat(auction): 서버 계산 입찰 API v2 추가`
5. `feat(auction): 경매 행 비관적 직렬화 추가`
6. `feat(auction): 입찰 결과 outbox 저장`
7. `fix(auction): outbox relay 순서와 다중 pod claim 보장`
8. `feat(auction): Redis 입찰 projection 추가`
9. `feat(auction): Redis 기반 WebSocket 팬아웃 추가`
10. `feat(frontend): 경매 실시간 sequence 복구 추가`
11. `test(auction): 입찰 장애 복구와 다중 pod 전달 검증`
12. `refactor(auction): 기존 direct WebSocket 경로 제거`

각 커밋은 코드와 해당 테스트를 함께 포함하고, 다음 커밋 전에 독립 회귀가 가능해야 한다.

## 13. 테스트 계획

### 13.1 단위 테스트

- nextAmount 계산
- observedSequence 일치·불일치
- maxAcceptableAmount 경계값
- 중복 requestId의 기존 결과 반환
- Redis Lua의 정상·중복·역순·gap 처리
- WebSocket client의 중복 무시·gap REST 복구

### 13.2 PostgreSQL 통합 테스트

- 동일 경매에 동시 요청하여 한 순서에 한 건만 성공하는지 검증한다.
- 다른 경매 A/B는 동시 성공할 수 있는지 검증한다.
- 중복 requestId, stale sequence, 종료 경매, 자기 입찰이 행과 카운트를 변경하지 않는다.
- 성공 건마다 Bid, Auction, Outbox가 모두 있고 하나라도 없으면 실패다.

### 13.3 이벤트·Redis 통합 테스트

- Outbox PENDING을 Kafka에 전달하고 acknowledgment 후 PROCESSED가 되는지 검증한다.
- 동일 eventId를 재전달해도 ZSET과 Hash가 중복 증가하지 않는다.
- sequence gap을 주입하면 Projection이 임의로 건너뛰지 않고 복구 경로로 진입한다.
- Redis 재시작 후 DB와 동일한 상태로 재구축할 수 있다.

### 13.4 다중 Pod WebSocket 테스트

```text
Auction Pod 1: PC subscriber
Auction Pod 2: mobile subscriber
Auction Pod 3: bid HTTP handler
```

- 모든 구독자가 같은 eventId, sequence, currentBid를 받는다.
- 한 Pod를 종료하고 재연결해도 REST snapshot으로 최신 상태를 회복한다.
- 이벤트 수신 지연은 `receivedAt - committedAt`로 측정한다.

### 13.5 장애 주입

- DB 커밋 후 Kafka 발행 전 Pod 종료
- Kafka 일시 중단과 복구
- Kafka acknowledgment 후 Outbox 상태 변경 전 Pod 종료
- Redis Lua 성공 후 Kafka acknowledgment 전 consumer 종료
- Redis Pub/Sub 누락
- 모바일 소켓 종료·재연결

모든 장애에서 DB 불변식은 유지되고, 중복은 eventId로 제거되며, 누락은 sequence gap과 snapshot으로 복구되어야 한다.

## 14. 관측 지표와 중단 조건

### 14.1 필수 지표

```text
bid_accept_total
bid_reject_stale_total
bid_idempotent_replay_total
bid_db_lock_wait_seconds
bid_db_lock_timeout_total
bid_outbox_pending_total
bid_outbox_oldest_pending_seconds
bid_outbox_publish_fail_total
bid_kafka_consumer_lag
bid_redis_projection_gap_total
bid_redis_projection_duplicate_total
bid_websocket_delivery_seconds
bid_client_sequence_gap_total
bid_client_snapshot_recovery_total
```

### 14.2 즉시 중단

- `201 성공 수 != Bid 행 증가 != bidCount 증가 != Outbox 행 증가`
- Auction.currentBid와 최대 sequence Bid.amount 불일치
- 중복 requestId로 신규 Bid 생성
- deadlock, DB connection timeout, Pod restart/OOMKilled
- Outbox PENDING이 계속 증가하고 복구되지 않음
- 클라이언트 sequence gap이 REST snapshot 후에도 해소되지 않음

## 15. 롤아웃과 롤백 매트릭스

| 장애 지점 | 우선 대응 | DB 처리 | 복구 |
|---|---|---|---|
| 정식 입찰 API 오류 | 직전 애플리케이션 커밋으로 롤백 | 하위 호환 DB 스키마 유지 | 원인 수정 후 재배포 |
| 비관적 lock wait 증가 | `bid.execution-mode=optimistic` | 기존 `@Version` 복귀 | lock timeout·풀 분석 |
| Outbox relay 장애 | relay 중단 | 입찰 커밋 유지 | PENDING 재발행 |
| Redis Projection 오류 | projection 비활성 | DB 입찰 커밋 유지 | DB/Kafka로 재구축 |
| Redis Pub/Sub 누락 | direct 경로 임시 복귀 | 영향 없음 | 클라이언트 snapshot |
| WebSocket 전달 오류 | REST fallback 강화 | 영향 없음 | sequence gap 해소 |

스키마는 expand-contract 방식으로 전환한다. 신규 컬럼과 인덱스는 전환 안정화 전에 제거하지 않으며, destructive migration은 독립 승인 없이 실행하지 않는다.

## 16. 최종 전환 완료 조건

- 신규 API와 클라이언트가 성공 입찰에 멱등적으로 동작한다.
- 비관적 락 경로의 정합성, lock wait, throughput이 운영 기준을 통과한다.
- Bid·Auction·Outbox·Kafka·Redis의 경매별 sequence가 일치한다.
- Auction Pod 3개의 모든 소켓 구독자가 동일 이벤트를 받는다.
- 모바일 재연결·화면 복귀·gap 복구가 성공한다.
- 장애 주입 테스트에서 유령 입찰이 없고 커밋된 이벤트가 최종적으로 복구된다.
- 기능 플래그 롤백을 한 번 이상 리허설한 후에만 기존 경로를 제거한다.

## 17. 2026-07-31 백엔드 구현 결과와 전환값

이번 단계에서 다음 경로를 소스에 구현했다.

```text
Bid/Auction/Outbox commit
  -> Outbox relay (Kafka key = auctionId)
  -> auction.bid.accepted consumer group
  -> Redis Lua
       Hash: auction:realtime:{auctionId}
       ZSET: auction:realtime:{auctionId}:history (최근 1,000건)
       Pub/Sub: auction:realtime:{auctionId}:events
  -> 모든 Auction Pod의 pattern subscriber
  -> /topic/auctions/{auctionId}
```

- Lua는 현재 Hash sequence보다 큰 이벤트만 Hash와 ZSET에 반영하고 Pub/Sub을 발행한다.
- 같은 sequence 재전달과 과거 sequence는 화면에 중복 발행하지 않는다.
- ZSET score는 DB 확정 sequence이며, ZSET은 승자 결정이나 DB 대체 용도가 아니다.
- WebSocket BID payload는 `eventId`, `sequence`, `currentBid`, `nextMinimumBid`, `bidCount`, `bidderId`를 전달한다.
- 경매 상세 REST 응답도 `sequence`를 제공한다. 클라이언트는 이전 sequence 이하를 무시하고 gap이면 상세 API로 최신 snapshot을 다시 조회한다.
- Redis Pub/Sub 자체는 내구성 큐가 아니다. 연결 중 누락은 REST snapshot으로 복구하고 DB·Outbox·Kafka를 내구성 원본으로 유지한다.

기본 설정은 기존 direct 경로를 유지한다. 운영 전환은 다음 순서를 사용한다.

```text
# 1. shadow projection: WebSocket은 기존 direct 유지
BID_REDIS_PROJECTION_ENABLED=true
BID_WEBSOCKET_SOURCE=direct

# 2. DB currentBid/bidSequence와 Redis Hash를 비교한 뒤 fan-out 전환
BID_REDIS_PROJECTION_ENABLED=true
BID_WEBSOCKET_SOURCE=redis

# 즉시 롤백
BID_WEBSOCKET_SOURCE=direct
```

백엔드 자동 테스트는 Redis Lua 호출 계약, Kafka 역직렬화·위임, Redis subscriber, direct 중복 차단,
WebSocket sequence 계약과 경매 상세 REST sequence를 검증한다. 실제 여러 Pod·브라우저·모바일을 이용한 배포 환경 E2E와
Redis 장애 주입은 운영 전환 전에 별도로 수행한다.

## 18. 정식 BidService 승격과 기존 실행 경로 제거

서버 계산·sequence·멱등성 구현을 정식 애플리케이션 서비스로 승격했다.

- `BidV2Service` → `BidService`
- `BidV2TransactionService` → `BidTransactionService`
- `BidV2UseCase` → `BidUseCase`
- 애플리케이션 command/result의 `V2` 접미사를 제거했다.
- 기존 금액 직접 지정 v1 입찰 POST, DTO, 트랜잭션 서비스와 관련 테스트를 삭제했다.
- 기존 GET 입찰 내역과 내 입찰 조회는 `BidQueryService`/`BidQueryUseCase`로 분리해 유지한다.
- HTTP 계약 버전인 `BidV2Controller`, `PlaceBidV2Request/Response`, `/api/v2/...` 경로는 유지한다.
- API 기능 플래그는 제거했고 Gateway가 `/api/v2/auctions/**`를 정식 라우팅한다.

이 전환 이후 입찰 실행 롤백은 구형 v1 경로 활성화가 아니라 직전 애플리케이션 커밋 배포로 수행한다.
