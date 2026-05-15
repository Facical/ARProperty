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
| 백엔드 | Spring Boot 앱 부팅 가능 | `/health` + `GET /api/v1/buildings/nearby` 엔드포인트 동작. 그 외 `complexes`, `livability`, 거래 상세는 컨트롤러 스텁 상태 |
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

### 신규 PC에서 처음 셋업할 때

private 리포지토리 가정으로 `local.properties` / `backend/.env`가 git에 포함되어 있습니다. 따라서 clone 직후 별도 키 입력 없이 빌드/실행이 가능합니다.

```bash
git clone <repo> ARProperty
cd ARProperty
```

PC별로 다른 두 가지만 보강해주세요.

1. **Android SDK 경로** — `android/local.properties`에 `sdk.dir` 라인이 없습니다. Android Studio가 첫 sync 시 자동으로 채우거나, `ANDROID_HOME` 환경변수가 잡혀 있으면 그대로 사용합니다.
2. **카카오/Google ARCore 키해시** — `build.gradle.kts:30`의 카카오 native 키와 `local.properties`의 `GEOSPATIAL_API_KEY`는 각각 카카오/Google Cloud 콘솔에서 *현재 PC의 디버그 keystore SHA1*에 묶여 있습니다. 새 PC에서는 본인 SHA1을 콘솔에 추가해야 카카오맵 타일 / Geospatial 앵커가 동작합니다.
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android
   # SHA1 라인을 복사해서 카카오 developers 콘솔 → 앱 → 플랫폼 → Android 키해시에 추가
   # (카카오는 SHA1을 base64로 변환한 키해시를 요구)
   ```

### 지금 바로 가능한 것

```bash
# DB / Redis 실행
docker-compose up -d db redis

# 옥계동 데모 시드 데이터 적재 (선택, 멱등 SQL — 단지는 sync로 채워야 함)
docker exec -i $(docker-compose ps -q db) psql -U arproperty -d arproperty \
  < backend/scripts/seed_demo_okgye.sql

# 백엔드 부팅 (포트 8080)
cd backend
./gradlew bootRun

# 옥계동 실 단지 좌표 적재 (공공데이터 apt_list → Kakao Local 자동 조회)
# 필요 env: DATA_GO_KR_API_KEY, KAKAO_REST_API_KEY
./gradlew bootRun --args='--sync-okgye-complex'

# 옥계동 동(棟) 단위 폴리곤·centroid 적재 (VWorld 2D Data → 단지 매칭)
# 필요 env: VWORLD_API_KEY (반드시 sync-okgye-complex 이후 실행)
./gradlew bootRun --args='--sync-okgye-buildings'

# 주변 건물 API 확인
curl 'http://localhost:8080/api/v1/buildings/nearby?lat=36.13&lon=128.34&radius=500'

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

- 백엔드는 `/health` 와 `GET /api/v1/buildings/nearby`만 동작합니다. `buildings/{id}`, `buildings/{id}/trades`, `complexes`, `livability` 엔드포인트는 컨트롤러 스텁 상태입니다.
- Android AR 화면은 `nearby` API를 호출해 옥계동 주변 단지 마커를 동적으로 그리고 등급별 색상(녹/황/적)으로 표시하지만, 마커 직접 탭은 아직 없고 화면 하단 칩 목록에서 단지를 선택하는 방식입니다.
- DetailPopup의 거래 이력 다건/건물 상세는 `buildings/{id}` 엔드포인트가 추가될 때 함께 채워집니다.
- ARCore Geospatial API는 `local.properties`에 `GEOSPATIAL_API_KEY=...`를 추가했을 때만 자동 활성화됩니다. 키가 없으면 카메라/세션은 정상 동작하고 Geospatial만 비활성으로 표시됩니다.

## 시연용 백엔드 외부 노출

발표 환경에서 안드로이드 디바이스가 같은 LAN에 없거나, 사내망/모바일 데이터로 백엔드를 호출해야 할 때 사용합니다.

### 옵션 1: cloudflared (계정 불필요, 임시 URL)

```bash
cloudflared tunnel --url http://localhost:8080
# 출력: https://<random>.trycloudflare.com
```

### 옵션 2: ngrok (계정 필요, 안정적 URL)

```bash
ngrok http 8080
# 출력: https://<random>.ngrok-free.app
```

### 안드로이드 측 적용

두 가지 방식 중 편한 쪽을 선택합니다.

**(A) 재빌드해서 새 URL을 박는다**

`android/local.properties`의 `ARPROPERTY_BASE_URL`을 발급된 https URL로 덮어쓰고 디버그 빌드를 다시 만듭니다.

```properties
ARPROPERTY_BASE_URL=https://<random>.trycloudflare.com/
```

`/`로 끝나야 Retrofit이 정상 동작합니다.

**(B) 재빌드 없이 런타임에 URL만 바꾼다** (실기기 권장)

앱 실행 후 AR 화면 안의 **BaseUrl 다이얼로그**에서 URL을 입력합니다. `BaseUrlOverrideInterceptor`가 모든 Retrofit 요청을 즉시 새 호스트로 리라우트하므로 앱 재시작도 불필요합니다.

### 시연 직전 체크리스트

```bash
# 단지 좌표 sync 완료 확인 (N>0이어야 함)
docker exec -it $(docker-compose ps -q db) psql -U arproperty -d arproperty \
  -c "SELECT count(*) FROM apt_complex_master WHERE legal_dong_code='4719012800';"

# 외부 URL에서 nearby API 응답 확인
curl 'https://<your-tunnel>/api/v1/buildings/nearby?lat=36.13&lon=128.34&radius=1000'
```

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
