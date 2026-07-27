# Auction AWS k6 테스트 계획

## 1. 문서 정보

| 항목 | 내용 |
|---|---|
| 대상 | Auction 도메인 API |
| 실행 환경 | AWS K3s 및 NAS PostgreSQL 연동 환경 |
| 부하 도구 | k6 |
| 작성일 | 2026-07-27 |
| 현재 단계 | 계획 수립, 기존 스크립트 재작성 전 |

이 문서는 AWS에 배포된 Auction 기능만 검증하기 위한 실행 계획이다. Member는 테스트 토큰 발급을 위한 사전 조건으로만 사용하고 Product, Order, Payment의 성능은 측정 범위에서 제외한다.

---

## 2. 테스트 목표

1. 비로그인 경매 목록·상세 조회의 처리 성능을 확인한다.
2. 로그인 사용자의 입찰 요청이 집중될 때 비관적 잠금이 데이터 정합성을 보장하는지 확인한다.
3. 경매 종료 직전의 급격한 입찰 증가를 재현한다.
4. Auction Pod, Gateway, NAS PostgreSQL의 병목 지점을 구분한다.
5. 부하 종료 후 `current_bid`, `current_bidder_id`, `bid_count`가 Bid 이력과 일치하는지 확인한다.
6. 한계 부하와 안전 운영 구간을 구분해 이후 리소스·HPA 기준을 마련한다.

### 핵심 성공 조건

- 시스템 5xx 및 네트워크 오류율 1% 미만
- 정상 부하에서 조회 P95 500ms 이하
- 정상 부하에서 입찰 P95 1초 이하
- DB Deadlock 0건
- Auction과 Bid 데이터 불일치 0건
- Auction Pod OOM 또는 반복 재시작 0건

초기 기준은 첫 Baseline 결과에 따라 조정하되 데이터 정합성 0건 기준은 완화하지 않는다.

---

## 3. 범위

### 포함

| 구분 | Endpoint/기능 |
|---|---|
| 조회 | `GET /api/v1/auctions` |
| 조회 | `GET /api/v1/auctions/{auctionId}` |
| 조회 | `GET /api/v1/auctions/{auctionId}/bids` |
| 입찰 | `POST /api/v1/auctions/{auctionId}/bids` |
| 인증 정책 | 비로그인 GET 허용, 비로그인 POST 401 |
| 동시성 | 동일 경매 행에 대한 비관적 잠금 |
| 데이터 | 현재가, 최고 입찰자, 입찰 수 정합성 |
| 운영 | Pod·DB 커넥션·HTTP 지표 관찰 |

### 제외

- 상품 등록 및 Product 이벤트 소비 성능
- 회원가입·로그인 성능
- 주문·결제·정산 성능
- 실제 운영 사용자와 운영 경매
- 실제 결제나 낙찰 주문을 만드는 시나리오
- 첫 실행에서의 장시간 Soak 및 장애 주입

경매 종료 이벤트를 테스트할 때는 Order·Payment에 영향을 주지 않는 격리 환경 또는 별도 합의가 필요하다.

---

## 4. 현재 환경과 제약

저장소 기준 Auction 배포는 다음 상태이다.

- `replicas: 1`
- 배포 전략 `Recreate`
- Auction 전용 HPA 없음
- Auction 컨테이너의 명시적 requests/limits 없음
- PostgreSQL은 K8s 내부 Pod가 아니라 NAS의 외부 Endpoint 사용
- K8s 내부 접속 주소는 `postgres:15432`

실제 클러스터 상태는 실행 직전에 다시 확인한다.

```bash
sudo k3s kubectl get deployment auction -n biddy -o wide
sudo k3s kubectl get hpa -n biddy
sudo k3s kubectl get service postgres -n biddy -o wide
sudo k3s kubectl get endpoints postgres -n biddy -o wide
sudo k3s kubectl top pod -n biddy -l app=auction
```

