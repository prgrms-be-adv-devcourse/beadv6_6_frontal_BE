# Biddy 경매 시스템 상세 기술 문서

## 📌 개요
Biddy 경매 시스템은 실시간 온라인 경매 플랫폼으로, 마이크로서비스 아키텍처 기반으로 설계되었습니다.
Spring Boot, WebSocket, Kafka, Redis 등의 기술 스택을 활용하여 실시간성, 확장성, 신뢰성을 보장합니다.

## 🏗️ 시스템 아키텍처

### 헥사고날 아키텍처 (Hexagonal Architecture)
```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │Controller│  │WebSocket │  │  Kafka   │  │Scheduler │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │ Service  │  │ UseCase  │  │   DTO    │                 │
│  └──────────┘  └──────────┘  └──────────┘                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│  │ Auction  │  │   Bid    │  │  Watch   │                │
│  │  Entity  │  │  Entity  │  │  Entity  │                │
│  └──────────┘  └──────────┘  └──────────┘                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │   JPA    │  │  Redis   │  │  Kafka   │  │WebSocket │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 📊 핵심 도메인 모델 상세 분석

### 1. Auction Entity (경매)
```java
// 파일: auction/src/main/java/com/biddy/auction/auction/domain/model/Auction.java

@Entity
@Table(name = "auction")
public class Auction extends BaseEntity {

    // ===== 식별자 =====
    @Id
    @Column(name = "auction_id", length = 20)
    private String auctionId;           // 경매 고유 ID (A-XXXXX 형식)

    // ===== 참조 필드 =====
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;              // Product Service 참조 (FK 없음)

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;               // 판매자 회원 ID

    // ===== 가격 정보 =====
    @Column(name = "start_price", nullable = false)
    private Long startPrice;             // 시작가

    @Column(name = "min_increment", nullable = false)
    private Long minIncrement;           // 최소 입찰 단위

    @Column(name = "current_bid", nullable = false)
    private Long currentBid = 0L;        // 현재 최고 입찰가

    @Column(name = "current_bidder_id")
    private Long currentBidderId;        // 현재 최고 입찰자

    // ===== 통계 정보 =====
    @Column(name = "bid_count", nullable = false)
    private Integer bidCount = 0;        // 총 입찰 수

    @Column(name = "watcher_count", nullable = false)
    private Integer watcherCount = 0;    // 관심 등록 수

    // ===== 상태 관리 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AuctionStatus status = AuctionStatus.LIVE;  // LIVE | ENDED

    // ===== 시간 정보 =====
    @Column(name = "starts_at")
    private LocalDateTime startsAt;      // 시작 시각

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;         // 종료 시각

    // ===== 낙찰 정보 =====
    @Column(name = "winner_id")
    private Long winnerId;                // 낙찰자 ID

    @Column(name = "winning_bid_id")
    private Long winningBidId;            // 낙찰 입찰 ID

    // ===== 핵심 도메인 메서드 =====

    /**
     * 입찰 적용 - 현재가와 입찰자를 원자적으로 갱신
     * 동시성 제어: Pessimistic Lock 하에서만 호출됨
     */
    public void applyBid(Long bidAmount, Long bidderId) {
        this.currentBid = bidAmount;      // 새로운 최고가
        this.currentBidderId = bidderId;  // 새로운 최고 입찰자
        this.bidCount++;                   // 입찰 수 증가
    }

    /**
     * 경매 종료 (낙찰)
     * 스케줄러나 판매자 즉시 종료 시 호출
     */
    public void close(Long winnerId, Long winningBidId) {
        this.status = AuctionStatus.ENDED;
        this.winnerId = winnerId;
        this.winningBidId = winningBidId;
    }

    /**
     * 경매 종료 (유찰)
     * 입찰이 없을 때 호출
     */
    public void closeUnsold() {
        this.status = AuctionStatus.ENDED;
        // winnerId, winningBidId는 null로 유지
    }
}
```

#### Auction 데이터 구조
```
Auction {
  auctionId: "A-B3F2D",
  productId: 12345,
  sellerId: 100,

  // 가격 정보
  startPrice: 10000,
  minIncrement: 500,
  currentBid: 25000,      // 실시간 갱신
  currentBidderId: 201,   // 실시간 갱신

  // 통계
  bidCount: 15,           // 실시간 증가
  watcherCount: 42,       // Redis 캐싱

  // 상태
  status: "LIVE" → "ENDED",
  startsAt: "2024-01-15T09:00:00",
  endsAt: "2024-01-15T21:00:00",

  // 낙찰 (종료 후)
  winnerId: 201,
  winningBidId: 8472,

  // 감사 필드 (BaseEntity)
  createdAt: "2024-01-15T09:00:00",
  updatedAt: "2024-01-15T15:32:10"
}
```

### 2. Bid Entity (입찰)
```java
// 파일: auction/src/main/java/com/biddy/auction/bid/domain/model/Bid.java

