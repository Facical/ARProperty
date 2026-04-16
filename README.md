# ARProperty

구미시 아파트 현장 탐색을 위한 AR 기반 부동산 프로젝트입니다. 스마트폰 카메라로 건물을 비추면 단지 정보, 실거래가, 생활 인프라 점수를 한 화면에서 확인하는 것을 목표로 합니다.

## 시작점

- 프로젝트 기준 문서: [docs/project-guide.md](docs/project-guide.md)
- API 계약 문서: [docs/api-spec.md](docs/api-spec.md)
- 역할 운영 문서: [역할_분담표.md](역할_분담표.md)
- 보존 문서 안내: [docs/archive/README.md](docs/archive/README.md)

## 현재 저장소 상태

| 영역 | 현재 상태 | 비고 |
|------|-----------|------|
| Android 앱 | Android Studio Compose scaffold 존재 | Navigation, placeholder 화면, 네트워크/AR/Map 연결용 뼈대 포함 |
| 백엔드 | Gradle 설정, Dockerfile, 환경설정, DB 스키마 존재 | `src/main/java` 애플리케이션 소스는 아직 없음 |
| 데이터 수집 | Python 수집 스크립트와 법정동 매핑 존재 | 구미시 대상 수집 뼈대가 준비된 상태 |
| 문서 | 핵심 기준 문서 재정리 완료 | 과거 조사/초안 문서는 `docs/archive/`에 보존 |

## 현재 구현됨 vs 목표 범위

### 현재 구현됨

- PostgreSQL/PostGIS 초기 스키마: `backend/scripts/init_db.sql`
- Spring Boot 빌드 설정과 환경변수 예시
- Docker Compose로 `db`, `redis` 실행 설정
- 데이터 수집 스크립트 초안
- Android Studio Compose 프로젝트 뼈대
- 프로젝트 기준 문서와 API 계약 문서

### 설계상 목표

- Android Kotlin 기반 AR 앱
- Spring Boot 기반 REST API
- 구미시 아파트 단지/동 단위 데이터 적재
- 실거래가, 건축 정보, 생활 인프라 점수 통합 조회

## 로컬 실행 기준

### 지금 바로 가능한 것

```bash
# DB / Redis 실행
docker-compose up -d db redis

# Android 앱 디버그 빌드
cd android
./gradlew :app:assembleDebug

# Python 수집 스크립트용 가상환경 준비
cd data/collectors
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 아직 불가능하거나 미완성인 것

- `backend/`에 Spring Boot 애플리케이션 소스가 아직 없어서 `./gradlew bootRun`은 현재 기준으로 실행 대상이 아닙니다.
- `docker-compose up -d`로 전체 스택을 올리는 흐름도 백엔드 소스가 커밋되기 전에는 완료되지 않습니다.
- Android 앱은 skeleton 수준이라 실제 ARCore 세션, 실데이터 상세 UI, 점수 비교 화면은 아직 placeholder입니다.

## 협업 기준

현재 저장소에는 `main` 브랜치만 존재합니다. 문서 기준도 이에 맞춰 통일합니다.

- 기본 베이스 브랜치: `main`
- 작업 브랜치: `feature/{영역}/{기능명}`
- 권장 영역: `frontend`, `backend`, `data`, `docs`, `infra`
- 머지 방식: `main`으로 PR 생성 후 리뷰를 거쳐 머지

예시:

```bash
git checkout main
git pull origin main
git checkout -b feature/backend/trade-api
```

브랜치 정책이 나중에 바뀌면, 관련 문서는 한 번의 PR에서 함께 갱신합니다. 한 문서만 먼저 바꾸는 식의 부분 전환은 금지합니다.

## 저장소 구조

```text
ARProperty/
├── android/                 # Android Studio Compose scaffold
├── backend/                 # Spring Boot 설정, Dockerfile, DB 스키마
├── data/                    # 데이터 수집 스크립트와 매핑 파일
├── docs/
│   ├── project-guide.md     # 프로젝트 기준 문서
│   ├── api-spec.md          # 프론트-백엔드 API 계약
│   └── archive/             # 과거 조사/초안 문서 보존
├── CLAUDE.md                # 로컬 도구용 메타 문서
└── 역할_분담표.md            # 팀 운영 문서
```

## 문서 사용 원칙

- 현재 팀 기준은 `README.md`와 `docs/project-guide.md`를 먼저 봅니다.
- API 필드와 경로는 반드시 `docs/api-spec.md`를 기준으로 맞춥니다.
- 과거 조사 내용은 `docs/archive/`에 남기되, 현재 구현 기준으로 직접 사용하지 않습니다.
- 구현 상태가 바뀌면 README의 `현재 저장소 상태`부터 갱신합니다.
