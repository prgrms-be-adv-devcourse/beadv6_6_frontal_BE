# 동시성 제어 — 낙관적 락 vs 비관적 락

경매 입찰처럼 동시 요청이 빈번한 기능에서 데이터 정합성을 보장하기 위한 잠금 전략.

---

## 1. 낙관적 락 (Optimistic Lock)

**충돌이 거의 없을 것**이라고 가정하고, 커밋 시점에 충돌을 감지하여 재시도하는 방식.

### 원리

```
1. 데이터 조회 시 version 값을 함께 읽음
2. 수정 후 저장할 때 version이 변경되었는지 확인
3. version이 같으면 → 저장 성공 (version + 1)
4. version이 다르면 → OptimisticLockException 발생 → 재시도
```

### Spring Boot + JPA 구현

```java
@Entity
public class Auction {
    @Id
    private String auctionId;

    private Long currentBid;
    private Integer bidCount;

    @Version  // 핵심: JPA가 자동으로 version 관리
    private Long version;
}
```

```java
@Service
@RequiredArgsConstructor
public class BidService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Retryable(
        retryFor = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional
    public BidResponse placeBid(String auctionId, Long amount, Long bidderId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.validateBid(amount);  // 최소 입찰 단위, 상태 검증
        auction.applyBid(amount);

        Bid bid = Bid.create(auctionId, bidderId, amount);
        bidRepository.save(bid);
        auctionRepository.save(auction);  // version 불일치 시 여기서 예외 발생

        return BidResponse.from(bid, auction);
    }
}
```

실행되는 SQL:
```sql
UPDATE auction
SET current_bid = 740000, bid_count = 7, version = 4
WHERE auction_id = 'A-FNF97' AND version = 3;
-- 영향 받은 행이 0이면 → OptimisticLockException
```

### 장단점

| 장점 | 단점 |
|------|------|
| DB 잠금 없음 → 높은 처리량 | 충돌 시 재시도 비용 |
| 읽기 성능 우수 | 충돌 빈도 높으면 성능 저하 |
| 데드락 없음 | 재시도 로직 직접 구현 필요 |
| 확장성 좋음 | 긴 트랜잭션에서 충돌 확률 증가 |

### 적합한 상황

- **읽기 > 쓰기**인 경우
- 동시 수정 확률이 **낮은** 경우
- 예: 상품 정보 수정, 회원 프로필 수정, 재고가 충분한 상품 주문

---

## 2. 비관적 락 (Pessimistic Lock)

**충돌이 자주 발생할 것**이라고 가정하고, 조회 시점부터 DB 행을 잠가 다른 트랜잭션의 접근을 차단하는 방식.

### 원리

```
1. SELECT ... FOR UPDATE로 행 잠금 획득
2. 다른 트랜잭션은 잠금이 해제될 때까지 대기
3. 데이터 수정 후 커밋 → 잠금 해제
4. 대기 중이던 트랜잭션이 순차 실행
```

### Spring Boot + JPA 구현

```java
public interface AuctionRepository extends JpaRepository<Auction, String> {

    // PESSIMISTIC_WRITE = SELECT ... FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.auctionId = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") String id);

    // PESSIMISTIC_READ = SELECT ... FOR SHARE (공유 잠금, 읽기 허용)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT a FROM Auction a WHERE a.auctionId = :id")
    Optional<Auction> findByIdForShare(@Param("id") String id);
}
```

```java
@Service
@RequiredArgsConstructor
public class BidService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Transactional
    public BidResponse placeBid(String auctionId, Long amount, Long bidderId) {
        // FOR UPDATE로 행 잠금 → 다른 입찰은 여기서 대기
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
            .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.validateBid(amount);
        auction.applyBid(amount);

        Bid bid = Bid.create(auctionId, bidderId, amount);
        bidRepository.save(bid);
        auctionRepository.save(auction);

        return BidResponse.from(bid, auction);
    }
}
```

실행되는 SQL:
```sql
SELECT * FROM auction WHERE auction_id = 'A-FNF97' FOR UPDATE;
-- 다른 트랜잭션은 이 행에 대해 대기

UPDATE auction SET current_bid = 740000, bid_count = 7 WHERE auction_id = 'A-FNF97';
```

