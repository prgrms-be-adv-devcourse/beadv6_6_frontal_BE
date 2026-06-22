# 07. Redis 활용 패턴

## 1. Redis란?

**In-Memory Key-Value 데이터 스토어**로, 모든 데이터를 메모리에 저장하여 극도로 빠른 읽기/쓰기를 제공한다.

```
일반 DB (PostgreSQL)          Redis
┌─────────────────┐         ┌─────────────────┐
│   디스크 기반     │         │   메모리 기반     │
│   ~1ms 응답      │         │   ~0.1ms 응답    │
│   영속성 보장     │         │   휘발성 (선택)   │
│   복잡한 쿼리     │         │   단순 Key-Value  │
└─────────────────┘         └─────────────────┘
```

### 핵심 특징
| 항목 | 설명 |
|------|------|
| **속도** | 초당 100,000+ 요청 처리 (단일 인스턴스) |
| **자료구조** | String, Hash, List, Set, Sorted Set, Stream 등 |
| **영속성** | RDB 스냅샷, AOF 로그 (선택적) |
| **만료(TTL)** | 키 단위 자동 만료 지원 |
| **Pub/Sub** | 메시지 브로커 기능 내장 |
| **싱글 스레드** | 명령어 단위 원자성 보장 (Thread-Safe) |

---

## 2. Redis 자료구조와 활용

### 2.1 String — 가장 기본

```redis
SET user:42:name "collector01"
GET user:42:name          → "collector01"
INCR auction:A-001:views  → 1, 2, 3...  (원자적 카운터)
SETEX cache:feed:live 300 "{...}"       (5분 TTL)
```

**활용**: 캐시, 카운터, 세션, Rate Limiting

### 2.2 Hash — 객체 저장

```redis
HSET auction:A-001 name "나이키 덩크" currentBid 720000 bidCount 6
HGET auction:A-001 currentBid  → "720000"
HINCRBY auction:A-001 bidCount 1  → 7 (원자적 증가)
```

**활용**: 엔티티 캐싱, 실시간 경매 상태 관리

### 2.3 Sorted Set — 점수 기반 정렬

```redis
ZADD leaderboard 720000 "bidder:42"
ZADD leaderboard 700000 "bidder:33"
ZREVRANGE leaderboard 0 9 WITHSCORES  → Top 10
ZRANK leaderboard "bidder:42"          → 순위 조회
```

**활용**: 리더보드, 입찰 랭킹, 마감임박 경매 정렬

### 2.4 List — 큐/스택

```redis
LPUSH notifications:user:42 "{type: BID_OUTBID, ...}"
LRANGE notifications:user:42 0 9  → 최근 10개 알림
```

**활용**: 알림 큐, 최근 활동 로그

### 2.5 Set — 중복 없는 집합

```redis
SADD auction:A-001:watchers "user:42" "user:33"
SCARD auction:A-001:watchers  → 2 (관심 등록 수)
SISMEMBER auction:A-001:watchers "user:42"  → 1 (true)
```

**활용**: 관심 등록(찜), 중복 방지, 온라인 사용자 추적

### 2.6 Stream — 이벤트 스트리밍

```redis
XADD bids * auctionId A-001 bidderId 42 amount 720000
XREAD COUNT 10 STREAMS bids 0  → 이벤트 소비
```

**활용**: 이벤트 소싱, 실시간 입찰 스트림

---

## 3. 주요 활용 패턴

### 3.1 캐시 패턴 (Cache-Aside / Look-Aside)

가장 일반적인 캐시 전략. **애플리케이션이 캐시를 직접 관리**한다.

```
[Client] → [Application]
               │
               ├── 1. Cache Hit?  → [Redis] → 있으면 바로 반환
               │
               └── 2. Cache Miss  → [DB] → 조회
                                      │
                                      └── 3. Cache에 저장 → [Redis]
```

```java
// Spring에서의 구현
@Cacheable(value = "auctionFeed", key = "#query.status + ':' + #query.page")
public Page<AuctionFeedResult> getAuctionFeed(AuctionFeedQuery query) {
    return auctionRepository.findByFilters(...);
}

@CacheEvict(value = "auctionFeed", allEntries = true)
public void placeBid(BidCommand command) {
    // 입찰 처리 후 캐시 무효화
}
```

