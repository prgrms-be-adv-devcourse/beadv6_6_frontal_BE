# 10. WebSocket 실시간 통신 가이드

## 1. WebSocket이란?

### HTTP vs WebSocket

```
HTTP (요청-응답):
  클라이언트 ──요청──▶ 서버
  클라이언트 ◀──응답── 서버
  (연결 종료)

  클라이언트 ──요청──▶ 서버    ← 새 데이터가 필요할 때마다 다시 요청
  클라이언트 ◀──응답── 서버
  (연결 종료)

WebSocket (양방향 상시 연결):
  클라이언트 ══핸드셰이크══▶ 서버
  클라이언트 ◀═══════════▶ 서버    ← 연결 유지, 양방향 실시간 통신
  클라이언트 ◀──서버 push── 서버    ← 서버가 먼저 데이터를 보낼 수 있음
```

### 왜 경매에 WebSocket이 필요한가?

| 방식 | 동작 | 문제점 |
|------|------|--------|
| Polling | 클라이언트가 1초마다 GET 요청 | 서버 부하, 지연 발생 |
| Long Polling | 서버가 변경 시까지 응답 보류 | 커넥션 낭비 |
| **WebSocket** | 서버가 변경 시 즉시 push | **실시간, 효율적** |

경매는 **입찰이 발생하는 즉시** 모든 참여자에게 현재가를 알려야 하므로 WebSocket이 최적.

---

## 2. Biddy 경매 WebSocket 설계

### API 스펙

```
Endpoint:  /ws/auctions/{auctionId}
Protocol:  WebSocket (STOMP over WebSocket)
구독 시:    연결 유지, 서버가 이벤트 발생 시 push
```

### 서버 Push 메시지 타입

#### (1) 입찰 발생 시 — BID

```json
{
  "type": "BID",
  "currentBid": 720000,
  "bidCount": 7,
  "bidder": {
    "collectorId": 42,
    "nickname": "collector01"
  }
}
```

#### (2) 경매 종료 시 — ENDED

```json
{
  "type": "ENDED",
  "winnerId": 42,
  "finalBid": 720000
}
```

---

## 3. 전체 아키텍처 흐름

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Client  │────▶│   Gateway    │────▶│   Auction    │
│ (Browser)│◀────│  (WebSocket) │◀────│   Service    │
└──────────┘     └──────────────┘     └──────────────┘
     │                                       │
     │              WebSocket 연결            │
     │◀══════════════════════════════════════╝
     │                                       │
     │    1. /ws/auctions/A-001 구독          │
     │────────────────────────────────────────▶
     │                                       │
     │    2. 누군가 입찰                       │
     │                                 ┌─────┴─────┐
     │                                 │  입찰 처리  │
     │                                 │  DB 저장   │
     │                                 └─────┬─────┘
     │                                       │
     │    3. BID 메시지 push                  │
     │◀───────────────────────────────────────│
     │  {"type":"BID","currentBid":720000}    │
     │                                       │
     │    4. 경매 종료 시각 도달               │
     │                                 ┌─────┴─────┐
     │                                 │  종료 처리  │
     │                                 │  낙찰 확정  │
     │                                 └─────┬─────┘
     │                                       │
     │    5. ENDED 메시지 push                │
     │◀───────────────────────────────────────│
     │  {"type":"ENDED","winnerId":42}        │
```

---

## 4. STOMP 프로토콜

WebSocket 위에서 동작하는 **메시징 프로토콜**. Spring이 공식 지원.

```
STOMP = Simple Text Oriented Messaging Protocol

역할:
  CONNECT     → WebSocket 연결 수립
  SUBSCRIBE   → 특정 토픽 구독
  SEND        → 메시지 전송
  MESSAGE     → 서버 → 클라이언트 push
  DISCONNECT  → 연결 해제
