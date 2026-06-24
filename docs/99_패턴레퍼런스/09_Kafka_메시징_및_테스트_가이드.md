# 09. Kafka 메시징 및 테스트 가이드

## 1. Kafka란?

**분산 이벤트 스트리밍 플랫폼**으로, 서비스 간 비동기 메시지를 안정적으로 전달한다.

```
전통적 방식 (동기 호출)              Kafka (비동기 메시징)
┌────────┐    HTTP    ┌────────┐   ┌────────┐           ┌────────┐
│Auction │ ────────→  │ Order  │   │Auction │ → [Kafka] → │ Order  │
│Service │ ← 응답대기  │Service │   │Service │   토픽     │Service │
└────────┘            └────────┘   └────────┘           └────────┘
 장애 전파, 강결합                    느슨한 결합, 장애 격리
```

### 핵심 개념

```
Producer ──→ Topic(Partition) ──→ Consumer Group
 (발행자)      (메시지 저장소)        (구독자)
```

| 개념 | 설명 |
|------|------|
| **Producer** | 메시지를 발행하는 서비스 |
| **Consumer** | 메시지를 구독하는 서비스 |
| **Topic** | 메시지가 저장되는 카테고리 (테이블과 유사) |
| **Partition** | Topic의 물리적 분할 단위 (병렬 처리용) |
| **Offset** | Partition 내 메시지 순번 (Consumer가 어디까지 읽었는지) |
| **Consumer Group** | 같은 그룹 내 Consumer는 Partition을 분배받음 |
| **Broker** | Kafka 서버 인스턴스 |

---

## 2. Kafka vs 다른 메시징 시스템

| 항목 | Kafka | RabbitMQ | Redis Pub/Sub |
|------|-------|----------|---------------|
| **메시지 보존** | 디스크 영속 (설정된 기간) | 소비 후 삭제 | 보존 안 함 |
| **재처리** | Offset 리셋으로 가능 | 불가 | 불가 |
| **순서 보장** | Partition 내 보장 | 큐 내 보장 | 보장 안 함 |
| **처리량** | 초당 수백만 | 초당 수만 | 초당 수십만 |
| **적합 시나리오** | 이벤트 소싱, 로그, 대용량 | 작업 큐, RPC | 실시간 알림 |

---

## 3. Kafka 아키텍처

### 3.1 Topic과 Partition

```
Topic: auction-events (3 Partitions)

Partition 0: [msg0] [msg3] [msg6] [msg9]  ...
Partition 1: [msg1] [msg4] [msg7] [msg10] ...
Partition 2: [msg2] [msg5] [msg8] [msg11] ...

Key 기반 파티셔닝:
  auctionId = "A-001" → hash("A-001") % 3 = Partition 1
  → 같은 경매의 이벤트는 항상 같은 파티션 → 순서 보장
```

### 3.2 Consumer Group

```
Consumer Group: order-service (3 인스턴스)

Consumer 1 ← Partition 0
Consumer 2 ← Partition 1
Consumer 3 ← Partition 2

→ 각 파티션은 그룹 내 1개 Consumer만 처리
→ 인스턴스 수 ≤ 파티션 수 (초과 시 유휴 Consumer 발생)
```

### 3.3 Offset 관리

```
Partition 0: [0] [1] [2] [3] [4] [5] [6]
                              ↑
                         Committed Offset = 4
                         → Consumer 재시작 시 5번부터 읽음
```

---

## 4. Biddy 프로젝트 적용

### 4.1 이벤트 흐름

```
경매 종료 시:

[Auction Service]
    │
    ├── PUBLISH → Topic: auction.ended
    │              {auctionId, winnerId, finalBid, endsAt}
    │
    ▼
[Order Service]                    [Notification Service]
    │                                    │
    ├── CONSUME → 주문 자동 생성          ├── CONSUME → 낙찰 알림 발송
    │                                    │
    ├── PUBLISH → Topic: order.created   └── WebSocket Push
    │              {orderId, auctionId}
    ▼
[Payment Service]
    │
    └── CONSUME → 결��� 요청 생성
```

### 4.2 Topic 설계

| Topic | Producer | Consumer | Key | 용도 |
|-------|----------|----------|-----|------|
| `auction.ended` | Auction | Order, Notification | auctionId | 경매 종료 이벤트 |
| `bid.placed` | Auction | Notification | auctionId | 새 입찰 알림 |
| `bid.outbid` | Auction | Notification | bidderId | 경쟁 패배 알림 |
| `order.created` | Order | Payment | orderId | 주문 생성 이벤트 |
| `payment.completed` | Payment | Order | orderId | 결제 완료 이벤트 |
| `payment.failed` | Payment | Order | orderId | 결제 실패 (보상) |

