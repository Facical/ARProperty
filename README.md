# ARProperty - 구미시 AR 부동산 탐색 앱

스마트폰 카메라로 아파트를 비추면 실거래가, 건축 정보, 생활 편의시설 점수가 AR로 표시되는 현장형 부동산 앱입니다.

---

## 프로젝트 구조

```
ARProperty/
├── android/          # 프론트엔드 (Android Kotlin)
├── backend/          # 백엔드 (FastAPI + PostgreSQL)
├── data/             # 데이터 수집 스크립트
├── docs/             # 프로젝트 문서
└── docker-compose.yml
```

---

## 프론트엔드 (Android Kotlin)

| 항목 | 내용 |
|------|------|
| 언어 | Kotlin |
| IDE | Android Studio |
| AR | ARCore Geospatial API, Streetscape Geometry |
| 지도 | Google Maps SDK |
| OCR | ML Kit (동번호 인식) |
| API 통신 | Retrofit |

### 주요 화면

- **AR 뷰**: 카메라로 건물을 비추면 단지명, 가격, 편의시설 등급 태그 표시
- **지도 뷰**: Google Maps에 건물 마커 + 편의시설 레이어
- **상세 카드**: 건축 정보, 거래 이력, 층별 보기
- **편의시설 점수**: 6개 카테고리별 점수 + 등급(S~F) + 건물 비교

### 프론트엔드 담당

- **R2 강형준**: AR 핵심 (ARCore, 건물 인식 알고리즘, 기술 가이드)
- **R4 권희성**: 앱 화면 (상세 카드, 층별 보기, 필터, 가격 그래프)
- **R5 나웅주**: 지도/점수 (지도 뷰, 편의시설 화면, 비교표, 디자인)

---

## 백엔드 (FastAPI + PostgreSQL)

| 항목 | 내용 |
|------|------|
| 프레임워크 | FastAPI (Python 3.11+) |
| DB | PostgreSQL 16 + PostGIS 3.4 |
| 캐시 | Redis 7 |
| 컨테이너 | Docker + docker-compose |

### 백엔드 실행

```bash
# 1. 환경변수 설정
cp backend/.env.example backend/.env
# .env에 실제 API 키 입력

# 2. Docker로 실행
docker-compose up -d

# 3. 확인
# API: http://localhost:8000/health
# Swagger UI: http://localhost:8000/docs
```

### 주요 API

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /api/v1/buildings/nearby` | 주변 건물 목록 (반경 검색) |
| `GET /api/v1/buildings/{id}/trades` | 건물 거래 이력 |
| `GET /api/v1/complexes` | 단지 목록 |
| `GET /api/v1/livability/{building_id}` | 편의시설 점수 |
| `GET /api/v1/livability/compare` | 건물 비교 |

상세 명세: [docs/api-spec.md](docs/api-spec.md)

### 외부 API 연동 (8종)

- **data.go.kr**: 실거래가, 전월세, 건축물대장, 공동주택 목록, 공동주택 기본정보
- **vworld.kr**: 지오코딩, 2D 건물 폴리곤
- **Kakao**: Local API (편의시설 카테고리 검색)

### 백엔드 담당

- **R3 엄태원**: FastAPI 서버, 공공데이터 7종 연동, DB 설계, 편의시설 점수화 엔진

---

## GitHub 협업 방법

### 초기 설정 (최초 1회)

```bash
# 1. 레포지토리 클론
git clone https://github.com/Facical/ARProperty.git
cd ARProperty

# 2. develop 브랜치로 이동
git checkout develop
```

### 브랜치 구조

```
main         ← 발표/시연 가능한 안정 상태 (직접 push 금지)
  └── develop    ← 개발 통합 브랜치 (PR로만 머지)
        ├── feature/frontend/...   (프론트엔드)
        ├── feature/backend/...    (백엔드)
        └── feature/data/...       (데이터 수집)
