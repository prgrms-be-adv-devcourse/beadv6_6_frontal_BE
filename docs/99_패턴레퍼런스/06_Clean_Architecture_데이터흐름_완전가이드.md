# Clean Architecture 데이터 흐름 완전 가이드

## 1. 머릿속에 그릴 그림 — 동심원이 아닌 "파이프라인"

교과서에서는 Clean Architecture를 동심원으로 그린다. 하지만 실제로 **데이터가 흐르는 방향**을 이해하려면 **파이프라인(수도관)**으로 생각하는 것이 훨씬 직관적이다.

```
[외부 세계]                                              [외부 세계]
  HTTP        interfaces      application       domain       infrastructure       DB
  요청 ──────> Controller ────> UseCase ────> Repository ────> JPA Adapter ────> PostgreSQL
              (수도꼭지)       (정수기)       (수도관 규격)     (실제 배관)         (수원지)

  HTTP        interfaces      application
  응답 <────── Response DTO <── Result DTO <──────────────────────────────────── Entity
              (컵에 따르기)    (깨끗한 물)                                       (원수)
```

**핵심:** 데이터는 바깥→안→바깥으로 **왕복**하지만, **의존성(import)은 한 방향**으로만 향한다.

---

## 2. 실제 코드로 따라가는 데이터 흐름

`GET /api/v1/auctions?status=LIVE&sort=ending&page=0&size=20` 요청이 들어왔을 때.

### Step 1: HTTP 요청 → Controller (interfaces 레이어)

```
브라우저 → Spring DispatcherServlet → AuctionController.getAuctionFeed()
```

```java
// interfaces/api/AuctionController.java
@GetMapping
public Page<AuctionFeedResponse> getAuctionFeed(
        @RequestParam(required = false) AuctionStatus status,  // "LIVE" → enum 변환
        @RequestParam(required = false) String category,       // null
        @RequestParam(required = false) String sort,           // "ending"
        @RequestParam(defaultValue = "0") int page,            // 0
        @RequestParam(defaultValue = "20") int size            // 20
) {
    // ① 외부 파라미터 → 내부 DTO로 변환 (번역)
    AuctionFeedQuery query = new AuctionFeedQuery(status, category, sort, page, size);

    // ② UseCase 호출 (인터페이스에 의존, 구현체를 모름)
    return getAuctionFeedUseCase.execute(query)
            .map(AuctionFeedResponse::from);  // ⑥ 내부 Result → 외부 Response 변환
}
```

**이 레이어의 역할:**
- HTTP 파라미터를 Java 객체로 변환 (Spring이 해줌)
- **외부 언어(HTTP)** → **내부 언어(Query DTO)** 번역
- UseCase 실행 후 **내부 언어(Result DTO)** → **외부 언어(Response DTO)** 역번역

### Step 2: Query DTO → UseCase → Service (application 레이어)

```java
// application/service/GetAuctionFeedService.java
@Override
public Page<AuctionFeedResult> execute(AuctionFeedQuery query) {
    // ③ 비즈니스 조건 해석 (sort 문자열 → 정렬 전략)
    Sort sort = resolveSort(query.sort());  // "ending" → endsAt ASC
    Pageable pageable = PageRequest.of(query.page(), query.size(), sort);

    // ④ Repository 호출 (인터페이스에 의존, JPA를 모름)
    return auctionRepository.findByFilters(query.status(), query.category(), pageable)
            .map(AuctionFeedResult::from);  // ⑤ Entity → Result DTO 변환
}
```

**이 레이어의 역할:**
- 비즈니스 규칙 조합 (정렬 전략, 필터 조합)
- Repository **인터페이스**를 호출 (구현체가 JPA인지, MongoDB인지 모름)
- Entity를 Result DTO로 변환 (Entity가 외부로 노출되지 않게 보호)

### Step 3: Repository 인터페이스 → 구현체 (domain ↔ infrastructure)

```java
// domain/repository/AuctionRepository.java (인터페이스 — domain 레이어)
public interface AuctionRepository {
    Page<Auction> findByFilters(AuctionStatus status, String category, Pageable pageable);
}

// infrastructure/persistence/AuctionRepositoryImpl.java (구현체 — infrastructure 레이어)
@Repository
public class AuctionRepositoryImpl implements AuctionRepository {
    private final AuctionJpaRepository jpaRepository;

    @Override
    public Page<Auction> findByFilters(AuctionStatus status, String category, Pageable pageable) {
        return jpaRepository.findByFilters(status, category, pageable);
    }
}
```