Auction에 리소스 제한이 없으므로 첫 실행부터 최대 부하를 가하지 않는다. 단계별 통과와 중단 기준을 적용한다.

---

## 5. 기존 k6 파일 점검 결과

현재 `k6-tests/scripts`의 Auction 파일은 참고용이며 AWS에서 그대로 실행하지 않는다.

| 파일 | 확인된 문제 |
|---|---|
| `01_auction_smoke_test.js` | `localhost:8084` 고정, Gateway 미경유 |
| `02_auction_business_rules_test.js` | JWT 없이 401도 성공으로 처리 |
| `03_auction_concurrency_test.js` | VU별 입찰 금액이 중복될 수 있고 성공 임계값이 없음 |
| `setup_auction_test_data.sql` | 현재 Auction/Bid 컬럼과 다른 SQL 포함 |

특히 다음 항목을 수정하기 전에는 AWS 쓰기 부하를 실행하지 않는다.

- `BASE_URL` 환경 변수화
- Gateway 경유 경로 사용
- 실제 테스트 JWT 사용
- 예상 비즈니스 거절과 시스템 오류 분리
- 현재 DB 스키마 기반 테스트 데이터 생성·정리 SQL 작성
- 테스트별 고유 Auction ID 사용
- 결과 JSON 저장 및 실행 ID 태깅

---

## 6. 테스트 데이터 전략

### 전용 경매

| 용도 | 예시 ID | 특징 |
|---|---|---|
| 조회 부하 | `A-K6-READ01` | 변경하지 않는 LIVE 경매 |
| 입찰 Hotspot | `A-K6-HOT01` | 동일 행 잠금 테스트 |
| Spike | `A-K6-SPIKE1` | 종료 직전 입찰 집중 |
| 복구 | `A-K6-RECOV1` | Pod 재시작 전용 |

Auction ID는 실제 Entity 길이 제한인 20자 이내로 만든다.

### 테스트 사용자

- 판매자 1명
- 입찰자 최소 20명
- Stress 단계는 최대 100명까지 확장
- 사용자마다 유효한 Access Token을 별도 파일로 준비
- 토큰 파일은 Git에 커밋하지 않음
- 한 사용자의 토큰을 모든 VU가 공유하지 않음

예정 파일:

```text
k6-tests/data/auction-users.example.json  # 형식만 커밋
k6-tests/data/auction-users.json          # 실제 토큰, Git 제외
```

### 데이터 격리

가장 안전한 방법은 별도 `biddy_auction_perf` DB 또는 별도 테스트 배포를 사용하는 것이다. 공유 NAS의 `biddy_auction`을 사용해야 한다면 다음 조건을 모두 만족해야 한다.

- 전용 Auction ID만 사용
- 테스트 시간 사전 공유
- 최대 부하 단계 별도 승인
- 다른 팀 서비스의 에러율 함께 관찰
- 테스트 종료 후 정합성 확인 전 데이터 삭제 금지
- 결과 보존 후 테스트 데이터만 정리

---

## 7. k6 환경 변수 규격

스크립트에 주소·토큰·경매 ID를 하드코딩하지 않는다.

| 변수 | 필수 | 예시/설명 |
|---|---|---|
| `BASE_URL` | Y | `https://<API_DOMAIN>` |
| `AUCTION_ID` | Y | 테스트별 Auction ID |
| `TOKENS_FILE` | 쓰기 테스트 | 실제 토큰 JSON 경로 |
| `RUN_ID` | Y | 실행 결과 구분용 |
| `PROFILE` | Y | `smoke`, `baseline`, `hotspot`, `spike` |
| `RESULT_DIR` | N | 기본값 `results` |

예시:

```bash
k6 run \
  -e BASE_URL=https://<API_DOMAIN> \
  -e AUCTION_ID=A-K6-READ01 \
  -e RUN_ID=20260727-01 \
  -e PROFILE=smoke \
  scripts/10_auction_preflight.js
```

