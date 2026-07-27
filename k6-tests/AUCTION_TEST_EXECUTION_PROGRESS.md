# Auction AWS k6 테스트 실행 절차 및 진행 현황

## 1. 테스트 운영 방식

현재 테스트는 개발자 PC에서 k6를 실행해 AWS의 공개 API Gateway를 호출한다.

```text
개발자 PC k6
  -> 인터넷
  -> https://43.202.187.240.nip.io
  -> API Gateway
  -> Auction Service
  -> NAS PostgreSQL
```

테스트 결과에는 PC와 인터넷 회선의 지연이 포함된다. 따라서 서버의 절대 최대 TPS보다 다음 항목을 확인하는 데 사용한다.

- 외부 사용자 관점의 응답 시간
- 변경 전후 상대적인 성능 차이
- 동시 입찰 시 Auction과 Bid의 데이터 정합성
- 종료 시점의 입찰 승인 규칙
- Auction Pod, Hikari, PostgreSQL, NAS 중 최초 병목 위치

## 2. 현재 진행 현황

기준 시각: 2026-07-27 10:23 KST

| 단계 | 상태 | 결과 또는 다음 조건 |
|---|---|---|
| k6 설치 확인 | 완료 | `k6 v2.1.0`, macOS arm64 |
| 보조 도구 확인 | 완료 | `jq`, `psql`, `kubectl` 설치됨 |
| 로컬 자원 간섭 확인 | 완료 | Colima 중지 상태, Docker API 미사용 상태 |
| AWS Gateway 주소 확인 | 완료 | `https://43.202.187.240.nip.io` |
| Gateway Health | 통과 | `/actuator/health` 응답 `UP` |
| 공개 Auction 목록 | 통과 | LIVE Auction 조회 HTTP 200 |
| 신규 k6 소스 정적 검증 | 통과 | `10_`~`15_` 전체 `k6 inspect` 성공 |
| PC -> AWS 읽기 Smoke | 통과 | 1 VU, 시스템 오류 0%, p95 92.87ms |
| 토큰 파일 생성 | 부분 완료 | `/tmp/auction-users.json`, 권한 `600`; 현재 예시 토큰이므로 실제 토큰 입력 필요 |
| 결과 디렉터리 생성 | 완료 | `k6-tests/results` |
| PC -> NAS PostgreSQL 포트 | 통과 | `1.234.196.160:15432 - accepting connections` |
| 전용 테스트 Auction 준비 | 완료 | `A-K6-PREF01`, KST 보정 후 공개 API에서 `LIVE`·현재가 100000·입찰 0건 확인 |
| Preflight 쓰기 | 대기 | 전용 Auction과 입찰자 JWT 파일 필요 |
| Read Baseline | 대기 | Smoke 통과 후 5~10 VU 실행 |
| Bid Hotspot | 대기 | 서로 다른 입찰 계정 3명부터 시작 |
| Closing Spike | 대기 | 종료 35~75초 전 전용 경매 필요 |
| Mixed Stress | 미승인 | Phase 0~3 통과와 팀 승인 필요 |
| Soak | 미승인 | 안전 VU 확정 후 10분부터 시작 |

## 3. 최초 실측 결과

### 3.1 연결 확인

```text
GET /actuator/health                     -> 200, UP
GET /api/v1/auctions?status=LIVE&size=5 -> 200
```

### 3.2 읽기 전용 Smoke

| 항목 | 결과 |
|---|---:|
| 실행 위치 | 개발자 PC |
| 대상 | AWS 공개 API Gateway |
| 테스트 경매 | `A-E7129` — 읽기 전용으로만 사용 |
| VU | 1 |
| 실행 시간 | 약 12초 |
| HTTP 요청 | 14 |
| 완료 iteration | 13 |
| 시스템 오류 | 0% |
| 조회 실패 | 0% |
| 평균 응답 | 45.84ms |
| 중앙값 | 35.06ms |
| p95 | 92.87ms |
| p99 | 108.67ms |
| 최대 | 112.62ms |
| 판정 | PASS |

실행 명령:

```bash
k6 run \
  -e BASE_URL=https://43.202.187.240.nip.io \
  -e AUCTION_ID=A-E7129 \
  -e ALLOW_NON_TEST_AUCTION=true \
  -e RUN_ID=20260727-read-smoke-01 \
  -e PROFILE=smoke \
  --summary-export=/tmp/auction-read-smoke-20260727.json \
  k6-tests/scripts/11_auction_read_baseline.js
```

이 테스트는 공개 경매에 쓰기를 수행하지 않았다. 1 VU 결과이므로 처리 한계가 아니라 다음 단계 실행 전에 PC와 AWS 경로가 정상임을 확인한 결과다.

## 4. 토큰 준비 방법

토큰은 채팅, 문서, Git 커밋 또는 명령행 인수에 직접 넣지 않는다. 개발자 PC의 임시 파일에 저장한다.

현재 프론트엔드는 Access Token을 `localStorage.accessToken`에 저장한다. 로그인한 브라우저의 개발자 도구 Console에서 다음 명령으로 클립보드에만 복사한다.

