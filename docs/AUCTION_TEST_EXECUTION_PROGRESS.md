# Auction AWS k6 테스트 실행 현황

> 최종 갱신: 2026-07-27 14:30 KST
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

### 단계별 진행 원칙

처음 k6를 사용하는 실행자가 결과를 잘못 해석하거나 다음 단계를 건너뛰지 않도록 다음 순서를 지킨다.

```text
한 단계의 명령만 실행
→ 출력 공유
→ PASS 또는 STOP 판정
→ 이 문서에 실제 결과 기록
→ 로컬 커밋
→ 다음 단계 명령 안내
```

- 앞 단계가 PASS로 기록되기 전에는 다음 단계 명령을 실행하지 않는다.
- 화면에 임계값이 0%로 보여도 표본 수와 완료 iteration을 함께 확인한다.
- 비밀번호, Access Token과 DB Secret은 출력하거나 문서에 기록하지 않는다.
- 실패하거나 중단되면 테스트 데이터를 먼저 삭제하지 않고 결과와 원인을 보존한다.
- 재실행은 새로운 `RUN_ID`를 사용해 이전 결과를 덮어쓰지 않는다.

### 현재 체크포인트

현재 실행할 단계는 **Step 1-A — AWS K3s Secret의 DB 비밀번호 재확인**이다. 비밀번호를 확인하기 전에는 SQL을 재실행하지 않는다.

| 단계 | 상태 | 결과 또는 다음 조건 |
|---|---|---|
| AWS Gateway 연결 | 통과 | Health `UP`, 공개 Auction 조회 HTTP 200 |
| Auction k6 소스 검증 | 통과 | `10_`~`15_` 스크립트 `k6 inspect` 성공 |
| PC -> AWS 읽기 Smoke | 통과 | 1 VU, 오류 0%, p95 92.87ms |
| 인증 입력 파일 | 준비 완료 | `/tmp/auction-credentials.json` 형식 검증 통과 |
| AWS 로그인 | 미검증 | 1차 Preflight가 경매 상태 검사에서 먼저 중단됨 |
| PC -> NAS DB 인증 | 중단 | NAS DB 연결 후 `password authentication failed for user "biddy"` 발생 |
| 전용 테스트 경매 | 재생성 필요 | `A-K6-PREF01`의 현재 상태 `ENDED` |
| Preflight 1차 | 중단 | 완료 iteration 0건, 입찰 및 DB 변경 없음 |
| Preflight 2차 | 대기 | 경매를 `LIVE`로 재생성한 뒤 실행 |
| Read Baseline 이후 | 대기 | Preflight와 DB 정합성 통과 후 진행 |
| Stress·Soak | 미승인 | Phase 0~3 통과와 팀 승인 필요 |

### 인증 테스트 계정 전제 조건

k6의 `AUTH_MODE=credentials`는 회원가입 기능이 아니라 기존 회원의 로그인 기능을 사용한다.

```text
이메일 인증 완료
→ 회원가입 완료
→ 회원 상태 ACTIVE
→ /tmp/auction-credentials.json에 이메일·비밀번호 저장
→ k6 setup()에서 POST /api/members/login
→ GET /api/members/me로 memberId 확인
→ Auction 입찰 요청
```

- Member 서비스의 회원가입은 이메일 인증 이력이 없으면 `이메일 인증이 필요합니다.`로 거절된다.
- k6는 `/api/members/email/send`, `/email/verify`, `/signup`을 실행하지 않는다.
- 존재하지 않는 이메일, 틀린 비밀번호, `SUSPENDED` 또는 `WITHDRAWN` 회원은 로그인할 수 없다.
- 입찰자는 테스트 경매의 판매자와 다른 회원이어야 한다.
- 로그인할 때 기존 Refresh Token이 교체되므로 개인 계정보다 별도 `ACTIVE` 테스트 계정을 권장한다.
- 현재 `/tmp/auction-credentials.json`은 파일 형식만 검증된 상태이며 실제 AWS 로그인 성공 여부는 Preflight 2차에서 확인한다.

현재 발생한 `password authentication failed for user "biddy"`는 Member 이메일 로그인이 아니라 NAS PostgreSQL 접속 계정의 오류다. 회원 이메일 인증 여부와 관계없이 DB 비밀번호부터 바로잡아 테스트 경매를 생성해야 한다.

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

#### Step 1 실행 1차 결과 — DB 인증에서 중단

| 항목 | 결과 |
|---|---|
| 실행 시각 | 2026-07-27 14:25 KST |
| 접속 대상 | `1.234.196.160:15432` |
| 데이터베이스 | `biddy_auction` |
| 사용자 | `biddy` |
| 네트워크 연결 | 성공 — PostgreSQL 서버가 인증 오류를 반환함 |
| DB 인증 | 실패 |
| SQL 실행 | 실행되지 않음 |
| DB 변경 | 없음 |
| 판정 | **STOPPED — 비밀번호 재확인 필요** |

오류:

```text
FATAL: password authentication failed for user "biddy"
```

이 오류는 NAS 주소나 포트 연결 문제가 아니다. PostgreSQL 서버까지 요청이 도착했지만 입력한 비밀번호가 현재 서버의 인증 정보와 일치하지 않는 상태다. SQL 파일을 읽기 전에 연결이 종료됐으므로 `DELETE`, `INSERT`와 `COMMIT`은 실행되지 않았다.

AWS Master에서 다음 명령으로 K3s Secret의 현재 비밀번호를 본인 화면에서만 확인한다. 출력된 값은 채팅, 문서나 Git에 기록하지 않는다.

```bash
sudo k3s kubectl get secret biddy-secret -n biddy \
  -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 --decode
echo
```

비밀번호 확인 후 같은 SQL 명령을 다시 실행한다. K3s Secret의 비밀번호로도 인증에 실패하면 Secret과 NAS PostgreSQL의 실제 비밀번호가 불일치할 수 있으므로 SQL 재시도를 멈추고 별도 진단한다.

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
