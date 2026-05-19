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
| Android 앱 | Compose 기반 실 UI 동작 | `ar`/`map`/`livability`/`building` 4개 feature 모듈, DetailPopup/PanelContent, 카카오맵, ARCore Geospatial 통합, BaseUrl 런타임 오버라이드 다이얼로그 포함. AR DetailPopup은 선택 건물 상세 API 일부와 최근 거래 API를 추가 조회 |
| 백엔드 | Spring Boot 앱 정상 가동 | 동작: `/health`, `GET /api/v1/buildings/nearby`, `GET /api/v1/buildings/{id}`, `GET /api/v1/buildings/{id}/trades`, `GET /api/v1/livability/infra/nearby`. 스텁: `ComplexController` (클래스만 존재) |
| 데이터 적재 | 옥계동 단지/건물 sync 검증 완료 | `--sync-okgye-complex` (단지 17개), `--sync-okgye-buildings` (동 99개) 완료. Python 보조 스크립트는 구현된 편의시설 경로와 placeholder가 섞여 있음 |
| 인프라 | `docker compose up -d` 한 줄로 db/redis/backend 전부 가동 | `backend/.env`가 git에 포함되어 있어 clone 직후 즉시 부팅 가능 |
| 문서 | 핵심 기준 문서 재정리 완료 | 과거 조사/초안 문서는 `docs/archive/`에 보존 |

## 현재 구현됨 vs 목표 범위

### 현재 구현됨

- PostgreSQL/PostGIS 초기 스키마: `backend/scripts/init_db.sql`
- Spring Boot 백엔드: `/health`, `buildings/nearby`, `buildings/{id}`, `buildings/{id}/trades`, `livability/infra/nearby` 동작
- 옥계동 데이터 적재 파이프라인: 공공데이터 K-apt → Kakao Local 좌표 → VWorld 동 폴리곤
- Docker Compose로 `db`, `redis`, `backend` 일괄 가동 (`backend/.env` 포함)
- Android Compose 앱: AR 카메라 + 카카오맵 + 동적 단지 마커(등급별 색상) + 단지 칩 선택 + 상세/거래 API 기반 건물 상세 화면 + 상세/거래 API 보강 DetailPopup + 런타임 BaseUrl 오버라이드
- 데이터 수집 Python 보조 스크립트 (`data/collectors/`): 일부는 구현됨, 일부는 `PLACEHOLDER`로 표시됨
- 프로젝트 기준 문서와 API 계약 문서

### 설계상 목표

- Android Kotlin 기반 AR 앱
- Spring Boot 기반 REST API
- 구미시 아파트 단지/동 단위 데이터 적재
- 실거래가, 건축 정보, 생활 인프라 점수 통합 조회

## 로컬 실행 기준

### 신규 PC에서 처음 셋업할 때

private 리포지토리 가정으로 `local.properties` / `backend/.env`가 git에 포함되어 있습니다. 따라서 clone 직후 별도 키 입력 없이 빌드/실행이 가능합니다.

### API 키 정책

이 저장소는 **private 팀/학교 데모용 리포지토리**라는 전제로 shared development credential을 커밋합니다. 이는 신규 PC 셋업 시간을 줄이기 위한 의도적인 예외이며, 운영/상용 배포용 키가 아닙니다.

- `backend/.env`: 공공데이터포털, VWorld, Kakao REST API 키를 포함합니다. 서버 사이드 수집/sync 작업과 Docker 실행에 사용합니다.
- `android/local.properties`: ARProperty base URL과 Google ARCore Geospatial API 키를 포함합니다. Geospatial 키는 패키지명과 디버그 keystore SHA-1 제한을 전제로 사용합니다.
- `android/app/build.gradle.kts`: Kakao Native 앱 키 fallback을 포함합니다. 카카오 콘솔의 Android 플랫폼 설정(패키지명/키해시)에 묶인 데모 키입니다.
- 리포지토리는 private 상태를 유지합니다. 외부 공유, public 전환, 발표/시연 기간 종료 후에는 shared key를 rotate 또는 revoke합니다.
- 새 운영용 키, 개인 결제 계정 키, 외부 공개 가능한 키는 커밋하지 않습니다. 그런 키가 필요하면 별도 환경변수나 로컬 override로 관리합니다.

```bash
git clone <repo> ARProperty
cd ARProperty

# 신규 PC 또는 Codex 세션에서 먼저 빠른 환경 점검
./scripts/codex-doctor.sh
```

PC별로 다른 두 가지만 보강해주세요.