@Entity
@Table(name = "bid", indexes = {
    @Index(name = "idx_bid_auction_bid_at", columnList = "auction_id, bid_at DESC"),
    @Index(name = "idx_bid_auction_amount", columnList = "auction_id, amount DESC")
})
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long bidId;                  // 입찰 고유 ID

    @Column(name = "auction_id", nullable = false, length = 20)
    private String auctionId;            // 경매 ID (Auction 참조)

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;               // 입찰자 회원 ID

    @Column(name = "amount", nullable = false)
    private Long amount;                 // 입찰 금액

    @Column(name = "bid_at", nullable = false)
    private LocalDateTime bidAt;         // 입찰 시각

    @PrePersist
    protected void onCreate() {
        this.bidAt = LocalDateTime.now();   // 자동 타임스탬프
    }
}
```

#### Bid 데이터 구조 (불변 레코드)
```
Bid {
  bidId: 8472,
  auctionId: "A-B3F2D",
  bidderId: 201,
  amount: 25000,
  bidAt: "2024-01-15T15:32:10.123"
}

// 인덱스 최적화로 빠른 조회
// 1. 최신순 조회: idx_bid_auction_bid_at
// 2. 최고가 조회: idx_bid_auction_amount
```

## 🔄 핵심 비즈니스 로직 상세 분석

### 1. AuctionService - 경매 관리
```java
// 파일: auction/src/main/java/com/biddy/auction/auction/application/service/AuctionService.java

@Service
@RequiredArgsConstructor
public class AuctionService implements AuctionUseCase {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WatchRedisRepository watchRedis;
    private final AuctionWebSocketPublisher webSocketPublisher;
    private final AuctionEndedEventProducer auctionEndedEventProducer;

    /**
     * [라인 56-69] 경매 피드 조회
     * - 동적 정렬 지원: ending(마감임박), price(가격순), latest(최신순)
     * - 페이징 처리
     * - Redis에서 실시간 관심 수 조회
     */
    @Transactional(readOnly = true)
    public Page<AuctionFeedResult> getAuctionFeed(AuctionFeedQuery query) {
        Sort sort = resolveSort(query.sort());  // 정렬 조건 변환
        Pageable pageable = PageRequest.of(query.page(), query.size(), sort);

        // 마감임박 정렬 시 LIVE 경매만 조회
        AuctionStatus statusFilter = "ending".equals(query.sort())
            && query.status() == null
            ? AuctionStatus.LIVE
            : query.status();

        return auctionRepository.findByFilters(statusFilter, pageable)
            .map(auction -> AuctionFeedResult.from(
                auction,
                watchRedis.getCount(auction.getAuctionId())  // Redis 캐시 조회
            ));
    }

    /**
     * [라인 83-96] 경매 상세 조회
     * - 경매 기본 정보
     * - 사용자별 관심 상태 (Redis)
     * - 사용자의 최고 입찰가
     */
    @Transactional(readOnly = true)
    public AuctionDetailResult getAuctionDetail(String auctionId, Long memberId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // Redis에서 관심 상태 확인
        boolean isWatching = memberId != null
            && watchRedis.isWatching(memberId, auctionId);
        int watcherCount = watchRedis.getCount(auctionId);

        // 내 최고 입찰가 조회
        Long myHighestBid = memberId != null
            ? bidRepository.findTopByAuctionIdAndBidderId(auctionId, memberId)
                .map(Bid::getAmount).orElse(null)
            : null;

        return AuctionDetailResult.from(auction, isWatching, watcherCount, myHighestBid);
    }

    /**
     * [라인 135-158] Kafka 이벤트 기반 경매 생성
     * - Product Service의 경매 등록 이벤트 수신
     * - 경매 ID 자동 생성 (A-XXXXX)
     * - 중복 방지 체크
     */
    @Transactional
    public void createFromProduct(ProductAuctionRegisteredPayload payload) {
        String auctionId = generateAuctionId();  // A-UUID 형식

        if (auctionRepository.existsById(auctionId)) {
            log.warn("경매 이미 존재: auctionId={}", auctionId);
            return;
        }

        Auction auction = Auction.builder()
            .auctionId(auctionId)
            .productId(payload.productId())
            .sellerId(payload.sellerId())
            .startPrice(payload.startPrice())
            .currentBid(payload.startPrice())  // 초기값 = 시작가
            .minIncrement(payload.minIncrement())
            .startsAt(payload.startsAt())
            .endsAt(payload.endsAt())
            .build();

        auctionRepository.save(auction);
    }

    /**
     * [라인 161-186] 판매자 즉시 종료
     * - 권한 검증 (판매자 본인만)
     * - 낙찰/유찰 분기 처리
     * - WebSocket & Kafka 이벤트 발행
     */
    @Transactional
    public void closeAuctionBySeller(String auctionId, Long sellerId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // 권한 검증
        if (!auction.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.NOT_AUCTION_OWNER);
        }

        if (!auction.isLive()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }

        if (auction.hasBids()) {
            // 낙찰 처리
            Bid topBid = bidRepository.findTopByAuctionId(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BID_NOT_FOUND));
            auction.close(topBid.getBidderId(), topBid.getBidId());

            // 실시간 알림
            webSocketPublisher.publishEnded(auctionId, topBid.getBidderId(), topBid.getAmount());

            // Order Service에 이벤트 발행
            auctionEndedEventProducer.publish(auction, topBid);
        } else {
            // 유찰 처리
            auction.closeUnsold();
            webSocketPublisher.publishUnsold(auctionId);
        }
    }
}
```

### 2. BidService - 입찰 처리 (동시성 제어)
```java
// 파일: auction/src/main/java/com/biddy/auction/bid/application/service/BidService.java