### 3.2 Write-Through vs Write-Behind

```
Write-Through (동기)           Write-Behind (비동기)
┌──────────┐                  ┌──────────┐
│ App      │                  │ App      │
│  ↓ 동시   │                  │  ↓ 먼저   │
│ Cache+DB │                  │ Cache    │ → 나중에 → DB
└──────────┘                  └──────────┘
일관성 높음, 느림              빠름, 데이터 유실 위험
```

### 3.3 분산 락 (Distributed Lock)

MSA 환경에서 동시성 제어에 필수적이다.

```
[Server A] ─── SETNX lock:auction:A-001 ───→ [Redis]
[Server B] ─── SETNX lock:auction:A-001 ───→ 실패 (이미 잠김)
```

```java
// Redisson 분산 락 예시
RLock lock = redissonClient.getLock("lock:auction:" + auctionId);
try {
    if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
        // 임계 영역: 입찰 처리
        processiBid(auctionId, bidCommand);
    }
} finally {
    lock.unlock();
}
```

**주의사항**:
- TTL 필수 설정 (데드락 방지)
- Redlock 알고리즘 (다중 노드 환경)
- 비관적 락 vs 낙관적 락 선택 → `02_동시성제어_Lock_패턴.md` 참고

### 3.4 Rate Limiting (속도 제한)

```
Sliding Window 방식:

MULTI
  ZADD rate:user:42 {timestamp} {uuid}
  ZREMRANGEBYSCORE rate:user:42 0 {1분 전 timestamp}
  ZCARD rate:user:42
EXEC
→ 카운트 > 제한이면 429 Too Many Requests
```

```java
// 입찰 Rate Limiting 예시 (1분에 10회)
public boolean isAllowed(Long userId) {
    String key = "rate:bid:" + userId;
    long now = System.currentTimeMillis();
    long windowStart = now - 60_000;

    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
    Long count = redisTemplate.opsForZSet().size(key);

    if (count != null && count >= 10) return false;

    redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
    redisTemplate.expire(key, 61, TimeUnit.SECONDS);
    return true;
}
```

### 3.5 Pub/Sub — 실시간 알림

```
[입찰 서비스]                    [알림 서비스]
     │                              │
     └── PUBLISH bid:A-001 {...} ──→ SUBSCRIBE bid:A-001
                                     │
                                     └── WebSocket → Client
```

```java
// Publisher
redisTemplate.convertAndSend("bid:" + auctionId, bidEvent);

// Subscriber
@Component
public class BidSubscriber implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // WebSocket으로 클라이언트에 전달
    }
}
```

---

## 4. Biddy 프로젝트 적용 시나리오

### 4.1 경매 실시간 상태 관리

```
Redis Hash로 실시간 경매 상태 관리:

HSET auction:A-001
  currentBid    720000
  bidCount      6
  watcherCount  88
  topBidderId   42
  endsAt        "2026-06-12T15:30:00"

→ DB는 최종 영속화, Redis가 실시간 읽기/쓰기 담당
→ 입찰 시 HINCRBY로 원자적 업데이트
```

### 4.2 입찰 동시성 제어

```
[입찰 요청] → Rate Limit 체크 → 분산 락 획득 → 금액 검증 → 입찰 처리

1. Rate Limit:  Sorted Set (1분 10회)
2. 분산 락:     SETNX + TTL (입찰 직렬화)
3. 현재가 조회:  HGET (Redis에서 즉시)
4. 입찰 반영:   HINCRBY bidCount + HSET currentBid
5. DB 비동기:   Kafka → DB 영속화
```

### 4.3 경매 피드 캐싱

```
캐시 키 설계:
  feed:LIVE:sneakers:ending:0:20  → 5분 TTL
  feed:LIVE:null:latest:0:20      → 5분 TTL

무효화 전략:
  - 새 입찰 발생 시 → 해당 카테고리 캐시 무효화
  - 경매 종료 시   → LIVE 캐시 전체 무효화
```

### 4.4 마감임박 경매 Sorted Set

```redis
ZADD ending:soon {endsAt_timestamp} "A-001"
ZADD ending:soon {endsAt_timestamp} "A-002"
ZRANGEBYSCORE ending:soon {now} {now + 10min}  → 10분 내 마감 경매
```