### 4.3 이벤트 메시지 형식

```json
{
  "eventId": "evt-uuid-001",
  "eventType": "AUCTION_ENDED",
  "timestamp": "2026-06-12T15:30:00",
  "payload": {
    "auctionId": "A-FNF97",
    "winnerId": 42,
    "finalBid": 720000,
    "sellerId": 1
  }
}
```

---

## 5. Spring Boot + Kafka 구현

### 5.1 설정 (현재 application.yaml)

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: auction-service
      auto-offset-reset: earliest        # 처음부터 읽기
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 5.2 Producer (이벤트 발행)

```java
@Component
@RequiredArgsConstructor
public class AuctionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 경매 종료 이벤트를 발행한다.
     * Key = auctionId → 같은 경매의 이벤트는 같은 파티션에 순서 보장
     */
    public void publishAuctionEnded(AuctionEndedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("auction.ended", event.auctionId(), payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
```

### 5.3 Consumer (이벤트 구독)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventConsumer {

    private final ObjectMapper objectMapper;

    /**
     * 입찰 이벤트를 수신하여 처리한다.
     * groupId로 Consumer Group을 지정하여 파티션 분배를 받는다.
     */
    @KafkaListener(topics = "bid.placed", groupId = "auction-service")
    public void handleBidPlaced(String message) {
        try {
            BidPlacedEvent event = objectMapper.readValue(message, BidPlacedEvent.class);
            log.info("입찰 이벤트 수신: auctionId={}, amount={}",
                    event.auctionId(), event.amount());
            // 비즈니스 로직 처리
        } catch (JsonProcessingException e) {
            log.error("이벤트 역직렬화 실패", e);
        }
    }
}
```

### 5.4 이벤트 DTO

```java
public record AuctionEndedEvent(
        String eventId,
        String auctionId,
        Long winnerId,
        Long finalBid,
        LocalDateTime timestamp
) {
    public static AuctionEndedEvent of(String auctionId, Long winnerId, Long finalBid) {
        return new AuctionEndedEvent(
                UUID.randomUUID().toString(),
                auctionId,
                winnerId,
                finalBid,
                LocalDateTime.now()
        );
    }
}
```

### 5.5 필수 의존성 (build.gradle)

```groovy
// Kafka 필수
implementation 'org.springframework.kafka:spring-kafka'

// 테스트 (EmbeddedKafka)
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

### 5.6 필수 어노테이션 정리

| 어노테이션 / 클래스 | 위치 | 역할 | 필수 여부 |
|----------------------|------|------|-----------|
| `@EnableKafka` | Config 클래스 또는 Application | Kafka Listener 활성화 | **필수** (Spring Boot는 자동 설정되지만 명시 권장) |
| `@KafkaListener` | Consumer 메서드 | 특정 토픽의 메시지를 수신 | **필수** |
| `KafkaTemplate<K,V>` | Producer 클래스 | 메시지 발행용 템플릿 | **필수** |
| `@EmbeddedKafka` | 테스트 클래스 | 내장 Kafka 브로커로 테스트 | 통합 테스트 시 필수 |

#### @EnableKafka

```java
@Configuration
@EnableKafka  // Kafka Listener 컨테이너 팩토리 활성화
public class KafkaConfig {
    // Spring Boot auto-config이 대부분 처리하지만,
    // 커스텀 ErrorHandler 등록 시 이 클래스에서 설정
}
```

#### @KafkaListener 옵션

```java
@KafkaListener(
    topics = "auction.ended",          // 구독할 토픽 (필수)
    groupId = "auction-service",       // Consumer Group ID (필수)
    containerFactory = "kafkaListenerContainerFactory",  // 커스텀 팩토리 (선택)
    concurrency = "3"                  // 동시 Consumer 스레드 수 (선택)
)
public void handle(String message, Acknowledgment ack) { ... }
```

#### KafkaTemplate 주요 메서드

```java
// 기본 발행
kafkaTemplate.send("topic", value);

// Key 지정 발행 (파티셔닝용, 권장)
kafkaTemplate.send("topic", key, value);

// 파티션 직접 지정
kafkaTemplate.send("topic", partition, key, value);

// 비동기 결과 처리
kafkaTemplate.send("topic", key, value)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("발행 실패: {}", ex.getMessage());
        } else {
            log.info("발행 성공: offset={}", result.getRecordMetadata().offset());
        }
    });
```

