# CQRS (Command Query Responsibility Segregation) 패턴

## 1. 개념

> **명령(Command)과 조회(Query)의 책임을 분리한다.**

하나의 서비스에서 쓰기(생성/수정/삭제)와 읽기(조회)를 별도의 모델과 경로로 처리하는 패턴.

```
기존 (단일 서비스):
  ProductService.create()
  ProductService.update()
  ProductService.findAll()    ← 읽기/쓰기가 하나에 섞임
  ProductService.findById()

CQRS 적용 후:
  ProductCommandService.create()   ← Command (쓰기)
  ProductCommandService.update()
  ProductQueryService.findAll()    ← Query (읽기)
  ProductQueryService.findById()
```

---

## 2. 실제 적용 사례 (이미지 기준 — Product 서비스)

```
com.example.demo
├── controller
│   └── ProductController          ← 단일 진입점 (Command/Query 위임)
├── entity
│   └── Product
└── service
    ├── ProductCommandUseCase      ← ★ Command 인터페이스
    ├── ProductCommandService      ← Command 구현체 (생성, 수정, 삭제)
    ├── ProductQueryUseCase        ← ★ Query 인터페이스
    └── ProductQueryService        ← Query 구현체 (조회)
```

### 2.1 Command 측 (쓰기)

```java
public interface ProductCommandUseCase {
    ProductResult create(CreateProductCommand command);
    ProductResult update(Long id, UpdateProductCommand command);
    void delete(Long id);
}
```

- 상태를 **변경**하는 모든 행위
- 트랜잭션 필요 (`@Transactional`)
- 비즈니스 검증 로직 포함

### 2.2 Query 측 (읽기)

```java
public interface ProductQueryUseCase {
    Page<ProductResult> findAll(Pageable pageable);
    ProductResult findById(Long id);
}
```

- 상태를 **변경하지 않는** 순수 조회
- 읽기 전용 트랜잭션 (`@Transactional(readOnly = true)`)
- 성능 최적화 가능 (캐시, 읽기 전용 DB 복제본 등)

---

## 3. 왜 분리하는가?

### 3.1 단일 서비스의 문제

```java
// 하나의 서비스에 모든 책임
@Service
public class ProductService {
    public Product create(...)  { /* 복잡한 검증 + 이벤트 발행 */ }
    public Product update(...)  { /* 동시성 제어 + 상태 변경 */ }
    public List<Product> findAll(...)  { /* 단순 조회 */ }
    public Product findById(...)       { /* 단순 조회 */ }
}
```

| 문제 | 설명 |
|------|------|
| SRP 위반 | 쓰기/읽기 로직이 한 클래스에 혼재 |
| 비대한 서비스 | 메서드가 늘어날수록 클래스가 거대해짐 |
| 독립적 최적화 불가 | 조회 성능을 위해 쓰기 로직까지 영향받음 |
| 테스트 복잡도 | 전체 의존성을 Mock해야 함 |

### 3.2 CQRS로 해결

| 이점 | 설명 |
|------|------|
| **SRP 준수** | Command와 Query가 각각 단일 책임 |
| **독립 스케일링** | 조회 트래픽이 많으면 Query 쪽만 확장 |
| **독립 최적화** | Query에 캐시, 읽기 복제본, Denormalized View 적용 가능 |
| **모델 분리 가능** | 쓰기 모델과 읽기 모델을 다르게 설계 가능 |

---

## 4. CQRS의 수준

### Level 1: 서비스 분리 (현재 이미지 수준)

```
Controller → CommandService (쓰기)
           → QueryService   (읽기)
```

- 같은 DB, 같은 Entity 사용
- 가장 단순하고 실용적
- 대부분의 프로젝트에서 이 수준이면 충분

### Level 2: 모델 분리

```
Controller → CommandService → Write DB (정규화된 모델)
           → QueryService  → Read DB  (비정규화된 뷰)
```

- 쓰기용 Entity와 읽기용 DTO/View를 별도 설계
- Event Sourcing과 결합 시 강력

### Level 3: 별도 저장소 (Event Sourcing + CQRS)

```
Command → Event Store → Event 발행 → Read Model 갱신
Query  → Read Store (Elasticsearch, Redis 등)
```

- 복잡도 높음, MSA에서 대규모 트래픽 처리 시 사용

---

## 5. SOLID 관점

| 원칙 | 적용 |
|------|------|
| **SRP** | Command/Query 각각 단일 책임 |
| **OCP** | 새 Command/Query 추가 시 기존 코드 수정 없음 |
| **ISP** | CommandUseCase와 QueryUseCase 인터페이스 분리 — 클라이언트는 필요한 것만 의존 |
| **DIP** | Controller는 UseCase 인터페이스에 의존, 구현체는 주입 |

---

## 6. Biddy 프로젝트에서의 적용

현재 auction 서비스의 UseCase 분리도 CQRS의 Level 1에 해당:

```
GetAuctionFeedUseCase   ← Query (조회)
PlaceBidUseCase         ← Command (입찰, 향후 구현)
CreateAuctionUseCase    ← Command (등록, 향후 구현)
```

UseCase 단위로 이미 Command/Query가 자연스럽게 분리되어 있다.