**이 경계의 역할:**
- domain은 "이런 데이터가 필요해"라고 **규격(인터페이스)**만 정의
- infrastructure는 "JPA로 이렇게 가져올게"라고 **구현**
- **DIP(의존성 역전)** — domain이 infrastructure를 import하지 않음

### Step 4: JPA → SQL → PostgreSQL → Entity (infrastructure 레이어)

```java
// infrastructure/persistence/AuctionJpaRepository.java
@Query("""
    SELECT a FROM Auction a
    WHERE (:status IS NULL OR a.status = :status)
      AND (:category IS NULL OR a.category = :category)
    """)
Page<Auction> findByFilters(@Param("status") AuctionStatus status,
                            @Param("category") String category,
                            Pageable pageable);
```

```sql
-- 실제 실행되는 SQL
SELECT * FROM auction
WHERE status = 'LIVE'
ORDER BY ends_at ASC
LIMIT 20 OFFSET 0;
```

**PostgreSQL → JDBC ResultSet → Hibernate → Auction Entity**

### Step 5: Entity → Result DTO → Response DTO (역방향 변환)

```
DB Row → Auction Entity → AuctionFeedResult → AuctionFeedResponse → JSON
  ⑤ infrastructure     ⑤ application        ⑥ interfaces          Spring
```

```java
// Entity → Result (application 레이어에서 변환)
AuctionFeedResult.from(auction)  →  new AuctionFeedResult("A-FNF97", "나이키 덩크", ...)

// Result → Response (interfaces 레이어에서 변환)
AuctionFeedResponse.from(result) →  new AuctionFeedResponse("A-FNF97", "나이키 덩크", ...)
```

**왜 변환을 두 번 하는가?**

```
Entity (Auction)           — DB 컬럼과 1:1, JPA 어노테이션 포함, 내부 전용
Result (AuctionFeedResult) — 비즈니스 의미 중심, 레이어 간 전달용
Response (AuctionFeedResponse) — API 스펙 중심, 클라이언트에게 노출
```

| 구분 | Entity가 직접 노출되면? | DTO 분리하면? |
|------|----------------------|-------------|
| DB 컬럼 변경 | API 응답이 깨짐 | Entity만 수정, Response는 그대로 |
| API 스펙 변경 | Entity까지 영향 | Response만 수정 |
| 민감 필드 노출 | sellerId 같은 내부 ID 노출 위험 | 필요한 필드만 선택적 노출 |

---

## 3. 전체 데이터 흐름 한눈에 보기

```
 요청 방향 →                                                           ← 응답 방향

 ┌─────────────┐    ┌──────────────┐    ┌──────────────┐    ┌─────────────────┐
 │  interfaces  │    │  application  │    │    domain     │    │ infrastructure  │
 │              │    │               │    │              │    │                 │
 │  Controller  │──>│  UseCase(I/F) │    │  Entity      │    │  JPA Adapter    │
 │              │    │       │       │    │              │    │       │         │
 │  Request DTO │    │  Service(구현) │──>│  Repository  │<──│  RepositoryImpl │
 │              │    │       │       │    │   (I/F)      │    │       │         │
 │  Response DTO│<──│  Result DTO   │<──│              │<──│  JpaRepository  │
 │              │    │               │    │              │    │       │         │
 └──────┬───────┘    └──────────────┘    └──────────────┘    └───────┼─────────┘
        │                                                           │
        ▼                                                           ▼
   HTTP JSON                                                   PostgreSQL
   (클라이언트)                                                   (데이터)

 ─────────────── import(의존성) 방향 ──────────────>
   interfaces → application → domain ← infrastructure
```

**화살표 정리:**
- `──>` 데이터 흐름 (요청): 바깥 → 안
- `<──` 데이터 흐름 (응답): 안 → 바깥
- `→` 의존성(import): 항상 domain을 향해

---

## 4. 의존성과 결합도 외에 고려할 것들

### 4.1 테스트 용이성 (Testability)

각 레이어가 분리되어 있으면 **독립 테스트**가 가능하다.

