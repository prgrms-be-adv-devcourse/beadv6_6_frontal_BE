# ACL (Anti-Corruption Layer, 부패 방지 계층) 패턴

## 1. 개념

> **외부 시스템(또는 다른 도메인)의 모델이 내 도메인을 오염시키지 않도록 번역 계층을 둔다.**

외부 API의 응답 구조, 네이밍, 개념이 내 도메인과 다를 때, 이를 직접 사용하면 외부 변경이 내부까지 전파된다. ACL은 이 경계에서 **번역기(Translator)** 역할을 한다.

```
[외부 시스템]  →  [ACL 번역 계층]  →  [내 도메인]

외부 Seller API          ACL               Product 도메인
{                    Translator가         내 도메인 모델인
  "bizNo": "123",    번역/변환             SellerProfile로
  "ceoName": "홍"                         변환하여 사용
}
```

---

## 2. 실제 적용 사례 (이미지 기준 — Product + Seller ACL)

```
com.example.demo
├── acl/                              ← ★ ACL 패키지
│   ├── ExternalSellerClient.java     ← 외부 Seller API 호출
│   ├── ExternalSellerResponse.java   ← 외부 응답 DTO (외부 모델)
│   ├── SellerAcl.java                ← ★ ACL 퍼사드 (진입점)
│   ├── SellerAclTranslator.java      ← ★ 번역기 (외부 → 내부 변환)
│   └── SellerProfile.java            ← 내 도메인용 모델 (번역 결과)
└── service/
    └── ProductServiceImpl.java       ← ACL을 통해 Seller 정보 사용
```

### 2.1 각 클래스의 역할

#### ExternalSellerClient — 외부 API 호출

```java
@Component
public class ExternalSellerClient {

    private final RestClient restClient;

    public ExternalSellerResponse fetchSeller(String businessNumber) {
        return restClient.get()
                .uri("/api/sellers/{bizNo}", businessNumber)
                .retrieve()
                .body(ExternalSellerResponse.class);
    }
}
```

#### ExternalSellerResponse — 외부 응답 (외부의 언어)

```java
// 외부 시스템이 정의한 구조 — 내 도메인과 네이밍/구조가 다를 수 있음
public record ExternalSellerResponse(
    String bizNo,           // 내 도메인에서는 businessNumber
    String ceoName,         // 내 도메인에서는 representativeName
    String companyName,
    String bizStatus        // "ACTIVE", "CLOSED" 등
) {}
```

#### SellerAclTranslator — 번역기 (핵심)

```java
@Component
public class SellerAclTranslator {

    public SellerProfile translate(ExternalSellerResponse response) {
        return new SellerProfile(
                response.bizNo(),
                response.companyName(),
                response.ceoName(),
                translateStatus(response.bizStatus())
        );
    }

    private boolean translateStatus(String bizStatus) {
        return "ACTIVE".equals(bizStatus);
    }
}
```

#### SellerProfile — 내 도메인 모델 (번역 결과)

```java
// 내 도메인의 언어로 표현된 판매자 정보
public record SellerProfile(
    String businessNumber,       // bizNo → businessNumber
    String companyName,
    String representativeName,   // ceoName → representativeName
    boolean active               // bizStatus → boolean
) {}
```

#### SellerAcl — ACL 퍼사드 (진입점)

```java
@Component
@RequiredArgsConstructor
public class SellerAcl {

    private final ExternalSellerClient client;
    private final SellerAclTranslator translator;

    public SellerProfile getSellerProfile(String businessNumber) {
        ExternalSellerResponse response = client.fetchSeller(businessNumber);
        return translator.translate(response);
    }
}
```

---

## 3. ACL이 없으면 어떻게 되는가?

### Bad: 외부 모델을 직접 사용

```java
@Service
public class ProductService {

    public void createProduct(CreateProductCommand cmd) {
        // 외부 응답을 그대로 사용 — 외부 변경이 내부에 직접 전파
        ExternalSellerResponse seller = sellerClient.fetchSeller(cmd.bizNo());

        // 외부 필드명을 내 코드 곳곳에서 사용
        if (!"ACTIVE".equals(seller.bizStatus())) {  // ← 외부 용어 침투
            throw new RuntimeException("비활성 판매자");
        }
        product.setSellerName(seller.ceoName());     // ← 외부 용어 침투
    }
}
```