---

## 5. Spring Boot + Redis 설정

### 5.1 의존성

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.redisson:redisson-spring-boot-starter:3.30.0'  // 분산 락
```

### 5.2 application.yaml

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
```

### 5.3 RedisTemplate 설정

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

---

## 6. 캐시 일관성 전략

### 6.1 문제: DB와 Cache 불일치

```
시나리오: 입찰 발생 시

1. DB 업데이트 성공
2. 캐시 삭제 실패  ← 네트워크 오류
3. 클라이언트가 오래된 캐시 조회 (Stale Data)
```

### 6.2 해결 전략

| 전략 | 방법 | 트레이드오프 |
|------|------|-------------|
| **Cache Invalidation** | DB 변경 시 캐시 삭제 | 단순, Cache Miss 증가 |
| **TTL 기반 만료** | 짧은 TTL 설정 (5~30초) | 일시적 불일치 허용 |
| **이벤트 기반 무효화** | Kafka 이벤트로 캐시 동기화 | 복잡하지만 정확 |
| **Write-Through** | DB + Cache 동시 쓰기 | 느리지만 일관성 보장 |

### 6.3 Biddy 권장 전략

```
경매 피드 (읽기 빈도 높음):
  → Cache-Aside + TTL 5분 + 입찰 시 @CacheEvict

실시간 입찰가 (쓰기 빈도 높음):
  → Redis를 Primary Store + Kafka로 DB 비동기 영속화

알림 (일회성):
  → Pub/Sub (캐시 불필요)
```

---

## 7. Redis vs 다른 캐시 비교

| 항목 | Redis | Memcached | Caffeine (로컬) |
|------|-------|-----------|----------------|
| **자료구조** | 다양 (Hash, Set, ZSet...) | String만 | 자바 객체 |
| **영속성** | RDB/AOF | 없음 | 없음 |
| **분산 락** | 지원 | 불가 | 불가 |
| **Pub/Sub** | 지원 | 불가 | 불가 |
| **MSA 공유** | 가능 (네트워크) | 가능 | 불가 (프로세스 내) |
| **속도** | ~0.1ms (네트워크 포함) | ~0.1ms | ~0.001ms |
| **적합 시나리오** | MSA 공유 캐시, 분산 락 | 단순 캐시 | 단일 서비스 L1 캐시 |

### 다단계 캐시 (L1 + L2)

```
[Client] → [Caffeine L1] → Cache Hit → 바로 반환 (0.001ms)
                │
                └── Cache Miss → [Redis L2] → Cache Hit → 반환 (0.1ms)
                                      │
                                      └── Cache Miss → [DB] → 반환 (1ms+)
```

---

## 8. 운영 시 주의사항

### 8.1 메모리 관리

```
maxmemory 2gb
maxmemory-policy allkeys-lru    # LRU 방식으로 오래된 키 제거

모니터링 필수:
  INFO memory → used_memory, maxmemory
  큰 키 탐지: redis-cli --bigkeys
```

### 8.2 장애 대응

| 시나리오 | 대응 |
|----------|------|
| Redis 다운 | DB Fallback (Circuit Breaker) |
| 네트워크 지연 | timeout 설정 + 로컬 캐시 병행 |
| 메모리 부족 | maxmemory-policy + 모니터링 알림 |
| 캐시 스탬피드 | 락 기반 캐시 갱신 (Cache Stampede 방지) |

### 8.3 캐시 스탬피드 방지

```
문제: TTL 만료 시 동시에 수백 요청이 DB로 몰림

해결: 락 기반 갱신
  1. 캐시 Miss 발생
  2. SETNX로 갱신 락 획득 시도
  3. 락 획득한 1개만 DB 조회 → 캐시 갱신
  4. 나머지는 짧은 대기 후 캐시 재조회
```

---

## 9. 관련 문서

- [02_동시성제어_Lock_패턴.md](02_동시성제어_Lock_패턴.md) — 분산 락 상세
- [01_Saga_패턴.md](01_Saga_패턴.md) — 분산 트랜잭션과 보상
- [04_CQRS_패턴.md](04_CQRS_패턴.md) — 읽기 최적화와 캐시 조합