비밀번호와 Access Token을 명령행에 직접 입력하지 않는다. 토큰은 권한이 제한된 파일에서 `open()`으로 읽도록 구현한다.

---

## 8. 지표 정의

### k6 지표

| 지표 | 의미 |
|---|---|
| `auction_read_latency` | 목록·상세 조회 응답 시간 |
| `auction_bid_latency` | 입찰 요청 응답 시간 |
| `accepted_bids` | HTTP 200/201 입찰 |
| `business_rejections` | 금액 경합 등 예상 가능한 4xx |
| `auth_failures` | 예상하지 않은 401/403 |
| `system_failures` | 네트워크 오류 및 5xx |
| `data_consistency_failures` | 종료 검증 불일치 |

입찰 경쟁에서 뒤늦게 도착한 낮은 금액의 4xx는 정상 비즈니스 거절일 수 있다. 이를 `http_req_failed` 하나로 판단하지 않고 `business_rejections`와 `system_failures`로 분리한다.

### AWS 관측 지표

- Auction CPU·메모리·Pod restart
- Gateway와 Auction RPS, P95, P99, 5xx
- `hikaricp_connections_active/max`
- Hikari connection acquire 시간과 timeout
- PostgreSQL 활성 세션, lock wait, deadlock
- NAS CPU, 메모리, 디스크 I/O가 가능하면 함께 기록
- Kafka 발행 오류

---

## 9. 단계별 테스트

### Phase 0. Preflight Smoke

| 항목 | 값 |
|---|---|
| VU | 1 |
| 반복 | 1회 |
| 쓰기 | 인증 확인용 최소 1회 |
| 목적 | 경로, 인증, 테스트 데이터 확인 |

검증:

- 비로그인 목록·상세 HTTP 200
- 비로그인 입찰 HTTP 401
- 로그인 상세 HTTP 200
- 유효한 최소 입찰 1건 성공
- 실행 후 DB 정합성 유지

Phase 0 실패 시 이후 테스트를 진행하지 않는다.

### Phase 1. Read Baseline

| 구간 | VU | 시간 |
|---|---:|---:|
| Warm-up | 1 → 5 | 1분 |
| Normal | 5 | 3분 |
| Peak candidate | 5 → 10 | 2분 |
| Cool-down | 10 → 0 | 1분 |

트래픽 비율:

- 목록 30%
- 상세 50%
- 입찰 내역 20%

통과 기준:

- P95 500ms 이하
- 시스템 오류율 1% 미만
- Pod restart 0
- Hikari 사용률 80% 미만

### Phase 2. Bid Hotspot

동일한 Auction ID에 입찰을 집중시켜 비관적 잠금과 데이터 정합성을 확인한다.

| 단계 | VU | 시간 |
|---|---:|---:|
| H1 | 5 | 1분 |
| H2 | 10 | 1분 |
| H3 | 20 | 1분 |

금액 생성 규칙:

- 요청마다 고유 후보 금액을 사용한다.
- 동시 요청 도착 순서 때문에 모든 요청의 성공을 기대하지 않는다.
- 최종 `current_bid`는 성공한 입찰 중 최대 금액과 같아야 한다.
- 최종 `current_bidder_id`는 최대 성공 입찰의 사용자와 같아야 한다.
- `bid_count`는 실제 Bid 이력 수와 같아야 한다.

통과 기준:

- DB Deadlock 0
- 시스템 오류율 1% 미만
- P95 1초 이하
- 정합성 불일치 0

### Phase 3. Closing-time Spike

종료 직전 갑자기 입찰자가 몰리는 상황을 재현한다.

| 구간 | VU | 시간 |
|---|---:|---:|
| 평상시 | 2 | 30초 |
| 급증 | 2 → 30 | 10초 |
| 유지 | 30 | 30초 |
| 감소 | 30 → 0 | 20초 |

