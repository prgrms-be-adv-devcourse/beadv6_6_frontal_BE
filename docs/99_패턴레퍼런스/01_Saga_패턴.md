# Saga 패턴

MSA 환경에서 분산 트랜잭션을 관리하기 위한 패턴.
각 서비스의 로컬 트랜잭션을 순차적으로 실행하고, 실패 시 **보상 트랜잭션(Compensating Transaction)**으로 롤백한다.

---

## 1. 왜 Saga가 필요한가

| 모놀리식 | MSA |
|----------|-----|
| 단일 DB → `@Transactional`로 ACID 보장 | 서비스별 DB 분리 → 2PC 불가능 |
| 롤백 = DB가 알아서 처리 | 롤백 = 각 서비스가 보상 트랜잭션을 직접 실행 |

> MSA에서는 **2PC(Two-Phase Commit)**가 성능과 가용성 문제로 비현실적이므로, **Eventually Consistent** 방식인 Saga를 사용한다.

---

## 2. Choreography (코레오그래피)

각 서비스가 **이벤트를 발행/구독**하여 자율적으로 다음 단계를 진행하는 방식.

### 구조

```
Auction        →  [auction.ended]  →  Order         →  [order.created]  →  Payment
(경매 종료)                           (주문 생성)                           (결제 처리)
                                         ↑                                    │
                                         └────  [payment.failed]  ←───────────┘
                                              (보상: 주문 취소)
```

### 적용 예시: 경매 낙찰 → 주문 → 결제

```
정상 흐름:
1. Auction: 경매 종료 → auction.ended 이벤트 발행
2. Order:   auction.ended 구독 → 주문 생성 → order.created 발행
3. Payment: order.created 구독 → 결제 처리 → payment.completed 발행
4. Order:   payment.completed 구독 → 주문 상태 CONFIRMED로 변경

보상 흐름 (결제 실패 시):
3'. Payment: 결제 실패 → payment.failed 발행
4'. Order:   payment.failed 구독 → 주문 취소 (보상 트랜잭션)
5'. Auction: order.cancelled 구독 → 경매 재등록 or 유찰 처리
```

### Spring Boot 구현

```java
// Auction Service — 이벤트 발행
@Service
@RequiredArgsConstructor
public class AuctionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void endAuction(String auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow();
        auction.end();
        auctionRepository.save(auction);

        AuctionEndedEvent event = new AuctionEndedEvent(
            auctionId, auction.getWinnerId(), auction.getCurrentBid()
        );
        kafkaTemplate.send("auction.ended",
            auctionId, objectMapper.writeValueAsString(event));
    }
}

// Order Service — 이벤트 구독 + 다음 이벤트 발행
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "auction.ended", groupId = "order-service")
    public void onAuctionEnded(String message) {
        AuctionEndedEvent event = objectMapper.readValue(message, AuctionEndedEvent.class);
        Order order = orderService.createFromAuction(event);
        kafkaTemplate.send("order.created",
            order.getId(), objectMapper.writeValueAsString(new OrderCreatedEvent(order)));
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    public void onPaymentFailed(String message) {
        // 보상 트랜잭션: 주문 취소
        PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
        orderService.cancelOrder(event.getOrderId());
    }
}
```

### 장단점

| 장점 | 단점 |
|------|------|
| 서비스 간 느슨한 결합 | 흐름 파악이 어려움 (이벤트 추적 필요) |
| 단순한 구현 (이벤트만 발행) | 서비스가 많아지면 이벤트 스파게티 |
| 개별 서비스 독립 배포 가능 | 디버깅/모니터링 복잡 |
| 중앙 장애점(SPOF) 없음 | 순환 의존 발생 가능 |

### 적합한 상황

- 서비스 수가 **3~4개 이하**인 단순한 흐름
- 서비스 간 결합을 최소화하고 싶을 때
- 팀이 독립적으로 서비스를 운영할 때

---

## 3. Orchestration (오케스트레이션)

**중앙 Orchestrator(Saga Coordinator)**가 각 서비스에 명령을 내리고 응답을 받아 다음 단계를 결정하는 방식.

### 구조

```
                    ┌─────────────────────┐
                    │  Saga Orchestrator  │
                    │  (Order Service)    │
                    └──────┬──────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
         ┌─────────┐ ┌─────────┐ ┌──────────┐
         │ Auction │ │ Member  │ │ Payment  │
         │ Service │ │ Service │ │ Service  │
         └─────────┘ └─────────┘ └──────────┘
```

### 적용 예시: 경매 낙찰 → 주문 → 결제

```
정상 흐름:
1. Orchestrator: Auction에 "낙찰 확정" 명령 → 응답 OK
2. Orchestrator: Member에 "예치금 차감" 명령 → 응답 OK
3. Orchestrator: Payment에 "결제 생성" 명령 → 응답 OK
4. Orchestrator: Saga 완료, 주문 상태 CONFIRMED

보상 흐름 (Step 3 실패 시):
3'. Payment 실패 응답
4'. Orchestrator: Member에 "예치금 환급" 보상 명령
5'. Orchestrator: Auction에 "낙찰 취소" 보상 명령
6'. Orchestrator: Saga 실패, 주문 상태 CANCELLED
```

