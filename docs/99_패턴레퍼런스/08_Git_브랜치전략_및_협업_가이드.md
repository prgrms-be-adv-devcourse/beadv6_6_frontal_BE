# 08. Git 브랜치 전략 및 협업 가이드

## 1. 브랜치 전략 — Simplified Git Flow

```
main (배포용, 보호 브랜치)
 │
 └── develop (통합 브랜치, PR 타겟, default)
      │
      ├── feature/auction-feed-api      ← 기능 개발
      ├── feature/bid-history-api
      ├── feature/member-service
      │
      ├── fix/auction-query-bug         ← 버그 수정
      │
      ├── chore/ci-develop-trigger      ← 설정/인프라
      │
      └── docs/redis-pattern-guide      ← 문서
```

### 브랜치 역할

| 브랜치 | 용도 | 병합 대상 | 보호 수준 |
|--------|------|-----------|-----------|
| `main` | 배포 가능 상태 유지 | ← develop | Approve 2명 + CI 통과 |
| `develop` | 통합 브랜치, 개발 기준점 | ← feature/* | Approve 1명 + CI 통과 |
| `feature/*` | 기능 개발 | → develop | 없음 (자유 push) |
| `fix/*` | 버그 수정 | → develop | 없음 |
| `chore/*` | 설정, 리팩토링 | → develop | 없음 |
| `docs/*` | 문서 작업 | → develop | 없음 |

### 브랜치 생명주기

```
생성               작업              PR 생성            머지 후 삭제
  │                 │                 │                  │
  ▼                 ▼                 ▼                  ▼
develop에서 분기 → 커밋 반복 → develop으로 PR → Squash Merge → 브랜치 삭제
```

---

## 2. 브랜치 네이밍 규칙

### 형식

```
{type}/{도메인}-{기능-설명}
```

### type 종류

| type | 용도 | 예시 |
|------|------|------|
| `feature` | 새 기능 개발 | `feature/auction-feed-api` |
| `fix` | 버그 수정 | `fix/bid-amount-validation` |
| `chore` | 설정, 빌드, 의존성 | `chore/ci-matrix-fix` |
| `docs` | 문서 작성/수정 | `docs/redis-pattern-guide` |
| `refactor` | 코드 리팩토링 | `refactor/auction-clean-arch` |
| `test` | 테스트 추가/수정 | `test/auction-service-unit` |

### 네이밍 규칙

```
좋은 예:
  feature/auction-feed-api
  fix/bid-concurrent-lock
  chore/auction-redis-config
  docs/auction-detail-design

나쁜 예:
  feature/work              ← 설명 부족
  my-branch                 ← type 없음
  feature/AuctionFeedAPI    ← 케밥케이스 아님
  feat/auction-feed-api     ← feat(X) → feature(O)
```

---

## 3. 커밋 메시지 컨벤션

### Conventional Commits 형식

```
<type>: <간결한 설명>
```

### type 종류

| type | 용도 | 예시 |
|------|------|------|
| `feat` | 새 기능 | `feat: 경매 피드 조회 API 구현` |
| `fix` | 버그 수정 | `fix: 입찰 금액 검증 누락 수정` |
| `refactor` | 리팩토링 (기능 변경 X) | `refactor: Auction 패키지 Clean Architecture 적용` |
| `chore` | 설정, 빌드, 의존성 | `chore: Redis 의존성 추가` |
| `docs` | 문서 | `docs: Redis 활용 패턴 문서 추가` |
| `test` | 테스트 | `test: AuctionService 단위 테스트 작성` |
| `style` | 포맷팅 (기능 변경 X) | `style: 코드 포맷팅 정리` |

### 좋은 커밋 vs 나쁜 커밋

```
좋은 예:
  feat: 경매 피드 조회 API 구현
  fix: 입찰 금액 음수 허용 버그 수정
  refactor: Auction RepositoryImpl → RepositoryAdapter 리네이밍
  docs: Clean Architecture 데이터흐름 가이드 작성
  test: BidService 입찰 내역 조회 테스트 5건 추가

나쁜 예:
  수정함                  ← 무엇을 수정했는지 불명확
  update                 ← 영어도 불명확
  fix bug                ← 어떤 버그인지 불명확
  작업중                  ← 미완성 커밋
  feat: 여러 가지 수정    ← 1 커밋에 여러 변경 혼합
```

### 커밋 분리 원칙

```
1 커밋 = 1 논리적 변경

좋은 예 (분리):
  git commit -m "feat: 경매 피드 조회 API 구현"
  git commit -m "test: AuctionController 테스트 작성"
  git commit -m "docs: Redis 활용 패턴 문서 추가"

나쁜 예 (혼합):
  git commit -m "feat: API 구현 + 테스트 + 문서"
```

---

## 4. 작업 플로우 (실전 커맨드)

### 4.1 브랜치 생성

```bash
# 1) develop 최신화
git checkout develop
git pull origin develop

# 2) 작업 브랜치 생성
git checkout -b feature/auction-feed-api
```

### 4.2 작업 및 커밋

```bash
# 변경 사항 확인
git status
git diff

# 파일 지정 스테이징 (git add . 금지)
git add auction/src/main/java/com/biddy/auction/auction/
git add auction/src/test/java/com/biddy/auction/auction/

# 스테이징 내용 최종 확인
git diff --staged

# 커밋
git commit -m "feat: 경매 피드 조회 API 구현"
```

### 4.3 추가 작업 시 커밋 분리

```bash
# 문서 작업
git add docs/99_패턴레퍼런스/07_Redis_활용_패턴.md
git commit -m "docs: Redis 활용 패턴 문서 추가"

# 테스트 추가
git add auction/src/test/
git commit -m "test: AuctionService 단위 테스트 7건 작성"
```

### 4.4 Push

```bash
# 첫 push (-u로 원격 브랜치 연결)
git push -u origin feature/auction-feed-api

# 이후 push
git push
```

### 4.5 PR 생성

```bash
# CLI (gh 설치 필요)
gh pr create --base develop \
  --title "feat: 경매 피드 조회 API 구현" \
  --body "$(cat <<'EOF'
## 변경 사항
- GET /api/v1/auctions 경매 피드 조회 API
- Clean Architecture 패키지 구조 적용
- 단위 테스트 12건 작성

## 테스트
- [x] 단위 테스트 통과
- [x] 로컬 동작 확인
EOF
)"
```

또는 push 후 터미널에 표시되는 GitHub 링크를 클릭하여 웹에서 PR 생성.

### 4.6 머지 후 정리

```bash
# PR 머지 후 로컬 정리
git checkout develop
git pull origin develop
git branch -d feature/auction-feed-api    # 로컬 브랜치 삭제
```

---

## 5. 코드 리뷰 규칙

### PR 규칙

| 규칙 | 이유 |
|------|------|
| **PR 크기 300줄 이하** | 큰 PR은 리뷰 품질 저하 |
| **1 PR = 1 기능** | 혼합 PR은 되돌리기 어려움 |
| **PR 설명 필수** | 맥락 없이 코드만 보면 비효율 |
| **24시간 내 리뷰** | 리뷰 지연 → 병합 충돌 증가 |
| **Approve / Request Changes 명확히** | 코멘트만 달면 진행 불가 |

### 리뷰 프로세스

```
1. feature 브랜치에서 작업
2. develop으로 PR 생성
3. CODEOWNERS 자동 할당 + 추가 리뷰어 지정
4. CI 통과 확인
5. 최소 1명 Approve
6. Squash Merge → develop
7. 배포 시점에 develop → main (2명 Approve)
```

### 리뷰 코멘트 접두사

```
[필수]    반드시 수정 필요 (Approve 불가)
[제안]    더 나은 방법 제안 (선택)
[질문]    이해가 안 되는 부분
[칭찬]    잘 작성된 코드

예시:
  [필수] SQL Injection 위험이 있습니다. PreparedStatement를 사용해주세요.
  [제안] 이 메서드는 stream()으로 간결하게 변환 가능합니다.
  [질문] 이 TTL 값의 근거가 있나요?
  [칭찬] 도메인 분리가 깔끔합니다!
```

---

## 6. CODEOWNERS 설정

파일 위치: `.github/CODEOWNERS`

```
# 도메인별 담당자 → PR 생성 시 자동 리뷰어 할당
/auction/           @auction-owner
/member/            @member-owner
/product/           @product-owner
/order/             @order-owner
/payment/           @payment-owner

# 공통 영역
/docs/              @all-members
docker-compose.yml  @infra-owner
.github/            @infra-owner
```

---

## 7. GitHub Branch Protection 설정

Settings > Branches > Add branch protection rule

### main 브랜치

```
✅ Require a pull request before merging
   ✅ Require approvals: 2
   ✅ Dismiss stale pull request approvals when new commits are pushed
✅ Require status checks to pass before merging
   ✅ Require branches to be up to date before merging
✅ Do not allow bypassing the above settings
❌ Allow force pushes
❌ Allow deletions
```

### develop 브랜치

```
✅ Require a pull request before merging
   ✅ Require approvals: 1
✅ Require status checks to pass before merging
❌ Allow force pushes
❌ Allow deletions
```

---

## 8. 주의사항

| 규칙 | 이유 |
|------|------|
| `git add .` 사용 금지 | `.env`, 다른 도메인 파일 혼입 방지 |
| `main`에 직접 push 금지 | 항상 develop → PR → 리뷰 → 머지 |
| push 전 `git diff --staged` 확인 | 의도치 않은 파일 포함 방지 |
| 머지된 브랜치는 즉시 삭제 | 브랜치 목록 깔끔하게 유지 |
| `--force push` 금지 | 다른 사람의 커밋 덮어쓰기 위험 |
| 커밋 전 `git status` 습관화 | 실수 방지의 가장 기본 |

---

## 9. 빠른 참조 (Cheat Sheet)

```bash
# === 새 기능 시작 ===
git checkout develop && git pull origin develop
git checkout -b feature/auction-{기능명}

# === 작업 중 ===
git status                           # 변경 확인
git add {파일/폴더 경로}              # 파일 지정 스테이징
git diff --staged                    # 스테이징 확인
git commit -m "feat: 설명"           # 커밋

# === 올리기 ===
git push -u origin feature/auction-{기능명}   # 첫 push
git push                                      # 이후 push

# === PR 생성 ===
gh pr create --base develop --title "feat: 설명"

# === 머지 후 정리 ===
git checkout develop && git pull origin develop
git branch -d feature/auction-{기능명}

# === 충돌 해결 ===
git checkout develop && git pull origin develop
git checkout feature/auction-{기능명}
git merge develop                    # 충돌 해결 후
git add . && git commit
git push
```