```

### 왜 순수 WebSocket 대신 STOMP를 쓰는가?

| 순수 WebSocket | STOMP |
|---|---|
| 메시지 라우팅 직접 구현 | `/topic/`, `/queue/` 자동 라우팅 |
| 구독/해제 직접 관리 | SUBSCRIBE/UNSUBSCRIBE 내장 |
| 브로드캐스트 직접 구현 | `SimpMessagingTemplate`으로 간단 |
| 인증/인가 직접 구현 | Spring Security 연동 가능 |

---

## 5. Spring Boot 구현

### 5-1. 의존성 추가

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-websocket'
```

### 5-2. WebSocket 설정 (`common/config/`)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 엔드포인트 등록.
     * 클라이언트는 /ws 로 WebSocket 연결을 수립한다.
     * SockJS 폴백을 지원하여 WebSocket 미지원 브라우저에서도 동작한다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 메시지 브로커 설정.
     * - /topic : 1:N 브로드캐스트 (경매 실시간 구독)
     * - /queue : 1:1 개인 메시지 (낙찰 알림 등)
     * - /app   : 클라이언트 → 서버 메시지 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

### 5-3. 이벤트 DTO

```java
/**
 * WebSocket으로 클라이언트에 push되는 경매 이벤트.
 * type 필드로 BID / ENDED를 구분한다.
 */
public sealed interface AuctionEvent {

    /** 새로운 입찰 발생 이벤트 */
    record BidEvent(
        String type,        // "BID"
        Long currentBid,
        Integer bidCount,
        BidderInfo bidder
    ) implements AuctionEvent {
        public record BidderInfo(Long collectorId, String nickname) {}
    }

    /** 경매 종료 이벤트 */
    record EndedEvent(
        String type,        // "ENDED"
        Long winnerId,
        Long finalBid
    ) implements AuctionEvent {}
}
```

### 5-4. 메시지 발행 서비스

```java
/**
 * WebSocket 메시지 발행기.
 * SimpMessagingTemplate을 사용하여 구독 중인 클라이언트에게 push한다.
 *
 * 토픽 형식: /topic/auctions/{auctionId}
 */
@Service
@RequiredArgsConstructor
public class AuctionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /** 입찰 이벤트를 해당 경매 구독자에게 브로드캐스트 */
    public void sendBidUpdate(String auctionId, BidEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/auctions/" + auctionId, event);
    }

    /** 경매 종료 이벤트를 해당 경매 구독자에게 브로드캐스트 */
    public void sendAuctionEnded(String auctionId, EndedEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/auctions/" + auctionId, event);
    }
}
```

### 5-5. 입찰 서비스에서 WebSocket 연동

```java
@Service
@RequiredArgsConstructor
public class BidPlaceService {

    private final AuctionWebSocketPublisher webSocketPublisher;

    @Transactional
    public void placeBid(PlaceBidCommand command) {
        // 1. 입찰 유효성 검증
        // 2. 입찰 저장
        // 3. 현재가 갱신

        // 4. WebSocket으로 실시간 push
        webSocketPublisher.sendBidUpdate(command.auctionId(),
            new BidEvent("BID", newCurrentBid, newBidCount,
                new BidderInfo(command.bidderId(), nickname)));
    }
}
```

---

## 6. 클라이언트 (JavaScript) 연동

```javascript
// SockJS + STOMP.js 사용
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {

    // 특정 경매 구독
    stompClient.subscribe('/topic/auctions/A-001', (message) => {
        const event = JSON.parse(message.body);

        if (event.type === 'BID') {
            // 현재가, 입찰 횟수 업데이트
            updateCurrentBid(event.currentBid);
            updateBidCount(event.bidCount);
        }

        if (event.type === 'ENDED') {
            // 경매 종료 처리
            showWinner(event.winnerId, event.finalBid);
        }
    });
});

// 연결 해제
function disconnect() {
    stompClient.disconnect();
}
```

---

## 7. MSA에서 WebSocket 확장 (Scale-out)

단일 서버에서는 `SimpleBroker`로 충분하지만, **서버가 여러 대**일 때는 문제가 발생한다.

```
문제: 서버 A에 연결된 클라이언트에게만 push됨

  Client 1 ──▶ Server A  ← 입찰 발생, push 받음 ✅
  Client 2 ──▶ Server B  ← push 못 받음 ❌
```

### 해결: 외부 메시지 브로커 사용