첫 실행은 최대 30 VU로 제한한다. 통과 후 50, 100 VU 확장을 별도 승인한다.

확인 항목:

- 급증 구간 P95/P99
- Lock wait와 Hikari 대기
- 5xx 및 timeout
- 최종 상태 정합성
- WebSocket 전파 지연은 별도 스크립트가 준비된 경우 측정

### Phase 4. Stress

Phase 0~3 성공 후에만 수행한다.

| 단계 | VU | 시간 |
|---|---:|---:|
| S1 | 10 | 2분 |
| S2 | 25 | 2분 |
| S3 | 50 | 2분 |
| S4 | 100 | 2분 |

목표는 성공을 증명하는 것이 아니라 다음 중 최초 발생 지점을 찾는 것이다.

- P95 기준 초과
- 5xx 1% 초과
- DB 커넥션 90% 이상
- Pod CPU/메모리 포화
- Lock wait 급증

중단 조건에 해당하면 다음 단계로 올리지 않는다.

### Phase 5. Soak

Stress 결과에서 확인한 안전 VU의 50~60%로 시작한다.

| 항목 | 초기 계획 |
|---|---|
| VU | 10~20 |
| 시간 | 60분 |
| 확장 | 1시간 성공 후 4시간 검토 |

확인 항목:

- Heap과 RSS의 지속 증가
- Hikari connection 반환 누락
- 응답 시간의 시간대별 증가
- Kafka 발행 누적 오류
- NAS I/O 지연

### Phase 6. Recovery

별도 승인 후 낮은 부하에서만 실행한다.

- 읽기 요청 중 Auction Pod 재시작
- 입찰 요청 중 Auction Pod 재시작
- 재기동 시간과 오류 구간 기록
- 재시작 후 상세·입찰·DB 정합성 재검증

현재 배포 전략이 `Recreate`, replica가 1이므로 재시작 중 짧은 서비스 중단은 예상된다. 데이터 손실과 비정상 장기 중단이 없어야 한다.

---

## 10. 즉시 중단 기준

다음 중 하나라도 발생하면 k6를 중단한다.

- Auction/Bid 데이터 불일치 발견
- 5xx 또는 네트워크 오류율이 1분간 5% 초과
- Auction Pod OOMKilled 또는 CrashLoopBackOff
- NAS PostgreSQL 연결 고갈 또는 다른 서비스 장애
- DB Deadlock 반복 발생
- Hikari connection 사용률 90% 이상 지속
- Gateway 또는 K3s control-plane 불안정
- 실제 운영 데이터가 테스트에 포함된 사실 발견

중단 후 테스트 데이터를 삭제하지 말고 로그와 DB 상태부터 보존한다.

---

## 11. 데이터 정합성 검증

각 쓰기 테스트 전후에 아래 결과를 저장한다.

```sql
WITH top_bid AS (
    SELECT DISTINCT ON (auction_id)
           auction_id,
           bid_id,
           bidder_id,
           amount
    FROM public.bid
    WHERE auction_id = '<TEST_AUCTION_ID>'
    ORDER BY auction_id, amount DESC, bid_at DESC, bid_id DESC
),
bid_counts AS (
    SELECT auction_id, COUNT(*)::INTEGER AS actual_bid_count
    FROM public.bid
    WHERE auction_id = '<TEST_AUCTION_ID>'
    GROUP BY auction_id
)
SELECT
    a.auction_id,
    a.current_bid,
    top_bid.amount AS highest_bid,
    a.current_bidder_id,
    top_bid.bidder_id AS highest_bidder_id,
    a.bid_count,
    COALESCE(bid_counts.actual_bid_count, 0) AS actual_bid_count
FROM public.auction a
LEFT JOIN top_bid ON top_bid.auction_id = a.auction_id
LEFT JOIN bid_counts ON bid_counts.auction_id = a.auction_id
WHERE a.auction_id = '<TEST_AUCTION_ID>';
```