### 5.7 구현 체크리스트

```
Producer 구현 시:
  ✅ build.gradle에 spring-kafka 의존성 추가
  ✅ application.yaml에 bootstrap-servers, serializer 설정
  ✅ KafkaTemplate 주입받아 send() 호출
  ✅ Key 설정 (같은 도메인 이벤트 순서 보장)
  ✅ 발행 실패 시 로깅 또는 재시도 처리

Consumer 구현 시:
  ✅ @EnableKafka 설정 확인
  ✅ application.yaml에 group-id, deserializer, auto-offset-reset 설정
  ✅ @KafkaListener(topics, groupId) 메서드 작성
  ✅ 수동 커밋 사용 시: ack-mode: manual + Acknowledgment 파라미터
  ✅ 멱등성 보장 (eventId 중복 체크)
  ✅ DLT(Dead Letter Topic) 에러 핸들러 등록
```

---

## 6. Kafka 테스트

### 6.1 단위 테스트 — Producer (Mock)

```java
@ExtendWith(MockitoExtension.class)
class AuctionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private AuctionEventPublisher publisher;

    @Test
    @DisplayName("경매 종료 이벤트를 auction.ended 토픽에 발행한다")
    void publishAuctionEnded_sendsToCorrectTopic() {
        // given
        AuctionEndedEvent event = AuctionEndedEvent.of("A-001", 42L, 720000L);

        // when
        publisher.publishAuctionEnded(event);

        // then
        verify(kafkaTemplate).send(
                eq("auction.ended"),     // topic
                eq("A-001"),             // key (auctionId)
                anyString()              // value (JSON)
        );
    }
}
```

### 6.2 단위 테스트 — Consumer (Mock)

```java
@ExtendWith(MockitoExtension.class)
class AuctionEventConsumerTest {

    @InjectMocks
    private AuctionEventConsumer consumer;

    @Test
    @DisplayName("유효한 입찰 이벤트 JSON을 정상 파싱한다")
    void handleBidPlaced_validJson_processesSuccessfully() {
        // given
        String message = """
                {
                    "eventId": "evt-001",
                    "auctionId": "A-001",
                    "bidderId": 42,
                    "amount": 720000,
                    "timestamp": "2026-06-12T13:55:00"
                }
                """;

        // when & then (예외 없이 정상 처리)
        assertDoesNotThrow(() -> consumer.handleBidPlaced(message));
    }

    @Test
    @DisplayName("잘못된 JSON이면 예외를 로깅하고 계속 동작한다")
    void handleBidPlaced_invalidJson_logsError() {
        // given
        String invalidMessage = "not a json";

        // when & then (예외를 던지지 않고 로깅만)
        assertDoesNotThrow(() -> consumer.handleBidPlaced(invalidMessage));
    }
}
```

### 6.3 통합 테스트 — EmbeddedKafka

실제 Kafka 없이 테스트 환경에서 Producer-Consumer 전체 흐름을 검증한다.

**의존성 추가 (build.gradle):**

```groovy
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

**테스트 코드:**

```java
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"auction.ended"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092"}
)
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    @DisplayName("auction.ended 토픽에 메시지를 발행하고 소비한다")
    void produceAndConsume_auctionEnded() throws Exception {
        // given
        String topic = "auction.ended";
        String message = """
                {"auctionId":"A-001","winnerId":42,"finalBid":720000}
                """;

        // when — 메시지 발행
        kafkaTemplate.send(topic, "A-001", message).get();

        // then — Consumer로 메시지 수신 확인
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        try (Consumer<String, String> consumer = cf.createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            ConsumerRecords<String, String> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isEqualTo("A-001");
            assertThat(record.value()).contains("720000");
        }
    }
}
```

### 6.4 수동 테스트 — CLI

```bash
# Kafka 컨테이너 접속 (docker-compose 사용 시)
docker exec -it biddy-kafka bash

# 토픽 목록 확인
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# 토픽 생성
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic auction.ended --partitions 3 --replication-factor 1

# 토픽 상세 확인
/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic auction.ended

# 메시지 발행 (Producer)
/opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic auction.ended
> {"auctionId":"A-001","winnerId":42,"finalBid":720000}

# 메시지 소비 (Consumer) — 다른 터미널에서
/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic auction.ended --from-beginning --group test-group

# Consumer Group 상태 확인 (Lag 모니터링)
/opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group auction-service --describe
```

### 6.5 로컬 테스트 (Docker 없이, KRaft 모드)

```bash
# Windows에서 Kafka 직접 실행 (KRaft, ZooKeeper 불필요)