### 타임아웃 설정 (데드락 방지)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("SELECT a FROM Auction a WHERE a.auctionId = :id")
Optional<Auction> findByIdForUpdate(@Param("id") String id);
```

### 장단점

| 장점 | 단점 |
|------|------|
| 데이터 정합성 확실 보장 | DB 잠금 → 처리량 제한 |
| 재시도 로직 불필요 | 데드락 가능성 |
| 구현 단순 | 트랜잭션 시간만큼 다른 요청 대기 |
| 충돌 빈도 높아도 안정적 | DB 커넥션 풀 소진 위험 |

### 적합한 상황

- **쓰기 > 읽기**이거나 동시 수정이 **빈번**한 경우
- 예: 경매 입찰, 선착순 쿠폰 발급, 좌석 예약, 잔액 차감

---

## 3. 비교 요약

| 기준 | 낙관적 락 | 비관적 락 |
|------|----------|----------|
| 잠금 시점 | 커밋 시 (사후 검증) | 조회 시 (사전 잠금) |
| 잠금 수준 | 애플리케이션 레벨 (@Version) | DB 레벨 (FOR UPDATE) |
| 충돌 처리 | 예외 → 재시도 | 대기 → 순차 실행 |
| 성능 (저경합) | 우수 | 보통 (불필요한 잠금 오버헤드) |
| 성능 (고경합) | 재시도 반복으로 저하 | 안정적 |
| 데드락 | 없음 | 가능 (타임아웃으로 방지) |
| 구현 난이도 | 중 (재시도 로직 필요) | 낮 (쿼리 어노테이션만) |

---

## 4. 이 프로젝트 적용 가이드

### 경매 입찰 → 비관적 락 추천

```
이유:
- 인기 경매에 동시 입찰이 집중됨 (고경합)
- 입찰 금액 순서가 중요 (선착순 보장)
- 재시도 반복보다 순차 처리가 사용자 경험에 유리
```

```java
// 추천 구현
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("SELECT a FROM Auction a WHERE a.auctionId = :id")
Optional<Auction> findByIdForUpdate(@Param("id") String id);
```

### 상품 정보 수정 → 낙관적 락 추천

```
이유:
- 판매자 본인만 수정 (저경합)
- 동시 수정 확률 매우 낮음
- 읽기 성능 중요
```

```java
// 추천 구현
@Entity
public class Product {
    @Version
    private Long version;
}
```

### 하이브리드 전략

실무에서는 두 방식을 조합해서 사용한다.

```
┌─────────────────┬──────────────┬──────────────────────┐
│ 기능            │ 잠금 전략     │ 이유                 │
├─────────────────┼──────────────┼──────────────────────┤
│ 경매 입찰       │ 비관적 락     │ 고경합, 순서 보장     │
│ 관심 경매 토글  │ 낙관적 락     │ 저경합, 개인 데이터   │
│ 상품 정보 수정  │ 낙관적 락     │ 저경합, 단일 수정자   │
│ 예치금 차감     │ 비관적 락     │ 고경합, 금액 정합성   │
│ 주문 상태 변경  │ 낙관적 락     │ 저경합, 상태 전이     │
│ 선착순 쿠폰     │ 비관적 락     │ 고경합, 수량 제한     │
└─────────────────┴──────────────┴──────────────────────┘
```

---

## 5. 추가 고려: Redis 분산 락

서비스가 수평 확장(Scale-out)되면 DB 락만으로 부족할 수 있다.
이 경우 **Redis(Redisson)** 기반 분산 락을 사용한다.

```java
@Service
@RequiredArgsConstructor
public class BidService {
    private final RedissonClient redissonClient;

    public BidResponse placeBid(String auctionId, Long amount, Long bidderId) {
        RLock lock = redissonClient.getLock("auction:lock:" + auctionId);
        try {
            // 최대 5초 대기, 3초 후 자동 해제
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) {
                return doPlaceBid(auctionId, amount, bidderId);
            }
            throw new ConcurrentBidException("입찰 처리 중입니다. 잠시 후 다시 시도해주세요.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

| 상황 | 추천 |
|------|------|
| 단일 인스턴스 | DB 비관적 락 |
| 다중 인스턴스 + 단일 DB | DB 비관적 락 (여전히 유효) |
| 다중 인스턴스 + DB 샤딩 | Redis 분산 락 |
