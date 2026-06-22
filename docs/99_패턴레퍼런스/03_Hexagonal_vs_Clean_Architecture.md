# Hexagonal Architecture vs Clean Architecture

## 1. 핵심 차이 요약

| 구분 | Hexagonal (Ports & Adapters) | Clean Architecture |
|------|-----------------------------|--------------------|
| 제안자 | Alistair Cockburn (2005) | Robert C. Martin (2012) |
| 핵심 개념 | Port(인터페이스)와 Adapter(구현체) | UseCase 중심의 동심원 레이어 |
| 구조 형태 | 육각형 — 안과 밖의 2분법 | 동심원 — 4개 레이어 (Entity → UseCase → Adapter → Framework) |
| 초점 | 외부 세계와의 격리 (인프라 교체 용이) | 비즈니스 규칙의 독립성 (UseCase 명시적 분리) |
| 네이밍 특징 | Port, Adapter, Driving/Driven | Entity, UseCase, Gateway, Presenter |

---

## 2. 공통점 — 왜 헷갈리는가?

두 아키텍처의 공통 원칙:

1. **의존성 방향은 안쪽으로만** — 도메인은 외부를 모른다
2. **인터페이스로 경계를 정의** — domain에 인터페이스, infrastructure에 구현체
3. **프레임워크 독립** — Spring, JPA 등은 바깥 레이어에만 존재

이 공통점 때문에 **패키지 구조가 거의 동일하게 보인다.** 차이는 구조보다 **의도와 네이밍 컨벤션**에 있다.

---

## 3. 구조 비교 — 실제 패키지로 보기

### 3.1 헥사고날 (Product 서비스 — 이미지 기준)

```
product
├── application/              ← Application Service (유스케이스 조합)
├── domain/
│   ├── model/
│   │   └── Product           ← 핵심 도메인 엔티티
│   └── repository/           ← ★ Port (인터페이스)
├── infrastructure.persistence/
│   ├── ProductJpaRepository  ← Spring Data JPA
│   └── ProductRepositoryAdapter  ← ★ Adapter (Port 구현체)
└── presentation/
    ├── dto/                  ← 요청/응답 DTO
    └── ProductController     ← ★ Driving Adapter (외부 → 내부)
```

**핵심 용어:**
- `Port` = domain 안의 인터페이스 (e.g., `ProductRepository`)
- `Adapter` = 포트를 구현하는 바깥 클래스 (e.g., `ProductRepositoryAdapter`)
- `Driving Adapter` = 외부에서 안으로 요청을 보내는 쪽 (Controller, CLI, Kafka Consumer)
- `Driven Adapter` = 안에서 밖으로 나가는 쪽 (DB, 외부 API, 메시지 발행)

### 3.2 클린 아키텍처 (Auction 서비스 — 현재 프로젝트)

```
auction
├── application/
│   ├── usecase/              ← ★ UseCase 인터페이스 (입력 Port 역할)
│   ├── service/              ← UseCase 구현체
│   ├── dto/                  ← Command/Query DTO
│   └── port/                 ← 외부 연동 Port
├── domain/
│   ├── model/                ← Entity, VO, Enum
│   ├── repository/           ← Repository 인터페이스
│   └── service/              ← Domain Service (순수 비즈니스 로직)
├── infrastructure/
│   ├── persistence/
│   │   ├── AuctionJpaRepository
│   │   └── AuctionRepositoryImpl  ← ★ Impl (구현체)
│   └── kafka/
└── interfaces/
    ├── api/                  ← ★ Controller
    └── dto/                  ← Request/Response DTO
```

**핵심 용어:**
- `UseCase` = 하나의 비즈니스 행위를 명시적으로 정의 (e.g., `GetAuctionFeedUseCase`)
- `Entity` = 도메인 핵심 객체
- `Impl` = 인터페이스 구현체 (Adapter라고 안 부름)
- `interfaces` = 외부에 노출하는 인터페이스 레이어

---

## 4. 결정적 차이점

### 4.1 UseCase의 명시성

| | Hexagonal | Clean Architecture |
|---|-----------|-------------------|
| UseCase | application 서비스에 메서드로 존재 | **별도 인터페이스로 분리** (1 UseCase = 1 인터페이스) |
| 예시 | `ProductService.getProducts()` | `GetAuctionFeedUseCase.execute()` |

Clean Architecture는 UseCase를 **일급 시민**으로 취급한다. 각 비즈니스 행위가 독립적인 인터페이스로 존재하므로:
- 의존성 주입 시 필요한 행위만 주입 가능
- 테스트 시 특정 UseCase만 Mock 가능
- SRP(단일 책임 원칙) 강제

### 4.2 레이어 네이밍

| Hexagonal | Clean Architecture |
|-----------|-------------------|
| `presentation/` | `interfaces/` |
| `~Adapter` (e.g., RepositoryAdapter) | `~Impl` (e.g., RepositoryImpl) |
| Port라는 용어 강조 | UseCase, Gateway 용어 강조 |

### 4.3 DTO 위치

| | Hexagonal | Clean Architecture |
|---|-----------|-------------------|
| 요청/응답 DTO | `presentation/dto/` | `interfaces/dto/` |
| 내부 전달 DTO | `application/` 내부 | `application/dto/` (Command, Query) |

---

## 5. 어느 것을 선택할까?

| 상황 | 추천 |
|------|------|
| 외부 시스템 연동이 많고 인프라 교체가 잦다 | **Hexagonal** — Adapter 교체가 직관적 |
| 복잡한 비즈니스 로직, UseCase가 다양하다 | **Clean Architecture** — UseCase 분리로 복잡도 관리 |
| 간단한 CRUD 중심 서비스 | 둘 다 오버엔지니어링 — 레이어드 아키텍처로 충분 |
| MSA에서 도메인별 서비스를 나눈다 | 둘 다 적합 — 팀 컨벤션에 따라 선택 |

---

## 6. SOLID 관점에서의 비교

| SOLID 원칙 | Hexagonal | Clean Architecture |
|-----------|-----------|-------------------|
| **SRP** | Adapter 단위로 책임 분리 | UseCase 단위로 책임 분리 (더 세분화) |
| **OCP** | 새 Adapter 추가로 확장 | 새 UseCase 추가로 확장 |
| **LSP** | Port 인터페이스 준수 | UseCase 인터페이스 준수 |
| **ISP** | Port를 목적별로 분리 | UseCase를 행위별로 분리 (1인터페이스 = 1메서드) |
| **DIP** | domain → Port ← Adapter | domain ← application → infrastructure |

---

## 7. 결론

> **헥사고날은 "어떻게 격리할 것인가"에 집중하고,
> 클린 아키텍처는 "비즈니스 규칙을 어떻게 표현할 것인가"에 집중한다.**

실무에서는 두 아키텍처를 혼합해서 사용하는 경우가 많다. 본 프로젝트(Biddy)에서는 **Clean Architecture 기반**으로 auction 서비스를 구성하되, Port/Adapter 개념을 infrastructure 레이어에서 차용한다.
