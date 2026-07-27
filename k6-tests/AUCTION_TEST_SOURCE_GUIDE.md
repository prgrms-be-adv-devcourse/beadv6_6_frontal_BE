# Auction k6 테스트 소스 가이드

> 실제 실행 순서와 현재 진행 상태는 [`AUCTION_TEST_EXECUTION_PROGRESS.md`](./AUCTION_TEST_EXECUTION_PROGRESS.md)를 확인한다.

## 1. 실행 환경 결정

현재 테스트는 개발자 PC에서 k6를 실행해 AWS의 공개 API Gateway를 호출한다.

```text
개발자 PC k6 -> 인터넷 -> AWS API Gateway -> Auction Service -> NAS PostgreSQL
```

결과는 Auction 서버만의 절대 최대 TPS가 아니라 외부 사용자 관점의 E2E 성능과 변경 전후 상대 비교값으로 해석한다.

## 2. 테스트 관점과 개선 목적

| 파일 | 테스트 관점 | 발견하려는 문제 | 결과로 개선할 부분 |
|---|---|---|---|
| `10_auction_preflight.js` | 배포·계약 정확성 | 경로, 비로그인 정책, JWT 전달, 테스트 데이터 오류 | Gateway 인증 규칙, API 계약, 데이터 준비 절차 |
| `11_auction_read_baseline.js` | 비로그인 사용자 체감 | 목록·상세·입찰내역의 기본 지연과 5xx | 조회 쿼리, 인덱스, 페이징, 캐시 |
| `12_auction_bid_hotspot.js` | 동일 경매 정합성 | 비관적 락 대기, 유실 갱신, Bid/Auction 불일치 | 락 범위, 트랜잭션 길이, 조건부 UPDATE |
| `13_auction_closing_spike.js` | 종료 경계 정확성 | 종료 직후 입찰 승인, 스케줄러 경쟁, 급증 지연 | `endsAt` 재검증, 종료 락, 스케줄러 처리 |
| `14_auction_mixed_stress.js` | 시스템 한계 | 5xx·타임아웃·지연이 처음 급증하는 부하 | Pod/JVM, Hikari, DB 락, NAS 병목 |
| `15_auction_soak.js` | 장시간 안정성 | 메모리·연결 누수와 시간이 지날수록 증가하는 지연 | 리소스 반환, 풀 설정, 이벤트 적체 |

정상적인 입찰 가격 경합으로 발생하는 400/409는 시스템 실패와 분리한다. 성능 개선 목표는 모든 입찰을 성공시키는 것이 아니라 데이터 정합성을 지키면서 시스템 오류와 지연을 낮추는 것이다.

## 3. 공통 안전장치

- `BASE_URL`을 필수로 받아 실수로 다른 환경을 호출하지 않는다.
- 기본적으로 HTTPS만 허용한다.
- 테스트 Auction ID는 `A-K6-`로 시작해야 한다.
- 쓰기 테스트는 `ALLOW_AUCTION_WRITES=true`가 필요하다.
- Stress·Soak 테스트는 추가로 `ALLOW_HIGH_LOAD=true`가 필요하다.
- JWT는 Git에 커밋하지 않은 파일에서 읽는다.
- 판매자 토큰은 입찰자 풀에 포함하지 않는다.
- 쓰기 테스트 종료 후 상세 API와 Bid 이력을 비교한다.

## 4. 토큰 파일 준비

### 4.1 권장 — Swagger와 같은 로그인 API 사용

k6는 `setup()`에서 Swagger와 동일한 `POST /api/members/login`을 한 번 호출하고, 응답의 `accessToken`을 Auction 요청에 사용한다. 이어서 `GET /api/members/me`로 회원 ID를 확인한다. 이 로그인 요청은 Auction 전용 지연 메트릭에 포함하지 않는다.

로그인하면 해당 회원의 기존 Refresh Token이 교체되므로 개인 계정보다 전용 테스트 계정을 사용한다. 실제 이메일과 비밀번호는 Git에 저장하지 않고 `/tmp` 파일에만 보관한다.

```bash
read "LOGIN_EMAIL?테스트 로그인 이메일: "
read -s "LOGIN_PASSWORD?테스트 로그인 비밀번호: "
echo

jq -n \
  --arg email "$LOGIN_EMAIL" \
  --arg password "$LOGIN_PASSWORD" \
  '[{email: $email, password: $password}]' \
  > /tmp/auction-credentials.json

chmod 600 /tmp/auction-credentials.json
unset LOGIN_EMAIL LOGIN_PASSWORD
```

Preflight 실행 시 다음 옵션을 사용한다.

```text
AUTH_MODE=credentials
CREDENTIALS_FILE=/tmp/auction-credentials.json
```

### 4.2 대안 — 이미 발급된 Access Token 사용

예제 파일을 복사하되 실제 토큰 파일은 Git에 커밋하지 않는다.