@Service
@RequiredArgsConstructor
public class BidService implements BidUseCase {

    /**
     * [라인 76-119] 핵심: 입찰 실행 with 동시성 제어
     *
     * 동시성 제어 전략: Two-Phase Validation + Pessimistic Lock
     *
     * Phase 1: Pre-Lock Validation (빠른 실패)
     * - 락 없이 기본 유효성 검증
     * - DB 락 대기 시간 최소화
     *
     * Phase 2: Pessimistic Lock
     * - SELECT ... FOR UPDATE로 경매 레코드 락
     * - 동시 입찰 요청 직렬화
     *
     * Phase 3: Post-Lock Validation (정확성 보장)
     * - 락 획득 후 최신 상태로 재검증
     * - Race condition 방지
     *
     * Phase 4: Atomic Update
     * - 입찰 저장 + 경매 갱신을 하나의 트랜잭션으로
     */
    @Transactional
    public PlaceBidResult placeBid(PlaceBidCommand command) {
        // ===== Phase 1: Pre-Lock Validation =====
        // 락 없이 빠르게 검증 (DB 락 경합 최소화)
        Auction auction = auctionRepository.findById(command.auctionId())
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // 빠른 실패 조건들
        if (!auction.isLive()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }
        if (auction.getSellerId().equals(command.bidderId())) {
            throw new BusinessException(ErrorCode.SELF_BID_NOT_ALLOWED);  // 본인 입찰 금지
        }
        if (command.amount() < auction.getCurrentBid() + auction.getMinIncrement()) {
            throw new BusinessException(ErrorCode.BID_AMOUNT_TOO_LOW);
        }

        // ===== Phase 2: Pessimistic Lock 획득 =====
        // SELECT ... FOR UPDATE - 다른 트랜잭션 대기
        Auction lockedAuction = auctionRepository.findByIdForUpdate(command.auctionId())
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        // ===== Phase 3: Post-Lock Validation =====
        // 락 획득 후 최신 상태로 재검증 (Race condition 방지)
        if (!lockedAuction.isLive()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }
        if (command.amount() < lockedAuction.getCurrentBid() + lockedAuction.getMinIncrement()) {
            throw new BusinessException(ErrorCode.BID_AMOUNT_TOO_LOW);
        }

        // ===== Phase 4: Atomic Update =====
        // 입찰 생성
        Bid bid = Bid.builder()
            .auctionId(command.auctionId())
            .bidderId(command.bidderId())
            .amount(command.amount())
            .build();

        Bid savedBid = bidRepository.save(bid);

        // 경매 상태 갱신 (원자적)
        lockedAuction.applyBid(command.amount(), command.bidderId());
        auctionRepository.save(lockedAuction);

        // ===== Phase 5: Real-time Notification =====
        // WebSocket으로 실시간 브로드캐스트
        webSocketPublisher.publishBid(
            command.auctionId(),
            lockedAuction.getCurrentBid(),
            lockedAuction.getBidCount(),
            command.bidderId()
        );

        return new PlaceBidResult(
            savedBid.getBidId(),
            savedBid.getAmount(),
            lockedAuction.getCurrentBid(),
            lockedAuction.getBidCount()
        );
    }
}
```

#### 동시성 제어 시나리오
```
시간축 →
T1: User A 입찰 시도 (15,000원)
T2: User B 입찰 시도 (15,500원)
T3: User C 입찰 시도 (16,000원)

[Pre-Lock Phase]
T1: 검증 통과 (현재가 14,000원)
T2: 검증 통과 (현재가 14,000원)
T3: 검증 통과 (현재가 14,000원)

[Lock Phase] - 직렬화
T1: 락 획득 ✓
T2: 대기 ⏳
T3: 대기 ⏳

[Post-Lock & Update]
T1: 재검증 통과 → 입찰 성공 → 현재가 15,000원
T2: 락 획득 → 재검증 실패 (15,500 < 15,000 + 500) → 예외
T3: 락 획득 → 재검증 통과 → 입찰 성공 → 현재가 16,000원

결과: A와 C만 입찰 성공, B는 금액 부족으로 실패
```

### 3. WebSocket 실시간 통신
```java
// 파일: auction/src/main/java/com/biddy/auction/auction/infra/websocket/AuctionWebSocketPublisher.java