```javascript
copy(localStorage.getItem("accessToken"))
```

```bash
cp k6-tests/data/auction-users.example.json /tmp/auction-users.json
chmod 600 /tmp/auction-users.json
```

파일 형식:

```json
[
  {
    "memberId": 10001,
    "token": "실제_ACCESS_TOKEN"
  }
]
```

준비 기준:

- Preflight: 판매자가 아닌 입찰자 1명
- Hotspot Smoke: 서로 다른 입찰자 3명
- Hotspot 정식: 서로 다른 입찰자 20명
- Closing Spike: 최대 VU와 같은 수의 입찰자, 기본 30명
- Stress: 단계별 최대 VU와 같은 수의 입찰자, 최대 100명
- Soak: `SOAK_VUS`와 같은 수의 입찰자

하나의 토큰을 여러 VU가 공유하면 실제 사용자 분산과 다르고 동일 사용자 정책의 영향을 받을 수 있으므로 정식 결과에는 사용하지 않는다. 판매자 계정이 토큰 풀에 포함되면 스크립트가 실행을 중단한다.

## 5. 전용 테스트 경매 준비

쓰기 테스트에서는 운영 경매를 사용하지 않는다. ID가 `A-K6-`로 시작하는 경매만 사용한다.

필요한 값:

| 값 | 조건 |
|---|---|
| `auction_id` | `A-K6-` 접두사, 20자 이내 |
| `product_id` | 다른 Auction이 사용하지 않는 테스트 전용 상품 |
| `seller_id` | 실제 존재하는 테스트 판매자 |
| `start_price` | 예: `100000` |
| `min_increment` | 예: `1000` |
| `ends_in_seconds` | 시나리오 실행 시간보다 길게 설정 |

준비 SQL은 Mac의 zsh에서 실행한다. `<NAS_DB_HOST>` 같은 설명용 꺾쇠 자리표시자를 그대로 입력하면 zsh가 파일 입력 연산자로 해석하므로 사용하지 않는다.

PostgreSQL 사용자명과 비밀번호를 모르면 AWS Master에서 다음 명령으로 본인만 확인한다. 출력값은 채팅이나 문서에 복사하지 않는다.

```bash
sudo k3s kubectl get secret biddy-secret -n biddy \
  -o jsonpath='{.data.POSTGRES_USER}' | base64 --decode
echo

sudo k3s kubectl get secret biddy-secret -n biddy \
  -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 --decode
echo
```

사용자명은 다음 Mac 명령의 프롬프트에, 비밀번호는 `psql -W`가 표시하는 비밀번호 프롬프트에 입력한다.

```bash
read "DB_USER?PostgreSQL 사용자명: "

psql -h 1.234.196.160 -p 15432 -U "$DB_USER" -W -d biddy_auction \
  -v auction_id=A-K6-PREF01 \
  -v product_id=900001 \
  -v seller_id=900001 \
  -v start_price=100000 \
  -v min_increment=1000 \
  -v ends_in_seconds=3600 \
  -f k6-tests/scripts/setup_auction_aws_test_data.sql
```

현재 PC에서 NAS의 `1.234.196.160:15432` 포트가 연결되는 것을 확인했다. `-W` 프롬프트에서 비밀번호를 입력하며 비밀번호는 명령, 문서 또는 채팅에 기록하지 않는다.

`900001`은 Auction 도메인만 격리해 테스트할 때 사용할 수 있는 예시 참조값이다. 입찰 토큰의 `memberId`가 `900001`이면 self-bid 방지를 위해 다른 seller ID를 사용한다. Product와 결합된 화면까지 검증하려면 실제 테스트 상품과 판매자 ID로 교체한다.

준비 SQL은 Auction 스케줄러와 동일하게 `Asia/Seoul` 기준 시간을 저장한다. 실행 직후 공개 상세 API가 `LIVE`인지 확인하고, `ENDED`이면 쓰기 테스트를 시작하지 않는다.

## 6. 권장 실행 순서

### Step 0. 실행 전 기록

- [ ] 테스트 시간 팀 공유
- [ ] AWS 배포 커밋 또는 이미지 SHA 기록
- [ ] Auction Pod 수와 재시작 횟수 기록
- [ ] 로컬 PC CPU·메모리·네트워크 상태 확인
- [ ] Docker와 Colima 중 불필요한 런타임 종료
- [ ] Grafana 또는 `kubectl top` 관측 준비
- [ ] 결과 저장 디렉터리 생성

```bash
mkdir -p k6-tests/results
```

### Step 1. Preflight

목적은 부하 측정이 아니라 경로, 인증, API 계약과 쓰기 데이터가 올바른지 확인하는 것이다.

```bash
k6 run \
  -e BASE_URL=https://43.202.187.240.nip.io \
  -e AUCTION_ID=A-K6-PREF01 \
  -e TOKENS_FILE=/tmp/auction-users.json \
  -e RUN_ID=20260727-preflight-01 \
  -e ALLOW_AUCTION_WRITES=true \
  --summary-export=k6-tests/results/20260727-preflight-01.json \
  k6-tests/scripts/10_auction_preflight.js
```

