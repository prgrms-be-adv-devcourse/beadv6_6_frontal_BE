# Auction AWS k6 테스트 실행 현황

> 최종 갱신: 2026-07-27 14:03 KST
> 상세 실행 절차와 명령은 [`k6-tests/AUCTION_TEST_EXECUTION_PROGRESS.md`](../k6-tests/AUCTION_TEST_EXECUTION_PROGRESS.md)를 기준으로 한다.

## 1. 테스트 환경

```text
개발자 PC k6
  -> 인터넷
  -> https://43.202.187.240.nip.io
  -> API Gateway
  -> Auction Service
  -> NAS PostgreSQL
```

현재 테스트는 별도 부하 발생 인스턴스 없이 개발자 PC에서 AWS 공개 API를 호출한다. 결과는 Auction 서버만의 절대 최대 TPS가 아니라 외부 사용자 관점의 E2E 응답 시간과 변경 전후 상대 비교값으로 해석한다.

## 2. 현재 상태

| 단계 | 상태 | 결과 또는 다음 조건 |
|---|---|---|
| AWS Gateway 연결 | 통과 | Health `UP`, 공개 Auction 조회 HTTP 200 |
| Auction k6 소스 검증 | 통과 | `10_`~`15_` 스크립트 `k6 inspect` 성공 |
| PC -> AWS 읽기 Smoke | 통과 | 1 VU, 오류 0%, p95 92.87ms |
| 인증 입력 파일 | 준비 완료 | `/tmp/auction-credentials.json` 형식 검증 통과 |
| AWS 로그인 | 미검증 | 1차 Preflight가 경매 상태 검사에서 먼저 중단됨 |
| 전용 테스트 경매 | 재생성 필요 | `A-K6-PREF01`의 현재 상태 `ENDED` |
| Preflight 1차 | 중단 | 완료 iteration 0건, 입찰 및 DB 변경 없음 |
| Preflight 2차 | 대기 | 경매를 `LIVE`로 재생성한 뒤 실행 |
| Read Baseline 이후 | 대기 | Preflight와 DB 정합성 통과 후 진행 |
| Stress·Soak | 미승인 | Phase 0~3 통과와 팀 승인 필요 |

## 3. 완료된 읽기 Smoke

| 항목 | 결과 |
|---|---:|
| 실행 위치 | 개발자 PC |
| 대상 | AWS 공개 API Gateway |
| 테스트 경매 | `A-E7129` — 읽기 전용 |
| HTTP 요청 | 14 |
| 시스템 오류 | 0% |
| 평균 응답 | 45.84ms |
| p95 | 92.87ms |
| p99 | 108.67ms |
| 판정 | PASS |

이 결과는 1 VU 연결 확인 결과이며 서버의 처리 한계를 의미하지 않는다.

## 4. Preflight 1차 실행 결과

| 항목 | 결과 |
|---|---|
| 실행 ID | `20260727-preflight-01` |
| 실행 시각 | 2026-07-27 14:02 KST |
| 인증 방식 | `AUTH_MODE=credentials` |
| 대상 경매 | `A-K6-PREF01` |
| 상세 조회 | HTTP 200, 35.451ms |
| 확인된 경매 상태 | `ENDED` |
| 완료 iteration | 0 |
| 로그인 API | 실행되지 않음 |
| 입찰 요청 | 실행되지 않음 |
| DB 변경 | 없음 |
| 판정 | **STOPPED — Preflight 미통과** |

중단 오류:

```text
Error: Preflight auction must be LIVE; status=ENDED
at setup (k6-tests/scripts/10_auction_preflight.js:63)
```

Preflight의 `setup()`은 경매 상태를 먼저 확인하고 그다음 로그인 API를 호출한다. 따라서 자격증명 파일 형식은 준비됐지만 실제 AWS 로그인과 JWT 전달은 아직 검증되지 않았다.

출력에 일부 실패율과 임계값이 0%로 표시됐지만 표본이 0개인 항목이므로 PASS로 판단하지 않는다. 이번 실행의 유효한 판정 근거는 `setup()` 예외와 완료 iteration 0건이다.

원본 요약은 로컬 `k6-tests/results/20260727-preflight-01.json`에 보존하며 Git에는 커밋하지 않는다.

## 5. Preflight 2차 실행 절차

### 5.1 테스트 경매 재생성

아래 SQL은 `A-K6-PREF01`에 속한 기존 테스트 데이터만 초기화한다.

```bash
psql -h 1.234.196.160 -p 15432 -U biddy -W -d biddy_auction \
  -v auction_id=A-K6-PREF01 \
  -v product_id=900001 \
  -v seller_id=900001 \
  -v start_price=100000 \
  -v min_increment=1000 \
  -v ends_in_seconds=7200 \
  -f k6-tests/scripts/setup_auction_aws_test_data.sql
```

### 5.2 공개 API 상태 확인

```bash
curl -fsS \
  https://43.202.187.240.nip.io/api/v1/auctions/A-K6-PREF01 \
  | jq '{auctionId, status, currentBid, currentBidderId, bidCount, endsAt}'
```

`status=LIVE`, `currentBid=100000`, `currentBidderId=null`, `bidCount=0`일 때만 다음 단계로 진행한다.

### 5.3 Preflight 재실행

```bash
k6 run \
  -e BASE_URL=https://43.202.187.240.nip.io \
  -e AUCTION_ID=A-K6-PREF01 \
  -e AUTH_MODE=credentials \
  -e CREDENTIALS_FILE=/tmp/auction-credentials.json \
  -e RUN_ID=20260727-preflight-02 \
  -e ALLOW_AUCTION_WRITES=true \
  --summary-export=k6-tests/results/20260727-preflight-02.json \
  k6-tests/scripts/10_auction_preflight.js
```

## 6. 2차 실행 통과 조건

- 공개 목록·상세 조회 HTTP 200
- 비로그인 입찰 HTTP 401
- 자동 로그인과 `/api/members/me` 조회 성공
- 인증된 최소 입찰 1건 HTTP 201
- `auction_preflight_failures == 0`
- `auction_auth_failures == 0`
- `auction_system_failures == 0`
- `auction_consistency_failures == 0`
- Auction의 현재가·최고 입찰자·입찰 수와 Bid 이력 일치

Preflight와 DB 정합성이 모두 통과한 뒤 Read Baseline과 Bid Hotspot을 진행한다.

## 7. 관련 문서

- [Auction AWS 테스트 계획](../k6-tests/AUCTION_AWS_TEST_PLAN.md)
- [Auction 테스트 소스 가이드](../k6-tests/AUCTION_TEST_SOURCE_GUIDE.md)
- [상세 실행 절차 및 진행 현황](../k6-tests/AUCTION_TEST_EXECUTION_PROGRESS.md)
- [Auction 상세 문서](./AUCTION_DETAILED_DOCUMENTATION.md)