@Component
@RequiredArgsConstructor
public class AuctionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * [라인 35-40] 입찰 이벤트 브로드캐스트
     * 채널: /topic/auctions/{auctionId}
     * 메시지: {type: "BID", currentBid: 25000, bidCount: 15, bidderId: 201}
     */
    public void publishBid(String auctionId, Long currentBid, Integer bidCount, Long bidderId) {
        AuctionWebSocketMessage message = AuctionWebSocketMessage.bid(currentBid, bidCount, bidderId);
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * [라인 49-54] 낙찰 이벤트 브로드캐스트
     * 메시지: {type: "ENDED", winnerId: 201, finalBid: 25000}
     */
    public void publishEnded(String auctionId, Long winnerId, Long finalBid) {
        AuctionWebSocketMessage message = AuctionWebSocketMessage.ended(winnerId, finalBid);
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * [라인 61-66] 유찰 이벤트 브로드캐스트
     * 메시지: {type: "UNSOLD"}
     */
    public void publishUnsold(String auctionId) {
        AuctionWebSocketMessage message = AuctionWebSocketMessage.unsold();
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
    }
}
```

#### WebSocket 메시지 흐름
```
┌─────────┐     STOMP      ┌─────────┐     Subscribe    ┌─────────┐
│Client A │────────────────▶│ Broker  │◀─────────────────│Client B │
└─────────┘                 └─────────┘                  └─────────┘
     │                           │                            │
     │   SUBSCRIBE               │                            │
     │   /topic/auctions/A-B3F2D│                            │
     │──────────────────────────▶│                            │
     │                           │                            │
     │                      [입찰 발생]                        │
     │                           │                            │
     │◀──────────────────────────│────────────────────────────▶
     │   MESSAGE                 │         MESSAGE            │
     │   {type:"BID",           │                            │
     │    currentBid:25000,     │                            │
     │    bidCount:15}          │                            │
```

### 4. Kafka 이벤트 기반 통신
```java
// 파일: auction/src/main/java/com/biddy/auction/auction/infra/kafka/AuctionEndedEventProducer.java

@Component
@RequiredArgsConstructor
public class AuctionEndedEventProducer {

    private static final String TOPIC = "auction.ended";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * [라인 41-60] Transactional Outbox Pattern
     *
     * 문제: 비즈니스 트랜잭션과 이벤트 발행의 원자성
     * 해결: Outbox 테이블을 통한 2단계 처리
     *
     * Step 1: 비즈니스 트랜잭션 내에서 Outbox에 이벤트 저장
     * Step 2: 별도 스케줄러가 Outbox를 폴링하여 Kafka 발행
     *
     * 장점:
     * - 트랜잭션 롤백 시 이벤트도 함께 취소
     * - Kafka 장애 시에도 이벤트 유실 방지
     * - At-least-once delivery 보장
     */
    public void publish(Auction auction, Bid topBid) {
        // 이벤트 페이로드 생성
        AuctionEndedEventPayload payload = AuctionEndedEventPayload.from(auction, topBid);

        try {
            // JSON 직렬화
            String json = objectMapper.writeValueAsString(payload);

            // Outbox 테이블에 저장 (현재 트랜잭션 내)
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("AUCTION")
                .aggregateId(auction.getAuctionId())
                .topic(TOPIC)
                .payload(json)
                .build();

            outboxEventRepository.save(outboxEvent);

            log.info("Outbox 이벤트 저장: auctionId={}, winnerId={}, finalBid={}",
                auction.getAuctionId(), topBid.getBidderId(), topBid.getAmount());

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("경매 종료 이벤트 직렬화 실패", e);
        }
    }
}
```

#### Kafka 이벤트 흐름
```
┌───────────────┐   경매 등록   ┌───────────────┐
│Product Service│─────────────▶│Auction Service│
└───────────────┘               └───────────────┘
                                        │
                              [경매 진행 & 종료]
                                        │
                                        ▼
                              ┌─────────────────┐
                              │  Outbox Table   │
                              └─────────────────┘
                                        │
                              [OutboxRelayScheduler]
                                        │
                                        ▼
                                  Kafka Topic
                                "auction.ended"
                                        │
                                        ▼
                              ┌─────────────────┐
                              │  Order Service  │
                              └─────────────────┘
                                        │
                                  [주문 생성]
```

### 5. AuctionCloseScheduler - 자동 종료
```java
// 파일: auction/src/main/java/com/biddy/auction/auction/application/scheduler/AuctionCloseScheduler.java

@Component
@RequiredArgsConstructor
public class AuctionCloseScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionWebSocketPublisher webSocketPublisher;
    private final AuctionEndedEventProducer auctionEndedEventProducer;

    /**
     * [라인 53-71] 1초 간격 경매 종료 스케줄러
     *
     * 처리 흐름:
     * 1. 만료된 LIVE 경매 조회 (ends_at <= now)
     * 2. 각 경매별 낙찰/유찰 판정
     * 3. 상태 변경 (LIVE → ENDED)
     * 4. WebSocket & Kafka 이벤트 발행
     *
     * @Transactional 보장:
     * - 경매 상태 변경의 원자성
     * - Dirty Checking으로 자동 DB 반영
     * - 실패 시 전체 롤백
     */
    @Scheduled(fixedDelay = 1000)  // 1초 간격
    @Transactional
    public void processExpiredAuctions() {
        // 종료 시각이 지난 LIVE 경매 조회
        List<Auction> expired = auctionRepository.findExpiredLiveAuctions(LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("종료 대상 경매 {}건 발견", expired.size());

        for (Auction auction : expired) {
            try {
                closeAuction(auction);
            } catch (Exception e) {
                // 개별 경매 실패 시 다른 경매 처리 계속
                log.error("경매 종료 처리 실패: auctionId={}", auction.getAuctionId(), e);
            }
        }
    }

    /**
     * [라인 78-85] 개별 경매 종료 처리
     */
    private void closeAuction(Auction auction) {
        if (auction.hasBids()) {
            handleAwarded(auction);  // 낙찰
        } else {
            auction.closeUnsold();
            handleUnsold(auction);    // 유찰
        }
    }

    /**
     * [라인 91-112] 낙찰 처리
     */
    private void handleAwarded(Auction auction) {
        // 최고 입찰 조회
        Bid topBid = bidRepository.findTopByAuctionId(auction.getAuctionId())
            .orElse(null);

        if (topBid == null) {
            auction.closeUnsold();
            handleUnsold(auction);
            return;
        }

        // 경매 종료 + 낙찰자 확정
        auction.close(topBid.getBidderId(), topBid.getBidId());

        // WebSocket 실시간 알림
        webSocketPublisher.publishEnded(
            auction.getAuctionId(),
            topBid.getBidderId(),
            topBid.getAmount()
        );

        // Kafka로 Order Service에 이벤트 발행
        auctionEndedEventProducer.publish(auction, topBid);

        log.info("낙찰 처리 완료: auctionId={}, winnerId={}, finalBid={}",
            auction.getAuctionId(), topBid.getBidderId(), topBid.getAmount());
    }

    /**
     * [라인 118-122] 유찰 처리
     */
    private void handleUnsold(Auction auction) {
        // WebSocket 실시간 알림만 (Order 생성 불필요)
        webSocketPublisher.publishUnsold(auction.getAuctionId());

        log.info("유찰 처리 완료: auctionId={}", auction.getAuctionId());
    }
}
```

#### 스케줄러 실행 타임라인
```
시간 →
00:00 ───────────────────────────────────────────────────────▶
      │     │     │     │     │     │     │     │
      ▼     ▼     ▼     ▼     ▼     ▼     ▼     ▼
    [1초]  [2초]  [3초]  [4초]  [5초]  [6초]  [7초]  [8초]

각 실행 시점:
1. SELECT * FROM auction
   WHERE status = 'LIVE'
   AND ends_at <= NOW()

2. 조회된 각 경매:
   - 입찰 있음 → 낙찰 처리
     └─ auction.close()
     └─ WebSocket ENDED
     └─ Kafka auction.ended

   - 입찰 없음 → 유찰 처리
     └─ auction.closeUnsold()
     └─ WebSocket UNSOLD
```

## 🔀 데이터 흐름 시각화

### 1. 입찰 프로세스 전체 흐름
```
┌──────────┐
│  Client  │
└─────┬────┘
      │ POST /api/v1/auctions/{id}/bids
      ▼
┌──────────────────────────────────────────────────────┐
│                   BidController                      │
│  1. 요청 수신                                        │
│  2. PlaceBidCommand 생성                            │
└──────────────────────┬───────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────┐
│                    BidService                        │
│                                                      │
│  Phase 1: Pre-Lock Validation                       │
│  ┌────────────────────────────┐                    │
│  │ - 경매 존재 확인           │                    │
│  │ - 경매 상태 (LIVE) 확인   │                    │
│  │ - 본인 입찰 방지          │                    │
│  │ - 최소 금액 검증          │                    │
│  └────────────────────────────┘                    │
│                                                      │
│  Phase 2: Pessimistic Lock                          │
│  ┌────────────────────────────┐                    │
│  │ SELECT ... FOR UPDATE      │──────▶ [DB Lock]   │
│  └────────────────────────────┘                    │
│                                                      │
│  Phase 3: Post-Lock Validation                      │
│  ┌────────────────────────────┐                    │
│  │ - 최신 상태 재검증        │                    │
│  │ - 금액 재확인             │                    │
│  └────────────────────────────┘                    │
│                                                      │
│  Phase 4: Transaction Update                        │
│  ┌────────────────────────────┐                    │
│  │ 1. Bid 엔티티 저장        │──────▶ [bid 테이블] │
│  │ 2. Auction.applyBid()     │──────▶ [auction 테이블] │
│  └────────────────────────────┘                    │
└──────────────────────┬───────────────────────────────┘
                       ▼
        ┌──────────────────────────────┐
        │   WebSocketPublisher        │
        │   /topic/auctions/{id}      │
        └───────────┬──────────────────┘
                    ▼
    ┌───────────────────────────────────┐
    │       모든 구독 클라이언트        │
    │  실시간 가격/입찰수 업데이트     │
    └───────────────────────────────────┘
```

### 2. 경매 생성 ~ 종료 전체 라이프사이클
```
[Product Service]
      │
      │ 1. 상품 경매 등록
      ▼
Kafka Topic: "product.auction.registered"
      │
      ▼
[Auction Service - ProductAuctionRegisteredConsumer]
      │
      │ 2. 경매 자동 생성
      ▼
┌─────────────────────────────────┐
│         auction 테이블          │
│  status: LIVE                   │
│  ends_at: 2024-01-15 21:00:00  │
└─────────────────────────────────┘
      │
      │ 3. 경매 진행 (입찰 반복)
      ▼
[시간 경과...]
      │
      ▼
[AuctionCloseScheduler - 1초마다 실행]
      │
      │ 4. 만료 경매 감지 (ends_at <= now)
      ▼
┌─────────────────────────────────┐
│       입찰 여부 확인            │
└────────┬──────────┬─────────────┘
         │          │
    [입찰 있음]  [입찰 없음]
         │          │
         ▼          ▼
    ┌────────┐  ┌────────┐
    │  낙찰  │  │  유찰  │
    └────┬───┘  └────┬───┘
         │           │
         ▼           ▼
    WebSocket    WebSocket
    "ENDED"      "UNSOLD"
         │
         ▼
    Kafka Topic
    "auction.ended"
         │
         ▼
   [Order Service]
    주문 생성
```

### 3. Redis 캐싱 전략 (Watch 기능)
```
┌──────────────────────────────────────────┐
│           Watch 관심 등록 흐름           │
└──────────────────────────────────────────┘
                    │
                    ▼
        POST /api/v1/auctions/{id}/watch
                    │
                    ▼
           ┌────────────────┐
           │  WatchService  │
           └────────┬───────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
  [DB 저장/삭제]          [Redis 캐시]
  auction_watch 테이블    watch:{auctionId}
  (영구 저장)            (Set 자료구조)
        │                       │
        │                   SADD/SREM
        │                   SCARD (count)
        │                   SISMEMBER (check)
        ▼                       ▼
  ┌──────────┐          ┌──────────────┐
  │ 영구성   │          │ 빠른 조회    │
  │ 보장     │          │ 실시간 집계  │
  └──────────┘          └──────────────┘

캐시 워밍업 (서버 시작 시):
WatchCacheWarmup.init()
  └─ DB 전체 조회
  └─ Redis 일괄 로드
  └─ TTL 설정 (경매 종료 + 1일)
```

### 4. Outbox Pattern 상세
```
┌────────────────────────────────────────────┐
│         Transactional Outbox Pattern       │
└────────────────────────────────────────────┘

[비즈니스 트랜잭션]
BEGIN TRANSACTION;
  1. UPDATE auction SET status = 'ENDED'
  2. INSERT INTO outbox_event (
       aggregate_id: 'A-B3F2D',
       topic: 'auction.ended',
       payload: '{...}',
       status: 'PENDING'
     )
COMMIT;

[별도 스케줄러 - OutboxRelayScheduler]
Every 5 seconds:
  1. SELECT * FROM outbox_event
     WHERE status = 'PENDING'
     ORDER BY created_at
     LIMIT 100

  2. For each event:
     - Kafka 발행 시도
     - 성공 → status = 'PUBLISHED'
     - 실패 → retry_count++

  3. 3회 실패 → status = 'FAILED'

장점:
✓ 트랜잭션 보장
✓ 이벤트 유실 방지
✓ 순서 보장
✓ 재시도 메커니즘
```

## 🔐 동시성 제어 메커니즘

### Pessimistic Lock 상세 분석
```sql
-- BidService.placeBid() 실행 시 생성되는 SQL

-- Phase 1: 일반 조회 (락 없음)
SELECT * FROM auction WHERE auction_id = 'A-B3F2D';

-- Phase 2: 비관적 락 (다른 트랜잭션 차단)
SELECT * FROM auction
WHERE auction_id = 'A-B3F2D'
FOR UPDATE;  -- 행 수준 배타적 락

-- Phase 3: 입찰 저장
INSERT INTO bid (auction_id, bidder_id, amount, bid_at)
VALUES ('A-B3F2D', 201, 25000, NOW());

-- Phase 4: 경매 갱신
UPDATE auction
SET current_bid = 25000,
    current_bidder_id = 201,
    bid_count = bid_count + 1
WHERE auction_id = 'A-B3F2D';

-- 트랜잭션 종료 시 락 해제
COMMIT;
```

### 동시 입찰 시나리오 분석
```
시나리오: 3명이 동시에 입찰

초기 상태:
- 현재가: 10,000원
- 최소 증분: 500원

User A: 11,000원 입찰
User B: 11,200원 입찰
User C: 11,500원 입찰

[타임라인]
T0: 모든 사용자 동시 요청
│
├─ User A: Pre-Lock 통과 (11,000 > 10,500 ✓)
├─ User B: Pre-Lock 통과 (11,200 > 10,500 ✓)
└─ User C: Pre-Lock 통과 (11,500 > 10,500 ✓)
│
T1: Lock 경합
├─ User A: Lock 획득 ✓
├─ User B: Lock 대기 ⏳
└─ User C: Lock 대기 ⏳
│
T2: User A 처리 완료
├─ Post-Lock 검증 통과
├─ 입찰 성공
└─ 현재가: 11,000원
│
T3: User B Lock 획득
├─ Post-Lock 검증 실패 (11,200 < 11,500 ✗)
└─ BID_AMOUNT_TOO_LOW 예외
│
T4: User C Lock 획득
├─ Post-Lock 검증 통과 (11,500 ≥ 11,500 ✓)
├─ 입찰 성공
└─ 현재가: 11,500원

최종 결과:
- A: 성공 (11,000원)
- B: 실패 (금액 부족)
- C: 성공 (11,500원)
- 최종 최고가: 11,500원
```

## 🎯 성능 최적화 전략

### 1. 인덱스 최적화
```sql
-- Bid 테이블 인덱스
CREATE INDEX idx_bid_auction_bid_at
ON bid(auction_id, bid_at DESC);  -- 최신 입찰 조회

CREATE INDEX idx_bid_auction_amount
ON bid(auction_id, amount DESC);  -- 최고가 조회

-- Auction 테이블 인덱스
CREATE INDEX idx_auction_status_ends_at
ON auction(status, ends_at);  -- 만료 경매 조회

CREATE UNIQUE INDEX idx_auction_product
ON auction(product_id);  -- 상품별 유일성
```

### 2. Redis 캐싱
```
Watch Count 캐싱:
- Key: watch:{auctionId}
- Type: Set
- Members: 관심 등록한 사용자 ID들
- Operations:
  - SADD: O(1)
  - SREM: O(1)
  - SCARD: O(1)
  - SISMEMBER: O(1)

TTL 전략:
- 경매 종료 시각 + 24시간
- 메모리 효율적 관리
```

### 3. 배치 처리
```java
// OutboxRelayScheduler
@Scheduled(fixedDelay = 5000)
public void relay() {
    // 한 번에 100개씩 배치 처리
    List<OutboxEvent> events = repository.findPending(100);

    // 병렬 처리
    events.parallelStream()
        .forEach(this::publishToKafka);
}
```

## 📈 모니터링 포인트

### 핵심 메트릭
1. **입찰 성공률**: 전체 시도 대비 성공 비율
2. **락 대기 시간**: Pessimistic Lock 평균 대기 시간
3. **스케줄러 지연**: 경매 종료 시각과 실제 처리 시각 차이
4. **WebSocket 연결 수**: 동시 접속자 수
5. **Kafka 지연**: Outbox → Kafka 발행 지연 시간

### 알람 설정
```yaml
alerts:
  - name: "높은 락 경합"
    condition: lock_wait_time > 500ms

  - name: "스케줄러 지연"
    condition: auction_close_delay > 5s

  - name: "Outbox 적체"
    condition: pending_events > 1000
```

## 🔧 트러블슈팅 가이드

### 문제 1: 동시 입찰 시 데드락
**원인**: 여러 경매에 대한 교차 입찰
**해결**: 락 순서 일관성 유지, 타임아웃 설정

### 문제 2: WebSocket 연결 끊김
**원인**: 네트워크 불안정, 서버 재시작
**해결**: 자동 재연결, 하트비트 구현

### 문제 3: Kafka 이벤트 유실
**원인**: Kafka 장애, 네트워크 문제
**해결**: Outbox Pattern으로 신뢰성 보장

## 📚 참고 자료
- Spring Data JPA Pessimistic Locking
- STOMP over WebSocket Protocol
- Transactional Outbox Pattern
- Redis Data Structures
- Kafka Event Sourcing

---
문서 작성일: 2024-01-15
작성자: Biddy Auction Team
버전: 1.0.0

---

## 🧪 로컬 PC에서 AWS Auction을 대상으로 하는 k6 테스트 방안

### 1. 현재 테스트 방식

현재 프로젝트는 별도의 k6 전용 AWS 인스턴스를 추가하지 않고, 개발자 PC에서 k6를 실행하여 AWS에 배포된 Auction 기능을 테스트한다.

```text
개발자 PC의 k6
  → 인터넷
  → AWS 외부 진입점/API Gateway
  → Auction Service
  → NAS PostgreSQL
```

이 방식은 AWS 내부 서버만의 순수 처리량보다는 인터넷 구간을 포함해 실제 외부 사용자가 체감하는 성능을 측정한다.

### 2. 전용 AWS k6 인스턴스와의 차이

| 구분 | AWS 동일 리전 전용 k6 서버 | 개발자 PC에서 AWS 호출 |
|---|---|---|
| 주요 측정 대상 | Auction 서버와 DB의 처리 한계 | 인터넷을 포함한 사용자 체감 성능 |
| 네트워크 영향 | 비교적 일정함 | 통신사, 공유기, Wi-Fi 등의 영향을 받음 |
| 부하 발생기 영향 | 전용 자원이라 작음 | PC의 CPU, 메모리, 실행 프로그램 영향을 받음 |
| 반복 측정 편차 | 상대적으로 작음 | 시간과 로컬 환경에 따라 커질 수 있음 |
| 실제 사용자 환경 유사성 | 내부 성능 측정에 가까움 | 외부 사용자 접속 환경에 가까움 |

전용 인스턴스가 항상 더 정확한 것은 아니다. Auction 서버 자체의 최대 처리량을 비교할 때는 동일 리전 전용 인스턴스가 유리하고, 외부 사용자의 E2E 응답을 확인할 때는 PC에서 AWS를 호출하는 방식도 의미가 있다.

### 3. 결과 해석 원칙

로컬 PC에서 얻은 수치는 다음과 같이 표현한다.

> 현재 개발자 PC와 인터넷 환경에서 AWS Auction API를 외부 호출하여 관측한 결과

따라서 측정된 TPS를 Auction 서버만의 절대 최대 TPS로 단정하지 않는다. 다음 목적으로 사용하는 것이 적절하다.

- 비관적 락, 낙관적 락, 조건부 UPDATE 적용 전후의 상대 비교
- 동시 입찰 시 데이터 정합성 확인
- 실제 외부 요청 기준의 p95, p99 응답 시간 확인
- WebSocket 연결과 메시지 전달 확인
- 부하 증가에 따른 AWS Auction과 NAS DB의 병목 위치 확인

### 4. 로컬 실행 조건

측정 편차를 줄이기 위해 다음 조건을 지킨다.

1. 가능한 경우 Wi-Fi보다 유선 네트워크를 사용한다.
2. Docker와 Colima를 동시에 실행하지 않고 필요한 런타임 하나만 실행한다.
3. IntelliJ 빌드, 브라우저 다운로드, 클라우드 동기화 등 무거운 작업을 중지한다.
4. k6는 GUI 없이 CLI로 실행한다.
5. 동일 시나리오를 같은 시간대에 3~5회 반복한다.
6. 평균값만 사용하지 않고 반복 결과의 중앙값과 p95, p99를 함께 기록한다.
7. 테스트 중 개발자 PC의 CPU, 메모리, 네트워크 사용률을 확인한다.
8. 같은 시간대의 AWS Auction Pod, JVM, DB 지표를 함께 저장한다.

### 5. 단계별 부하 증가

처음부터 큰 부하를 주지 않고 단계적으로 증가시킨다.

```text
Smoke: 1~2 VU
  → Baseline: 10 VU
  → Normal Load: 30 VU
  → Stress: 50 VU
  → High Stress: 100 VU 이상
```

각 단계에서 다음 항목을 확인한 뒤 다음 단계로 이동한다.

- k6 요청률이 목표치에 도달하는가
- `dropped_iterations`가 발생하지 않는가
- 로컬 PC CPU와 네트워크가 포화되지 않는가
- AWS Auction Pod의 CPU와 메모리가 증가하는가
- DB 커넥션 풀 대기와 락 대기 시간이 증가하는가
- Auction 현재가, 최고 입찰자, 입찰 수가 Bid 이력과 일치하는가

### 6. 로컬 부하 발생기가 병목인 경우

다음 현상이 나타나면 AWS Auction 서버가 아니라 개발자 PC 또는 인터넷 회선이 먼저 한계에 도달했을 가능성이 있다.

- 로컬 CPU 사용률이 지속적으로 80~90% 이상임
- k6의 `dropped_iterations`가 증가함
- 설정한 요청률에 도달하지 못함
- 로컬 업로드 대역폭이 포화됨
- AWS CPU와 DB 사용률은 낮은데 응답 시간만 증가함
- WebSocket 연결이 특정 개수부터 갑자기 실패함

이 경우 해당 단계의 결과를 서버 최대 처리량으로 사용하지 않는다. VU 또는 요청률을 낮추고 로컬 환경을 정리한 뒤 다시 측정한다.

### 7. AWS 서버 지표와 함께 판단하기

k6 결과만으로 병목을 판단하지 않고 같은 시간대의 서버 지표를 함께 비교한다.

| k6/로컬 상태 | AWS 상태 | 판단 |
|---|---|---|
| 로컬 CPU 높음, AWS CPU 낮음 | 서버 여유 있음 | 로컬 부하 발생기 병목 가능성 |
| 로컬 여유 있음, Auction CPU 높음 | 응답 지연 증가 | Auction 애플리케이션 병목 가능성 |
| Auction CPU 낮음, DB 대기 증가 | 락·커넥션 대기 증가 | PostgreSQL 또는 NAS 연결 병목 가능성 |
| API 정상, WebSocket 오류 증가 | 연결 수 증가 | WebSocket 또는 네트워크 제한 확인 |
| 409 응답과 락 충돌 증가 | 시스템 오류는 낮음 | 동시성 제어 정책의 정상 업무 충돌 여부 확인 |

### 8. K3s 내부에서 k6를 실행하지 않는 이유

추가 인스턴스가 없는 상태에서 Auction과 같은 K3s 노드에 k6를 실행하면 부하 발생기가 Auction Pod와 CPU, 메모리, 네트워크를 경쟁한다. 이 경우 k6가 테스트 대상의 성능을 직접 떨어뜨려 결과를 왜곡할 수 있다.

따라서 현재 환경에서는 다음 방식을 사용한다.

```text
채택: 개발자 PC에서 k6 실행 → AWS Auction 호출
비채택: Auction이 운영되는 K3s 노드에서 k6 실행
```

### 9. 테스트 결과에 반드시 기록할 정보

- 테스트 실행 날짜와 시간
- k6 스크립트 Git 커밋
- 개발자 PC 사양과 운영체제
- 유선 또는 Wi-Fi 여부
- Docker 또는 Colima 실행 여부
- VU, duration, arrival rate 설정
- 대상 AWS URL과 배포 버전
- Auction Pod 수
- 테스트 Auction ID와 시작 현재가
- k6 p50, p95, p99와 처리량
- `dropped_iterations`, HTTP 실패율, WebSocket 실패 수
- Auction/DB CPU, 메모리, 락 대기, 커넥션 풀 지표
- 테스트 종료 후 DB 데이터 정합성 확인 결과

### 10. 최종 결정

현재 Auction 성능 테스트는 **개발자 PC에서 k6를 실행하여 AWS 환경을 호출하는 방식**으로 진행한다. 결과는 절대적인 서버 최대 성능보다 다음 항목에 초점을 맞춘다.

1. 변경 전후의 상대적인 성능 차이
2. 동시 입찰과 종료 시 데이터 정합성
3. 외부 사용자 관점의 API 및 WebSocket 지연
4. Auction Service와 NAS PostgreSQL 중 실제 병목 위치

추후 별도 부하 발생 인스턴스를 사용할 수 있게 되면 동일 시나리오를 AWS 동일 리전에서 다시 실행하여 로컬 결과와 비교한다.