| 문제 | 설명 |
|------|------|
| **도메인 오염** | 외부의 `bizNo`, `ceoName` 같은 용어가 내 코드에 퍼짐 |
| **변경 전파** | 외부 API 응답 구조 변경 시 내 서비스 코드 전체 수정 필요 |
| **테스트 어려움** | 외부 API를 직접 호출하므로 Mock이 산재 |

### Good: ACL을 통해 사용

```java
@Service
public class ProductService {

    private final SellerAcl sellerAcl;  // ACL만 의존

    public void createProduct(CreateProductCommand cmd) {
        SellerProfile seller = sellerAcl.getSellerProfile(cmd.businessNumber());

        if (!seller.active()) {           // ← 내 도메인 용어
            throw new RuntimeException("비활성 판매자");
        }
        product.setSellerName(seller.representativeName());  // ← 내 도메인 용어
    }
}
```

---

## 4. ACL의 구성 요소

```
┌──────────────────────────────────────────────┐
│                ACL (부패 방지 계층)            │
│                                              │
│  ┌─────────────┐    ┌──────────────────┐     │
│  │   Client     │───>│   Translator     │     │
│  │ (외부 호출)   │    │ (외부→내부 변환)  │     │
│  └─────────────┘    └───────┬──────────┘     │
│                             │                │
│  ┌─────────────┐    ┌───────▼──────────┐     │
│  │ External     │    │  Domain Model    │     │
│  │ Response     │    │ (내 도메인 모델)   │     │
│  │ (외부 모델)   │    │ e.g. SellerProfile│    │
│  └─────────────┘    └──────────────────┘     │
│                                              │
│  ┌─────────────────────────────────────┐     │
│  │         ACL Facade (진입점)          │     │
│  │  내 도메인이 사용하는 유일한 접점     │     │
│  └─────────────────────────────────────┘     │
└──────────────────────────────────────────────┘
```

| 구성 요소 | 역할 |
|-----------|------|
| **Client** | 외부 시스템 호출 (REST, gRPC, Kafka 등) |
| **External Response** | 외부 시스템의 응답 구조 (외부의 언어) |
| **Translator** | 외부 모델 → 내 도메인 모델 변환 (번역기) |
| **Domain Model** | 번역된 결과 — 내 도메인 언어로 표현 |
| **ACL Facade** | 내 도메인이 사용하는 단일 진입점 |

---

## 5. MSA에서 ACL이 중요한 이유

MSA에서는 서비스 간 통신이 빈번하다. 각 서비스는 자체 도메인 언어(Ubiquitous Language)를 사용하므로, **Bounded Context 경계**에서 ACL이 필수적이다.

```
[Member Service]          [Auction Service]
  회원 = Member              판매자 = Seller
  memberId                   sellerId
  nickname                   sellerNickname

         └────── ACL ──────┘
              번역 계층
```

| 상황 | ACL 없이 | ACL 있으면 |
|------|---------|----------|
| Member API 응답 변경 | Auction 코드 전체 수정 | Translator만 수정 |
| 필드명 차이 (memberId vs sellerId) | 혼란, 일관성 없음 | Translator에서 매핑 |
| 외부 서비스 장애 | 내 도메인까지 영향 | ACL에서 Fallback 처리 가능 |

---

## 6. SOLID 관점

| 원칙 | 적용 |
|------|------|
| **SRP** | Client(호출), Translator(변환), Facade(조합) 각각 단일 책임 |
| **OCP** | 외부 API 변경 시 Translator만 수정 — 도메인 코드는 건드리지 않음 |
| **DIP** | 도메인은 ACL Facade 인터페이스에 의존, 외부 구현 세부사항은 모름 |
| **ISP** | 외부의 수십 개 필드 중 내 도메인에 필요한 것만 SellerProfile에 포함 |

---

## 7. Biddy 프로젝트에서의 적용 가능성

Auction 서비스에서 외부 서비스 연동 시 ACL 적용 대상:

| 외부 서비스 | ACL 대상 | 내 도메인 모델 |
|-----------|---------|-------------|
| Member Service | 판매자/입찰자 정보 조회 | `SellerProfile`, `BidderProfile` |
| Product Service | 상품 정보 동기화 (Kafka) | `ProductSnapshot` |
| Payment Service | 결제 결과 수신 | `PaymentResult` |

```
auction
└── infrastructure
    └── acl
        ├── member
        │   ├── MemberClient.java
        │   ├── MemberApiResponse.java
        │   ├── MemberAclTranslator.java
        │   └── SellerProfile.java
        └── payment
            ├── PaymentClient.java
            └── PaymentResult.java
```