```
┌──────────┐     ┌──────────────┐
│ Server A │────▶│              │
│          │◀────│  Redis Pub/  │
└──────────┘     │  Sub 또는    │
                 │  RabbitMQ    │
┌──────────┐     │              │
│ Server B │────▶│              │
│          │◀────│              │
└──────────┘     └──────────────┘

→ 모든 서버가 브로커를 통해 메시지를 공유
→ 어느 서버에 연결된 클라이언트든 push 수신 가능
```

### Spring 설정 (RabbitMQ 외부 브로커)

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    // SimpleBroker 대신 외부 브로커 사용
    registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("localhost")
            .setRelayPort(61613);   // RabbitMQ STOMP port
    registry.setApplicationDestinationPrefixes("/app");
}
```

---

## 8. Biddy 프로젝트 적용 패키지 구조

```
com.biddy.auction
├── common/
│   └── config/
│       └── WebSocketConfig.java        ← STOMP 설정
│
├── auction/
│   ├── application/
│   │   └── service/
│   │       └── AuctionWebSocketPublisher.java  ← 메시지 발행
│   └── presentation/
│       └── dto/
│           └── AuctionEvent.java       ← BidEvent, EndedEvent
│
└── bid/
    └── application/
        └── service/
            └── BidPlaceService.java    ← 입찰 시 WebSocket push 호출
```

---

## 9. WebSocket vs SSE vs Polling 비교

| 항목 | WebSocket | SSE | Polling |
|------|-----------|-----|---------|
| 방향 | 양방향 | 서버→클라이언트 단방향 | 클라이언트→서버 |
| 프로토콜 | ws:// | HTTP | HTTP |
| 실시간성 | 즉시 | 즉시 | 주기에 따라 지연 |
| 서버 부하 | 낮음 (연결 유지) | 낮음 | 높음 (반복 요청) |
| 브라우저 지원 | 대부분 | 대부분 (IE 미지원) | 전체 |
| 적합한 경우 | **채팅, 경매, 게임** | 알림, 피드 | 간단한 상태 확인 |

**Biddy 경매** → 입찰(클라이언트→서버)도 필요하므로 **양방향 WebSocket**이 최적.

---

## 10. 테스트 방법

### 단위 테스트 (Publisher)

```java
@ExtendWith(MockitoExtension.class)
class AuctionWebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AuctionWebSocketPublisher publisher;

    @Test
    @DisplayName("입찰 이벤트를 올바른 토픽으로 전송한다")
    void sendBidUpdate_sendsToCorrectTopic() {
        BidEvent event = new BidEvent("BID", 720000L, 7,
            new BidderInfo(42L, "collector01"));

        publisher.sendBidUpdate("A-001", event);

        verify(messagingTemplate).convertAndSend(
            "/topic/auctions/A-001", event);
    }
}
```

### 통합 테스트 (WebSocket 연결)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("WebSocket으로 경매 이벤트를 수신한다")
    void receivesBidEventViaWebSocket() throws Exception {
        WebSocketStompClient stompClient =
            new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient.connectAsync(
            "ws://localhost:" + port + "/ws",
            new StompSessionHandlerAdapter() {})
            .get(3, TimeUnit.SECONDS);

        CompletableFuture<BidEvent> future = new CompletableFuture<>();
        session.subscribe("/topic/auctions/A-001",
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return BidEvent.class;
                }
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    future.complete((BidEvent) payload);
                }
            });

        // 입찰 트리거 → WebSocket push 확인
        BidEvent received = future.get(5, TimeUnit.SECONDS);
        assertThat(received.type()).isEqualTo("BID");
    }
}
```

### .http 파일로 WebSocket 테스트 (IntelliJ)

```
### WebSocket 연결 테스트 (IntelliJ HTTP Client는 WebSocket 미지원)
### 대안: wscat 또는 Postman의 WebSocket 기능 사용

# wscat 설치
# npm install -g wscat

# 연결
# wscat -c ws://localhost:8080/ws

# 또는 Postman → New → WebSocket Request
# URL: ws://localhost:8080/ws
```