판정 기준:

- `current_bid = highest_bid`
- `current_bidder_id = highest_bidder_id`
- `bid_count = actual_bid_count`

입찰이 없는 경매는 `current_bidder_id = null`이 정상이다.

---

## 12. 실행 위치

### 권장

Auction Pod가 실행되는 Worker와 분리된 전용 부하 발생기 EC2에서 k6를 실행한다.

### 제한적 대안

전용 EC2가 없으면 `biddy-master`에서 Phase 0과 낮은 부하의 Phase 1만 실행할 수 있다. Master에서 Stress·Spike·Soak를 실행하면 control-plane 자원을 사용해 결과를 왜곡하거나 클러스터를 불안정하게 만들 수 있으므로 금지한다.

Auction Worker 자체에서 k6를 실행하지 않는다. 부하 발생기와 대상 서비스가 CPU·네트워크를 경쟁하면 측정값을 신뢰할 수 없다.

---

## 13. 결과 저장

파일명 규칙:

```text
k6-tests/results/<RUN_ID>_<PROFILE>_summary.json
k6-tests/results/<RUN_ID>_<PROFILE>_raw.json
k6-tests/results/<RUN_ID>_<PROFILE>_notes.md
```

결과에 기록할 항목:

- 실행 일시와 실행자
- 배포 이미지 SHA
- Auction Pod 수와 리소스 설정
- BASE_URL과 테스트 Auction ID
- k6 옵션과 임계값
- P50/P95/P99, RPS, 5xx
- 비즈니스 거절 수
- Pod·Hikari·PostgreSQL 지표
- 테스트 전후 정합성 SQL 결과
- 중단 또는 장애 내용

토큰, 비밀번호, Secret 값은 결과 파일에 포함하지 않는다.

---

## 14. 구현 예정 파일

```text
k6-tests/
├── AUCTION_AWS_TEST_PLAN.md
├── data/
│   └── auction-users.example.json
├── scripts/
│   ├── 10_auction_preflight.js
│   ├── 11_auction_read_baseline.js
│   ├── 12_auction_bid_hotspot.js
│   ├── 13_auction_spike.js
│   ├── 14_auction_stress.js
│   ├── 15_auction_soak.js
│   ├── setup_auction_aws_test_data.sql
│   └── cleanup_auction_aws_test_data.sql
└── results/
```

WebSocket/STOMP 부하 테스트는 HTTP 단계 완료 후 별도 스크립트로 추가한다.

---

## 15. 실행 승인 절차

| 단계 | 실행 조건 | 승인 |
|---|---|---|
| Phase 0 | 스크립트 리뷰, 테스트 데이터 준비 | Auction 담당자 |
| Phase 1 | Phase 0 성공 | Auction 담당자 |
| Phase 2 | 유효 토큰 풀과 정합성 SQL 확인 | Auction 담당자 |
| Phase 3 | 모니터링 화면 준비 | 인프라 공유 |
| Phase 4 | Phase 0~3 성공, 시간 공지 | 팀 승인 |
| Phase 5 | 안전 부하 확정 | 팀 승인 |
| Phase 6 | 장애 영향과 복구 방법 확인 | 팀 승인 |

---

## 16. 다음 작업

1. AWS API Gateway 도메인과 k6 실행 호스트 확정
2. 현재 스키마에 맞는 테스트 데이터 setup/cleanup SQL 작성
3. 테스트 사용자와 JWT 파일 형식 확정
4. Phase 0 Preflight 스크립트 작성 및 코드 리뷰
5. Phase 0을 AWS에서 2회 연속 실행
6. 결과와 DB 정합성 확인 후 Phase 1 진행

계획 승인 전에는 기존 Auction 쓰기·동시성 스크립트를 AWS에서 실행하지 않는다.