### Spring Boot 구현

```java
// Saga 상태 관리 Entity
@Entity
@Table(name = "order_saga")
public class OrderSaga {
    @Id
    private String sagaId;
    private String orderId;

    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;    // AUCTION_CONFIRM, DEPOSIT_DEDUCT, PAYMENT_CREATE

    @Enumerated(EnumType.STRING)
    private SagaStatus status;       // STARTED, COMPENSATING, COMPLETED, FAILED

    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum SagaStep {
    AUCTION_CONFIRM, DEPOSIT_DEDUCT, PAYMENT_CREATE, COMPLETED
}

// Orchestrator
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    private final AuctionClient auctionClient;   // FeignClient
    private final MemberClient memberClient;
    private final PaymentClient paymentClient;
    private final OrderSagaRepository sagaRepository;

    @Transactional
    public void executeSaga(AuctionEndedEvent event) {
        OrderSaga saga = OrderSaga.start(event);
        sagaRepository.save(saga);

        try {
            // Step 1: 낙찰 확정
            saga.moveTo(SagaStep.AUCTION_CONFIRM);
            auctionClient.confirmWinner(event.getAuctionId());

            // Step 2: 예치금 차감
            saga.moveTo(SagaStep.DEPOSIT_DEDUCT);
            memberClient.deductDeposit(event.getWinnerId(), event.getFinalBid());

            // Step 3: 결제 생성
            saga.moveTo(SagaStep.PAYMENT_CREATE);
            paymentClient.createPayment(saga.getOrderId(), event.getFinalBid());

            // 완료
            saga.complete();
        } catch (Exception e) {
            saga.fail(e.getMessage());
            compensate(saga, event);
        }

        sagaRepository.save(saga);
    }

    private void compensate(OrderSaga saga, AuctionEndedEvent event) {
        saga.startCompensating();

        // 현재 단계 이전까지 역순으로 보상
        switch (saga.getCurrentStep()) {
            case PAYMENT_CREATE:
                // Payment는 실패했으므로 보상 불필요
            case DEPOSIT_DEDUCT:
                memberClient.refundDeposit(event.getWinnerId(), event.getFinalBid());
            case AUCTION_CONFIRM:
                auctionClient.cancelWinner(event.getAuctionId());
        }
    }
}
```

### 비동기 Orchestration (Kafka 기반)

동기 HTTP 대신 Kafka 명령/응답 채널을 사용하면 더 견고해진다.

```
Orchestrator  →  [auction.command]  →  Auction  →  [auction.reply]  →  Orchestrator
Orchestrator  →  [member.command]   →  Member   →  [member.reply]   →  Orchestrator
Orchestrator  →  [payment.command]  →  Payment  →  [payment.reply]  →  Orchestrator
```

### 장단점

| 장점 | 단점 |
|------|------|
| 흐름이 한 곳에서 명확히 보임 | Orchestrator가 SPOF가 될 수 있음 |
| 복잡한 비즈니스 로직 관리 용이 | 서비스 간 결합도 증가 |
| 디버깅/모니터링 쉬움 | Orchestrator에 로직 집중 |
| 보상 트랜잭션 순서 제어 용이 | 추가 인프라 (Saga 상태 저장소) 필요 |

### 적합한 상황

- 서비스 수가 **5개 이상**인 복잡한 흐름
- 비즈니스 로직이 복잡하고 조건 분기가 많을 때
- 전체 흐름의 가시성이 중요할 때

---

## 4. Choreography vs Orchestration 비교

| 기준 | Choreography | Orchestration |
|------|-------------|---------------|
| 제어 방식 | 분산 (각 서비스 자율) | 중앙 (Orchestrator) |
| 결합도 | 낮음 | 중간 |
| 복잡도 | 서비스 수 증가 시 급상승 | 선형 증가 |
| 가시성 | 낮음 (이벤트 추적 필요) | 높음 (한 곳에서 확인) |
| SPOF | 없음 | Orchestrator |
| 보상 트랜잭션 | 각 서비스가 독립 처리 | Orchestrator가 순서 제어 |
| 적합 규모 | 소규모 (3~4 서비스) | 중대규모 (5+ 서비스) |
| 테스트 | 통합 테스트 어려움 | Orchestrator 단위 테스트 용이 |

### 이 프로젝트 추천

```
경매 낙찰 흐름 (Auction → Order → Payment → Member):
→ 서비스 4개, 조건 분기 있음
→ Orchestration 추천 (Order Service가 Orchestrator 역할)

단순 알림 흐름 (입찰 → WebSocket 브로드캐스트):
→ 단순 이벤트 전파
→ Choreography 추천
```