# 1) 다운로드: https://kafka.apache.org/downloads
# 2) 압축 해제 후:

# 클러스터 ID 생성
bin/kafka-storage.sh random-uuid

# 스토리지 초기화
bin/kafka-storage.sh format -t {생성된UUID} -c config/kraft/server.properties

# Kafka 시작
bin/kafka-server-start.sh config/kraft/server.properties
```

---

## 7. 주요 설정 및 튜닝

### 7.1 Producer 설정

| 설정 | 값 | 설명 |
|------|-----|------|
| `acks` | `all` | 모든 복제본 확인 후 응답 (안전) |
| `retries` | `3` | 실패 시 재시도 |
| `linger.ms` | `5` | 배치 전송 대기 시간 |
| `enable.idempotence` | `true` | 중복 발행 방지 |

### 7.2 Consumer 설정

| 설정 | 값 | 설명 |
|------|-----|------|
| `auto-offset-reset` | `earliest` | 처음부터 읽기 (latest: 최신만) |
| `enable.auto.commit` | `false` | 수동 커밋 (정확한 처리 보장) |
| `max.poll.records` | `500` | 한 번에 가져올 최대 레코드 |
| `session.timeout.ms` | `30000` | Consumer 장애 감지 시간 |

### 7.3 수동 커밋 (정확한 처리 보장)

```java
@KafkaListener(topics = "auction.ended", groupId = "order-service")
public void handleAuctionEnded(
        String message,
        Acknowledgment ack    // 수동 커밋
) {
    try {
        processEvent(message);
        ack.acknowledge();    // 처리 성공 시에만 커밋
    } catch (Exception e) {
        // 커밋하지 않음 → 재처리됨
        log.error("이벤트 처리 실패, 재시도 예정", e);
    }
}
```

```yaml
# 수동 커밋 설정
spring:
  kafka:
    consumer:
      enable-auto-commit: false
    listener:
      ack-mode: manual
```

---

## 8. 장애 처리 패턴

### 8.1 Dead Letter Topic (DLT)

처리 실패한 메시지를 별도 토픽에 보관하여 후속 분석/재처리한다.

```
[auction.ended] → Consumer 처리 실패 (3회 재시도)
                         │
                         ▼
              [auction.ended.DLT]  ← 실패 메시지 보관
                         │
                         ▼
                    운영팀 모니터링 / 수동 재처리
```

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
    DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(template);
    return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    // 1초 간격, 최대 3회 재시도 후 DLT로 전송
}
```

### 8.2 멱등성 (Idempotency)

같은 이벤트가 중복 수신되어도 결과가 동일해야 한다.

```java
@KafkaListener(topics = "auction.ended")
public void handleAuctionEnded(String message) {
    AuctionEndedEvent event = parse(message);

    // eventId로 중복 체크
    if (eventRepository.existsByEventId(event.eventId())) {
        log.info("중복 이벤트 무시: {}", event.eventId());
        return;
    }

    processEvent(event);
    eventRepository.save(event.eventId());  // 처리 완료 기록
}
```

### 8.3 순서 보장

```
Key = auctionId 사용 시:

같은 경매의 이벤트 순서 보장:
  bid.placed(A-001, 700000) → bid.placed(A-001, 720000) → auction.ended(A-001)
  ↑ 모두 같은 Partition에 순서대로 저장

다른 경매는 병렬 처리:
  Partition 0: A-001 이벤트들
  Partition 1: A-002 이벤트들
  Partition 2: A-003 이벤트들
```

---

## 9. 모니터링

### 9.1 Consumer Lag 확인

```bash
/opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group auction-service --describe

GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
auction-service auction.ended   0          150             152             2
auction-service auction.ended   1          148             148             0
auction-service auction.ended   2          149             155             6
```

| LAG 상태 | 의미 | 대응 |
|----------|------|------|
| 0 | 실시간 처리 중 | 정상 |
| 1~100 | 일시적 지연 | 모니터링 |
| 100+ 증가 추세 | Consumer 처리 속도 부족 | 인스턴스 추가 |

### 9.2 Spring Actuator 연동

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,kafka
  health:
    kafka:
      enabled: true
```

---

## 10. 관련 문서

- [01_Saga_패턴.md](01_Saga_패턴.md) — Kafka 기반 Choreography Saga
- [04_CQRS_패턴.md](04_CQRS_패턴.md) — 이벤트 기반 읽기 모델 동기화
- [07_Redis_활용_패턴.md](07_Redis_활용_패턴.md) — Kafka + Redis 캐시 무효화 조합