```

- `main`: Phase 완료 시에만 develop에서 머지. 항상 동작하는 상태 유지
- `develop`: 모든 작업은 여기로 PR을 보냄
- `feature/*`: 각자 작업하는 브랜치

### 브랜치 네이밍 규칙

```
feature/{영역}/{기능명}

영역: frontend, backend, data, docs
```

| 역할 | 예시 |
|------|------|
| R1 여창훈 (데이터) | `feature/data/gumi-apt-collector` |
| R2 강형준 (AR) | `feature/frontend/arcore-geospatial` |
| R3 엄태원 (백엔드) | `feature/backend/trade-api` |
| R4 권희성 (상세UI) | `feature/frontend/building-detail-card` |
| R5 나웅주 (지도) | `feature/frontend/map-view` |

### 작업 흐름 (매번 반복)

```bash
# 1. develop에서 최신 코드 가져오기
git checkout develop
git pull origin develop

# 2. 새 브랜치 생성
git checkout -b feature/backend/trade-api

# 3. 코드 작성 후 커밋
git add 변경한파일들
git commit -m "feat(backend): 실거래가 API 연동 모듈 추가"

# 4. 원격에 push
git push origin feature/backend/trade-api

# 5. GitHub에서 PR(Pull Request) 생성
#    - base: develop  ←  compare: feature/backend/trade-api
#    - 리뷰어 지정
#    - 리뷰 승인 후 Merge
```

### 커밋 메시지 규칙

```
<타입>(<영역>): <요약>

타입:
  feat     새 기능 추가
  fix      버그 수정
  docs     문서 변경
  style    코드 포맷팅 (동작 변경 없음)
  refactor 리팩토링
  test     테스트 추가/수정
  chore    빌드, 설정 변경
```

**예시**:
```
feat(backend): 실거래가 API 연동 모듈 추가
fix(frontend): AR 태그 위치 오프셋 보정
docs(docs): API 명세서 v2 업데이트
chore(infra): docker-compose Redis 추가
```

### PR(Pull Request) 규칙

1. 모든 코드는 **PR을 통해서만** `develop`에 머지
2. **리뷰어 지정**:
   - 프론트엔드 PR → R2(강형준) 필수 리뷰
   - 백엔드 PR → R2(강형준) 또는 R1(여창훈) 리뷰
3. 최소 **1명 승인** 후 머지
4. `.env`나 API 키가 포함되지 않았는지 확인

### PR 생성 방법 (GitHub 웹)

1. GitHub에서 **"Compare & pull request"** 버튼 클릭 (push 직후 뜸)
2. base를 `develop`으로 설정
3. 제목과 설명 작성 (PR 템플릿이 자동으로 뜸)
4. 오른쪽에서 **Reviewers** 지정
5. **"Create pull request"** 클릭
6. 리뷰어가 코드 확인 후 **"Approve"** → **"Merge pull request"**

### 충돌(Conflict) 발생 시

```bash
# 1. develop 최신 코드를 내 브랜치에 합치기
git checkout feature/backend/trade-api
git pull origin develop

# 2. 충돌 파일 수정 (<<<< ==== >>>> 부분을 정리)

# 3. 수정 후 커밋
git add .
git commit -m "fix(backend): develop 머지 충돌 해결"
git push origin feature/backend/trade-api
```

### 자주 쓰는 Git 명령어 요약

| 명령어 | 설명 |
|--------|------|
| `git clone <url>` | 레포 복제 |
| `git checkout <브랜치>` | 브랜치 이동 |
| `git checkout -b <브랜치>` | 새 브랜치 생성 + 이동 |
| `git pull origin <브랜치>` | 원격 최신 코드 가져오기 |
| `git add <파일>` | 변경 파일 스테이징 |
| `git commit -m "메시지"` | 커밋 |
| `git push origin <브랜치>` | 원격에 push |
| `git status` | 현재 상태 확인 |
| `git log --oneline` | 커밋 이력 확인 |
| `git branch` | 브랜치 목록 확인 |
| `git stash` | 작업 중인 변경 임시 저장 |
| `git stash pop` | 임시 저장한 변경 복원 |

---

## 팀 구성

| 역할 | 담당자 | 영역 |
|------|--------|------|
| R1 | 여창훈 | PM / 데이터 수집 / QA |
| R2 | 강형준 | 기술리드 / AR 핵심 개발 |
| R3 | 엄태원 | 백엔드 (API + DB + 점수화) |
| R4 | 권희성 | 프론트엔드 - 앱 화면 / 상세 UI |
| R5 | 나웅주 | 프론트엔드 - 지도 / 편의시설 화면 |