```bash
cp k6-tests/data/auction-users.example.json /tmp/auction-users.json
chmod 600 /tmp/auction-users.json
```

각 항목의 `memberId`와 `token`을 실제 테스트 회원 값으로 바꾼다. Hotspot과 Stress는 최대 VU 수 이상의 서로 다른 입찰 회원을 준비한다.

현재 프론트엔드는 로그인 후 Access Token을 브라우저 `localStorage`의 `accessToken` 키에 저장한다. 로그인된 프론트 화면에서 개발자 도구 Console을 열고 다음 명령으로 토큰을 클립보드에만 복사한다.

```javascript
copy(localStorage.getItem("accessToken"))
```

Mac zsh에서 다음 명령을 실행한 뒤 클립보드의 토큰을 붙여 넣고 Enter를 누른다. `read -s`를 사용하므로 토큰은 화면과 명령 이력에 표시되지 않는다. JWT의 회원 ID도 로컬에서 자동으로 추출한다.

```bash
read -s "TOKEN?Access Token 붙여넣기: "
echo

MEMBER_ID="$(TOKEN="$TOKEN" node -e '
  const payload = JSON.parse(Buffer.from(process.env.TOKEN.split(".")[1], "base64url"));
  const memberId = Number(payload.sub ?? payload.memberId);
  if (!Number.isSafeInteger(memberId)) process.exit(1);
  process.stdout.write(String(memberId));
')"

jq -n \
  --argjson memberId "$MEMBER_ID" \
  --arg token "$TOKEN" \
  '[{memberId: $memberId, token: $token}]' \
  > /tmp/auction-users.json

chmod 600 /tmp/auction-users.json
unset TOKEN MEMBER_ID
```

실제 토큰 값을 출력하지 않고 준비 여부만 확인한다.

```bash
jq -e '
  type == "array" and length > 0 and
  all(.[];
    (.memberId | type) == "number" and
    (.token | type) == "string" and
    (.token | startswith("REPLACE_") | not)
  )
' /tmp/auction-users.json >/dev/null \
  && echo "TOKEN READY" \
  || echo "TOKEN NOT READY"
```

### 4.3 전용 Auction 준비

테스트용 판매자·상품이 존재하는지 먼저 확인한 후 현재 스키마용 SQL을 실행한다. `product_id`는 다른 Auction이 사용하지 않는 테스트 전용 상품 ID여야 한다.

Mac의 zsh에서 다음 명령을 실행하면 사용자명과 비밀번호를 대화형으로 입력할 수 있다. 꺾쇠괄호(`<...>`)를 명령에 그대로 입력하지 않는다.

```bash
read "DB_USER?PostgreSQL 사용자명: "

psql -h 1.234.196.160 -p 15432 -U "$DB_USER" -W -d biddy_auction \
  -v auction_id=A-K6-HOT01 \
  -v product_id=900001 \
  -v seller_id=900001 \
  -v start_price=100000 \
  -v min_increment=1000 \
  -v ends_in_seconds=3600 \
  -f k6-tests/scripts/setup_auction_aws_test_data.sql
```

`900001`은 Auction 도메인 격리를 위한 예시 참조값이다. `product_id`는 Auction 테이블에서 사용 중이지 않아야 하고 `seller_id`는 입찰 토큰의 회원 ID와 달라야 한다. 실제 Product 화면 조합까지 확인하려면 별도로 생성한 테스트 상품·판매자 ID를 사용한다.

Closing Spike는 `ends_in_seconds`를 실행 준비 시간을 고려해 약 60초로 지정한다. 정합성 결과를 보존한 후 `cleanup_auction_aws_test_data.sql`로 해당 `A-K6-` 경매만 정리한다.

Auction의 시간 컬럼은 `LocalDateTime`이고 현재 AWS 스케줄러는 KST 기준으로 동작한다. 준비 SQL은 PostgreSQL 세션의 기본 시간대와 무관하게 `Asia/Seoul` 시각으로 저장한다. 생성 직후 상세 API에서 `status=LIVE`와 종료까지 남은 시간을 반드시 확인한다.

## 5. 실행 순서

결과 디렉터리를 먼저 만든다.

```bash
mkdir -p k6-tests/results
```

### 5.1 Preflight

```bash
k6 run \
  -e BASE_URL=https://<AWS_API_DOMAIN> \
  -e AUCTION_ID=A-K6-PREF01 \
  -e AUTH_MODE=credentials \
  -e CREDENTIALS_FILE=/tmp/auction-credentials.json \
  -e RUN_ID=20260727-preflight-01 \
  -e ALLOW_AUCTION_WRITES=true \
  --summary-export=k6-tests/results/20260727-preflight-01.json \
  k6-tests/scripts/10_auction_preflight.js
```

### 5.2 Read Baseline