통과 후 즉시 DB 정합성 SQL로 확인한다. 실패하면 Baseline이나 Hotspot으로 진행하지 않는다.

### Step 2. Read Baseline

먼저 `PROFILE=smoke`를 실행하고 통과하면 `PROFILE=baseline`을 실행한다. 전용 읽기 경매를 권장하지만 쓰기를 하지 않으므로 승인된 경우 LIVE 경매를 읽기 전용으로 사용할 수 있다.

판정:

- 조회 실패율 1% 미만
- 시스템 오류율 1% 미만
- p95 500ms 미만
- Pod restart 0
- 로컬 PC가 부하 발생 병목이 아님

### Step 3. Bid Hotspot

```text
3 VU Smoke -> 정합성 확인 -> 5 VU -> 10 VU -> 20 VU
```

각 단계가 끝날 때마다 다음을 확인한다.

- `auction_system_failures` 1% 미만
- `auction_auth_failures` 0
- `auction_consistency_failures` 0
- DB deadlock 0
- Auction `current_bid`, `current_bidder_id`, `bid_count` 일치
- p95와 DB lock wait 동시 기록

정상적인 가격 경합 400/409는 실패 TPS에 포함하지 않고 `auction_business_rejections`로 별도 기록한다.

### Step 4. Closing Spike

실행 직전 전용 경매의 종료 시간을 약 60초 뒤로 다시 준비한다. PC와 서버의 시계 차이를 확인하고, 종료 2초 이후 시작된 요청이 201로 승인되면 즉시 실패로 판정한다.

확인 항목:

- `auction_after_end_accepted == 0`
- 최종 Auction 상태 `ENDED`
- 낙찰자와 최고 Bid 일치
- 종료 스케줄러 지연
- 종료와 입찰 동시 실행 중 5xx 없음

### Step 5. Mixed Stress

Phase 0~3 통과 후에만 실행한다. 최초 실행은 `PROFILE=smoke`로 제한하고 이후 팀 승인 후 `PROFILE=stress`를 사용한다.

```text
10 VU -> 25 VU -> 50 VU -> 100 VU
```

목적은 100 VU 성공을 증명하는 것이 아니라 p95, 5xx, Hikari, DB 락 또는 로컬 PC 중 최초 한계 지점을 찾는 것이다.

### Step 6. Soak

Stress에서 확인한 안전 VU의 50~60%로 시작한다.

```text
10분 -> 결과 확인 -> 60분 -> 필요하면 4시간
```

시간이 지날수록 Heap, RSS, Hikari active connection, 응답 p95, Kafka/Outbox 적체가 증가하는지 관찰한다.

## 7. 즉시 중단 조건

다음 중 하나라도 발생하면 테스트를 중단하고 데이터와 로그를 먼저 보존한다.

- Auction과 Bid 데이터 불일치
- 종료 시각 이후 확실한 입찰 승인
- 5xx 또는 네트워크 오류율이 1분간 5% 초과
- Auction Pod OOMKilled 또는 CrashLoopBackOff
- Hikari 사용률 90% 이상 지속 또는 connection timeout
- PostgreSQL deadlock 반복
- NAS DB로 인해 다른 서비스 장애 발생
- 테스트 Auction이 아닌 데이터에 쓰기 발생
- 로컬 PC CPU 90% 이상 또는 목표 부하 생성 실패

## 8. 매 실행 결과 기록 양식

```markdown
### 실행 ID
- RUN_ID:
- 실행 시각:
- 실행자:
- 스크립트/PROFILE:

### 환경
- Git commit:
- AWS 배포 이미지 SHA:
- Auction Pod 수:
- PC 사양/네트워크:
- Auction ID:

### k6 결과
- 요청 수/RPS:
- p50/p95/p99:
- system failures:
- business rejections:
- auth failures:
- consistency failures:
- dropped iterations:

### AWS/DB 결과
- Pod CPU/메모리/restart:
- Hikari active/max/wait:
- PostgreSQL lock wait/deadlock:
- NAS CPU/I/O:

### 정합성
- current_bid == highest bid:
- current_bidder_id == highest bidder:
- bid_count == Bid count:

### 결론
- PASS/FAIL:
- 최초 병목:
- 다음 개선 대상:
- 다음 단계 진행 여부:
```

## 9. 현재 다음 행동

1. `/tmp/auction-users.json`의 `REPLACE_...` 값을 판매자가 아닌 실제 입찰자 토큰으로 교체한다.
2. AWS의 `biddy-secret`에서 PostgreSQL 사용자명과 비밀번호를 본인 터미널에서 확인한다.
3. `A-K6-PREF01` 준비와 공개 API의 `LIVE` 상태 확인은 완료됐다.
4. 토큰 파일 준비가 완료되면 Preflight를 실행한다.
5. Preflight와 DB 정합성이 통과하면 Read Baseline으로 진행한다.

토큰 값 자체는 공유하지 않고 준비된 로컬 파일 경로만 사용한다.