1. **Android SDK 경로** — `android/local.properties`에 `sdk.dir` 라인이 없습니다. Android Studio가 첫 sync 시 자동으로 채우거나, `ANDROID_HOME` 환경변수가 잡혀 있으면 그대로 사용합니다.
2. **카카오/Google ARCore 키해시** — `build.gradle.kts:30`의 카카오 native 키와 `local.properties`의 `GEOSPATIAL_API_KEY`는 각각 카카오/Google Cloud 콘솔에서 *현재 PC의 디버그 keystore SHA1*에 묶여 있습니다. 새 PC에서는 본인 SHA1을 콘솔에 추가해야 카카오맵 타일 / Geospatial 앵커가 동작합니다.
   **(1) 패키지 이름** — 고정값. `android/app/build.gradle.kts`의 `applicationId`로 확인 가능 (모든 팀원 동일).

   ```bash
   # macOS / Linux
   grep applicationId android/app/build.gradle.kts
   # → applicationId = "com.arproperty.android"
   ```
   ```powershell
   # Windows (PowerShell)
   Select-String applicationId android\app\build.gradle.kts
   ```

   **(2) 디지털 지문(SHA-1)** — 본인 PC의 디버그 keystore에서 추출. PC마다 값이 다름.

   ```bash
   # macOS / Linux
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android | grep SHA1
   # → SHA1: BB:AC:B7:4F:E8:0E:...
   ```
   ```powershell
   # Windows (PowerShell)
   keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" `
     -alias androiddebugkey -storepass android -keypass android | Select-String SHA1
   # keytool이 PATH에 없으면 Android Studio 번들 JDK를 직접 호출:
   # & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String SHA1
   ```

   **(3) 등록할 위치** — 위 두 값(패키지명 + SHA1)을 다음 두 콘솔에 추가:
   - 카카오 developers 콘솔 → 앱 → 플랫폼 → Android 키해시 (카카오는 SHA1을 base64로 변환한 키해시를 요구)
   - Google Cloud 콘솔 → API 및 서비스 → 사용자 인증 정보 → 해당 API 키 → "애플리케이션 제한사항(Android 앱)" → +Add
     (미등록 시 ARCore Geospatial 앵커 비활성. 적용까지 최대 5분 소요)

신규 PC의 첫 Gradle/Docker 실행은 Gradle 배포본과 Maven 의존성을 내려받느라 느릴 수 있습니다. 저장소의 Gradle wrapper 네트워크 제한시간은 60초로 늘려두었고, 백엔드 Docker 빌드는 의존성 계층을 먼저 캐시하도록 구성되어 있습니다.

### 지금 바로 가능한 것

```bash
# DB + Redis + 백엔드 한 번에 가동 (포트 8080)
docker compose up -d
# 구버전 Docker 환경이면: docker-compose up -d

# (선택) 옥계동 데모 시드 SQL — 단지 좌표는 아래 sync로 채우는 게 우선
docker exec -i $(docker compose ps -q db) psql -U arproperty -d arproperty \
  < backend/scripts/seed_demo_okgye.sql

# 옥계동 실 단지 좌표 적재 (공공데이터 apt_list → Kakao Local 자동 조회)
# 필요 env: DATA_GO_KR_API_KEY, KAKAO_REST_API_KEY (이미 backend/.env에 포함)
docker run --rm --network arproperty_default --env-file backend/.env \
  -e DB_HOST=db -e DB_PORT=5432 -e DB_NAME=arproperty \
  -e DB_USERNAME=arproperty -e DB_PASSWORD=devpassword \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 \
  arproperty-backend java -jar app.jar \
  --sync-okgye-complex --spring.main.web-application-type=none

# 옥계동 동(棟) 단위 폴리곤·centroid 적재 (VWorld 2D Data → 단지 매칭)
# 필요 env: VWORLD_API_KEY (반드시 sync-okgye-complex 이후 실행)
docker run --rm --network arproperty_default --env-file backend/.env \
  -e DB_HOST=db -e DB_PORT=5432 -e DB_NAME=arproperty \
  -e DB_USERNAME=arproperty -e DB_PASSWORD=devpassword \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 \
  arproperty-backend java -jar app.jar \
  --sync-okgye-buildings --spring.main.web-application-type=none

# 주변 건물 API 확인 (옥계동 좌표)
curl 'http://localhost:8080/api/v1/buildings/nearby?lat=36.139&lon=128.432&radius=1000'

# 도커 없이 호스트에서 백엔드 띄우려면 (포트 8080 충돌 시 위 docker compose 중지 필요)
cd backend && ./gradlew bootRun --no-daemon

# Android 앱 디버그 빌드 + 실기기 설치
cd android
ANDROID_HOME=~/Library/Android/sdk ./gradlew :app:installDebug --no-daemon   # macOS 예시

# Python 보조 스크립트용 가상환경 준비
# 주의: `PLACEHOLDER` 표시가 있는 스크립트는 아직 실행 가능한 수집/적재 로직이 없음
cd data/collectors
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 아직 불가능하거나 미완성인 것

- 백엔드 컨트롤러 중 `ComplexController`는 클래스만 있고 매핑이 비어 있습니다. `complexes/*` 엔드포인트는 아직 없습니다.
- Android AR 화면은 `nearby` API로 옥계동 주변 단지 마커를 동적으로 그리고 등급별 색상(녹/황/적)으로 표시하지만, 마커 직접 탭은 아직 없고 화면 하단 칩 목록에서 단지를 선택하는 방식입니다.
- Android 건물 상세 화면은 `buildings/{id}`와 `buildings/{id}/trades` API에 연결됐습니다. AR DetailPopup도 선택 건물의 상세 API와 거래 API를 보강 조회해 층수, 높이, 구조, 세대수, 최근 거래 최대 3건, 가격 추세 그래프를 표시합니다.
- ARCore Geospatial API는 `local.properties`의 `GEOSPATIAL_API_KEY`가 채워지고 *현재 PC의 디버그 keystore SHA1*이 GCP 콘솔에 등록돼 있을 때 실제 지리 앵커를 생성합니다. 키가 없거나 Earth tracking 전이면 기기 위치 또는 옥계 기본 좌표로 `nearby`를 먼저 불러오고, 카메라 화면에 fallback 태그를 표시합니다.

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

전체 옥계동 AR 현장 QA 기준은 [docs/okgye-ar-qa.md](docs/okgye-ar-qa.md)를 우선합니다.

```bash
# 단지 좌표 sync 완료 확인 (N>0이어야 함)
docker exec -it $(docker compose ps -q db) psql -U arproperty -d arproperty \
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