```
┌────────────────────────────────────────────────────────────┐
│ 테스트 종류별 범위                                           │
│                                                            │
│  단위 테스트 (Mock)        슬라이스 테스트         통합 테스트  │
│  ┌──────────────┐        ┌──────────────┐     ┌──────────┐ │
│  │ Service만    │        │ Controller만 │     │ 전체     │ │
│  │ Mock Repo    │        │ Mock UseCase │     │ 실제 DB  │ │
│  │ DB 불필요    │        │ MockMvc      │     │          │ │
│  │ 0.06초       │        │ 0.25초       │     │ 3초+     │ │
│  └──────────────┘        └──────────────┘     └──────────┘ │
└────────────────────────────────────────────────────────────┘
```

| 관심사 | 왜 중요한가 | Clean Architecture의 해결 |
|--------|-----------|-------------------------|
| Service 로직 테스트 | 비즈니스 규칙 검증 | Repository를 Mock → DB 없이 0.06초 |
| Controller 테스트 | API 스펙 검증 | UseCase를 Mock → @WebMvcTest로 0.25초 |
| Repository 교체 | DB 변경 가능성 | 인터페이스만 의존 → 구현체 교체해도 Service 무영향 |

### 4.2 변경의 전파 범위 (Change Propagation)

**"하나를 바꿨을 때 다른 곳이 얼마나 깨지는가?"**

```
 변경 지점        영향 범위                   이유
 ──────────────────────────────────────────────────────
 DB 컬럼 추가  →  Entity + JPA만              domain 내부
 API 필드 추가 →  Response DTO만              interfaces 내부
 정렬 로직 변경 →  Service만                  application 내부
 JPA → MyBatis →  Impl만 교체                infrastructure 내부
 비즈니스 규칙  →  Entity 또는 Service         domain/application
```

**나쁜 예: Entity를 직접 API 응답으로 쓰면?**
```
 DB 컬럼 추가 → Entity 변경 → API 응답 변경 → 프론트엔드 깨짐
                            ↑ 연쇄 반응!
```

**좋은 예: DTO 분리하면?**
```
 DB 컬럼 추가 → Entity 변경 → (끝. Response DTO는 별도)
```

### 4.3 DTO 변환 비용 vs 보호 가치

```
"변환이 너무 많은 거 아닌가요?"

  HTTP Param → Query DTO → Entity → Result DTO → Response DTO → JSON

  총 5번의 변환. 과연 가치가 있는가?
```

| 변환 | 비용 | 보호하는 것 |
|------|------|-----------|
| HTTP → Query | record 생성 1회 | Controller가 Service 시그니처에 종속되지 않음 |
| DB → Entity | Hibernate가 알아서 | 테이블 구조와 객체 구조의 분리 |
| Entity → Result | 필드 복사 | Entity의 내부 필드(version, audit)가 외부 노출되지 않음 |
| Result → Response | 필드 복사 | 비즈니스 모델과 API 스펙의 독립적 진화 |

**결론:** 각 변환은 **레이어 경계를 보호**한다. 변환을 생략하면 경계가 무너지고, 한 곳의 변경이 전체로 전파된다.

### 4.4 트랜잭션 경계 (Transaction Boundary)

```
 Controller        Service             Repository           DB
     │                │                    │                 │
     │  호출           │                    │                 │
     ├───────────────>│                    │                 │
     │                │ @Transactional 시작  │                 │
     │                │════════════════════│═════════════════│
     │                │  findByFilters()   │                 │
     │                │───────────────────>│  SELECT ...     │
     │                │                    │────────────────>│
     │                │                    │<────────────────│
     │                │<───────────────────│                 │
     │                │ @Transactional 종료  │                 │
     │                │════════════════════│═════════════════│
     │<───────────────│                    │                 │
     │  응답           │                    │                 │
```

- 트랜잭션은 **application 레이어(Service)**에서 관리
- Controller에서 트랜잭션을 열면 → HTTP 응답까지 DB 커넥션을 물고 있음 (위험)
- Repository에서 트랜잭션을 열면 → 여러 Repository 호출을 하나의 트랜잭션으로 묶을 수 없음

### 4.5 예외 처리 전략 (Exception Flow)

예외도 데이터와 마찬가지로 **레이어를 따라 위로 전파**된다.

```
 DB 에러          infrastructure        application         interfaces         HTTP
 SQLException →  DataAccessException →  BusinessException →  @ExceptionHandler → 400/409/500
                 (Spring이 변환)        (도메인 의미 부여)    (HTTP 상태 매핑)
```

