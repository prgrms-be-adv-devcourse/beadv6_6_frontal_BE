# K6 Performance Testing

> AWS Auction 테스트는 [`AUCTION_TEST_SOURCE_GUIDE.md`](./AUCTION_TEST_SOURCE_GUIDE.md)를 먼저 확인하고 `10_auction_preflight.js`부터 순서대로 실행한다.
> 실제 실행 현황은 [`AUCTION_TEST_EXECUTION_PROGRESS.md`](./AUCTION_TEST_EXECUTION_PROGRESS.md)에 기록한다.
>
> `00_`~`03_` 스크립트는 localhost와 과거 API 요청 규격을 사용하는 기존 자료이므로 AWS 테스트에 사용하지 않는다.

AWS Auction 쓰기 테스트의 권장 인증 방식은 `AUTH_MODE=credentials`이다. k6가 `setup()`에서 Swagger와 같은 `POST /api/members/login`을 호출하며, 실제 이메일·비밀번호는 Git이 아닌 `/tmp/auction-credentials.json`에만 저장한다. 이미 발급한 Access Token 파일을 쓰는 `token` 모드도 대안으로 지원한다.

> Phase 1: Smoke Tests and Environment Verification

## 디렉토리 구조

```
k6-tests/
├── scripts/          # K6 테스트 스크립트
│   └── 00_smoke_test.js    # 기본 API 검증
├── results/          # 테스트 결과 (JSON, HTML)
└── data/             # 테스트용 데이터 (CSV, JSON)
```

---

## Quick Start

### 1. Smoke Test 실행

```bash
# 로컬 환경 (API Gateway가 localhost:8000에서 실행 중)
k6 run scripts/00_smoke_test.js

# 다른 환경
BASE_URL=http://your-api-gateway:8000 k6 run scripts/00_smoke_test.js
```

**성공 조건**:
- 모든 체크가 통과
- Error rate = 0%
- 실제 Controller 경로 확인 완료

---

## Smoke Test 세부사항

### 테스트 항목

1. **API Gateway Health Check**
   - GET `/actuator/health`
   - Gateway가 정상 동작하는지 확인

2. **Product Service**
   - GET `/api/products` - 상품 목록
   - GET `/api/products/{id}` - 상품 상세 (404 허용)

3. **Auction Service**
   - GET `/api/v1/auctions` - 경매 목록
   - GET `/api/v1/auctions/{id}` - 경매 상세 (404 허용)

4. **Member Service**
   - POST `/api/members/login` - 로그인 엔드포인트 접근성 (400/401 허용)

5. **Legacy Path Check**
   - GET `/api/auctions` vs `/api/v1/auctions` 경로 차이 확인

---

## API 경로 정리

### 실제 Controller 경로 (Phase 1 분석 결과)

| Service | Endpoint | Controller Path |
|---------|----------|-----------------|
| Product | GET /api/products | ✅ `/api/products` |
| Product | GET /api/products/{id} | ✅ `/api/products/{id}` |
| Auction | GET /api/v1/auctions | ✅ `/api/v1/auctions` |
| Auction | GET /api/v1/auctions/{id} | ✅ `/api/v1/auctions/{id}` |
| Bid | POST /api/v1/auctions/{id}/bids | ✅ `/api/v1/auctions/{auctionId}/bids` |
| Member | POST /api/members/login | ✅ `/api/members/login` |
| Order | POST /api/order/create | ✅ `/api/order/create` |

**주의**: 기존 테스트 문서는 `/api/auctions`를 사용했으나, 실제는 `/api/v1/auctions`

---

## 환경 요구사항

### 필수 서비스 실행

```bash
# API Gateway
lsof -i :8000 || echo "❌ API Gateway 미실행"

# Microservices
lsof -i :8081 || echo "❌ Member Service 미실행"
lsof -i :8082 || echo "❌ Product Service 미실행"
lsof -i :8083 || echo "❌ Order Service 미실행"
lsof -i :8084 || echo "❌ Auction Service 미실행"
```

### 외부 서비스 (Docker on EC2)

```bash
# K8s에서 외부 서비스 연결 확인
kubectl get endpoints -n biddy redis postgres kafka
```

---

## Phase 1 완료 조건

> 테스트 전략 문서 인용:
> "새 환경에서 문서만 보고 smoke test가 성공하며 두 번 실행해도 결과가 깨지지 않는다."

- [x] API 경로 분석 완료
- [x] Smoke test 스크립트 작성
- [ ] Smoke test 실행 및 결과 확인
- [ ] 환경 스펙 문서화
- [ ] 두 번 연속 실행 시 idempotent 확인

---

## 다음 단계 (Phase 2+)

1. **Phase 2**: Business Invariant Tests
2. **Phase 3**: API Contract Tests
3. **Phase 4**: Integration Tests
4. **Phase 5**: Recovery & Failure Tests
5. **Phase 6**: Performance & Load Tests

**참고**: 성능 테스트는 Phase 1-5 완료 후 진행 (테스트 전략 문서 권고)