```bash
k6 run \
  -e BASE_URL=https://<AWS_API_DOMAIN> \
  -e AUCTION_ID=A-K6-READ01 \
  -e RUN_ID=20260727-read-01 \
  -e PROFILE=baseline \
  --summary-export=k6-tests/results/20260727-read-01.json \
  k6-tests/scripts/11_auction_read_baseline.js
```

### 5.3 Bid Hotspot

```bash
k6 run \
  -e BASE_URL=https://<AWS_API_DOMAIN> \
  -e AUCTION_ID=A-K6-HOT01 \
  -e AUTH_MODE=credentials \
  -e CREDENTIALS_FILE=/tmp/auction-credentials.json \
  -e RUN_ID=20260727-hotspot-01 \
  -e PROFILE=smoke \
  -e ALLOW_AUCTION_WRITES=true \
  --summary-export=k6-tests/results/20260727-hotspot-01.json \
  k6-tests/scripts/12_auction_bid_hotspot.js
```

`PROFILE=smoke` 성공과 DB 정합성 확인 후 `PROFILE=hotspot`으로 확장한다.

### 5.4 Closing Spike

테스트 경매의 종료 시각이 실행 시점 기준 약 35~75초 뒤인지 확인한다.

```bash
k6 run \
  -e BASE_URL=https://<AWS_API_DOMAIN> \
  -e AUCTION_ID=A-K6-SPIKE1 \
  -e AUTH_MODE=credentials \
  -e CREDENTIALS_FILE=/tmp/auction-credentials.json \
  -e RUN_ID=20260727-spike-01 \
  -e ALLOW_AUCTION_WRITES=true \
  --summary-export=k6-tests/results/20260727-spike-01.json \
  k6-tests/scripts/13_auction_closing_spike.js
```

### 5.5 Stress와 Soak

Preflight, Baseline, Hotspot, Spike가 모두 성공한 뒤 팀 승인 후 실행한다.

```bash
# Stress
k6 run \
  -e BASE_URL=https://<AWS_API_DOMAIN> \
  -e AUCTION_ID=A-K6-STRS01 \
  -e AUTH_MODE=credentials \
  -e CREDENTIALS_FILE=/tmp/auction-credentials.json \
  -e ALLOW_AUCTION_WRITES=true \
  -e ALLOW_HIGH_LOAD=true \
  k6-tests/scripts/14_auction_mixed_stress.js

# Soak: 먼저 10분 검증 후 60분으로 확장
k6 run \
  -e BASE_URL=https://<AWS_API_DOMAIN> \
  -e AUCTION_ID=A-K6-SOAK01 \
  -e AUTH_MODE=credentials \
  -e CREDENTIALS_FILE=/tmp/auction-credentials.json \
  -e SOAK_DURATION=10m \
  -e ALLOW_AUCTION_WRITES=true \
  -e ALLOW_HIGH_LOAD=true \
  k6-tests/scripts/15_auction_soak.js
```

## 6. 결과 판정

| 지표 | 해석 |
|---|---|
| `auction_read_latency` | 외부 PC에서 AWS까지 포함한 조회 지연 |
| `auction_bid_latency` | Gateway, Auction 락, NAS DB를 포함한 입찰 지연 |
| `auction_business_rejections` | 가격 변경·종료 등 정상 업무 거절 수 |
| `auction_auth_failures` | 토큰 또는 Gateway 전달 문제 |
| `auction_system_failures` | 네트워크 오류, 5xx, 예상하지 않은 상태 |
| `auction_consistency_failures` | Auction 집계와 Bid 이력 불일치 |
| `auction_after_end_accepted` | 종료 경계 이후 승인된 입찰 수 |

로컬 CPU가 80~90% 이상이거나 목표 부하를 만들지 못하면 해당 결과는 AWS 서버 한계로 사용하지 않는다. k6 결과와 같은 시간대의 Auction Pod, Hikari, PostgreSQL 및 NAS 지표를 함께 비교한다.

## 7. 테스트 직후 확인

1. `auction_system_failures`와 `auction_consistency_failures`가 0인지 확인한다.
2. 400/409가 시스템 장애가 아닌 정상 가격 경합인지 응답 코드로 구분한다.
3. 문서의 정합성 SQL로 `current_bid`, `current_bidder_id`, `bid_count`를 재검증한다.
4. AWS 로그에서 deadlock, lock timeout, Hikari timeout, Pod restart를 확인한다.
5. 결과 파일과 실행 당시 환경 정보를 함께 보관한다.

## 8. 기존 스크립트와 구분

`00_smoke_test.js`부터 `03_auction_concurrency_test.js`는 localhost와 과거 요청 규격을 사용하는 기존 로컬 스크립트다. AWS 테스트에는 사용하지 않는다. AWS 테스트는 `10_`부터 `15_`까지의 스크립트만 사용한다.