| 레이어 | 예외 종류 | 예시 |
|--------|---------|------|
| domain | 비즈니스 규칙 위반 | `AuctionEndedException`, `BidTooLowException` |
| application | 유스케이스 실패 | `AuctionNotFoundException` |
| infrastructure | 기술적 실패 | `DataAccessException` (Spring이 변환) |
| interfaces | HTTP 응답 매핑 | `@ExceptionHandler` → 409, 400, 500 |

**원칙:** domain 예외는 **비즈니스 언어**로 표현하고, HTTP 상태코드 같은 **기술적 세부사항을 모른다.**

### 4.6 직렬화 독립성 (Serialization Independence)

```
 같은 데이터, 다른 직렬화:

  REST API    → JSON  { "auctionId": "A-FNF97", "currentBid": 720000 }
  Kafka Event → JSON  { "eventType": "BID_PLACED", "payload": { ... } }
  WebSocket   → JSON  { "type": "BID", "currentBid": 740000 }
  gRPC        → Protobuf (바이너리)
```

Entity가 `@JsonProperty` 같은 직렬화 어노테이션을 가지면, 출력 채널이 추가될 때마다 Entity가 수정된다. DTO를 분리하면 **각 채널별 독립적인 직렬화**가 가능하다.

---

## 5. 안티패턴 — 이렇게 하면 안 된다

### 5.1 Controller에서 Repository 직접 호출

```java
// BAD: Controller → Repository (application 레이어 건너뜀)
@GetMapping
public List<Auction> getAuctions() {
    return auctionRepository.findAll();  // Entity 직접 노출, 비즈니스 규칙 무시
}
```

**문제:** 비즈니스 로직 삽입 불가, Entity 외부 노출, 트랜잭션 관리 불명확

### 5.2 Service에서 HttpServletRequest 참조

```java
// BAD: Service가 HTTP에 의존
public void placeBid(HttpServletRequest request) {
    String token = request.getHeader("Authorization");  // 인프라 세부사항 침투
}
```

**문제:** Service가 웹 프레임워크에 종속, CLI나 Kafka에서 호출 불가

### 5.3 Entity를 API 응답으로 직접 반환

```java
// BAD: Entity가 곧 API 응답
@GetMapping
public Auction getAuction(@PathVariable String id) {
    return auctionRepository.findById(id);  // 내부 필드 전부 노출
}
```

**문제:** DB 컬럼 = API 필드가 되어, 둘 중 하나를 바꾸면 다른 쪽이 깨짐

---

## 6. 최종 요약 — Clean Architecture의 본질

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  "바깥 레이어는 쉽게 교체할 수 있어야 하고,              │
│   안쪽 레이어는 바깥이 바뀌어도 영향받지 않아야 한다."    │
│                                                         │
│   DB를 바꿔도       → domain/application은 그대로        │
│   프레임워크를 바꿔도 → domain/application은 그대로       │
│   API 스펙이 바뀌어도 → domain은 그대로                   │
│   출력 채널이 늘어도  → domain은 그대로                   │
│                                                         │
│   ✕ 레이어를 나누는 것이 목적이 아니라,                   │
│   ○ 변경의 전파를 차단하는 것이 목적이다.                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 고려사항 체크리스트

| # | 고려사항 | 질문 | 해결 수단 |
|---|---------|------|----------|
| 1 | 의존성 방향 | domain이 외부 프레임워크를 import하는가? | DIP, Port/Adapter |
| 2 | 결합도 | 한 곳을 바꾸면 몇 곳이 깨지는가? | DTO 변환, 인터페이스 |
| 3 | 테스트 용이성 | DB 없이 비즈니스 로직을 테스트할 수 있는가? | Mock Repository |
| 4 | 변경 전파 | DB 컬럼 추가가 API 응답에 영향을 주는가? | Entity/DTO 분리 |
| 5 | 트랜잭션 경계 | 트랜잭션이 어느 레이어에서 열리는가? | Application Service |
| 6 | 예외 흐름 | 비즈니스 예외가 HTTP 상태코드를 아는가? | 레이어별 예외 변환 |
| 7 | 직렬화 독립 | Entity에 @JsonProperty가 붙어있는가? | 채널별 Response DTO |
| 8 | 재사용성 | Service를 REST 외에 Kafka/CLI에서도 쓸 수 있는가? | HTTP 독립적 UseCase |
