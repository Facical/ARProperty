# ARProperty 중간 점검 발표자료 참고 문서

> 본 문서는 PPT(10~15장) 제작 시 옆에 띄워두고 참고하는 **원천 자료**입니다.
> 최초 작성은 main 브랜치의 코드/문서 상태를 기준으로 했습니다.
> 기준 일자: 2026-05-08 / 현재 보정: 2026-05-19 `upgrade`
> 보정 메모: Redis 컨테이너와 `api_call_log` 테이블은 존재하지만, 실제 서비스 캐시/호출량 카운트 로직은 아직 연결되지 않았습니다.

---

## 0. 한눈에 보는 요약

| 영역 | 진행률 | 핵심 담당 | 상태 키워드 |
|---|---|---|---|
| Android | **약 65%** | R2 강형준 / R4 권희성 / R5 나웅주 | AR·지도 동작, 데이터 미연동 |
| Backend | **약 50%** | R3 엄태원 | DB·설정 완성, API 로직 미작성 |
| Data | **약 30%** | R1 여창훈 | 매핑 완료, 수집 스크립트 미구현 |
| Docs·설계 | **약 95%** | 전원 (총괄 R3·R2) | 기준 문서 v1.1 확정 |
| **전체 평균** | **약 60%** | — | 인프라·설계 안정 / 실연동 단계 진입 직전 |

> 가중 평균 산식: `Android·Backend 각 35% + Data·Docs 각 15%` 의 가중치를 가정

---

## 1. 슬라이드 매핑 (총 13장 권장)

각 섹션은 1슬라이드에 매핑됩니다. 📸 마크는 **실행 화면/터미널 캡처를 끼워 넣을 위치**입니다.

### 슬라이드 1 — 표지

- 프로젝트명: **ARProperty**
- 부제: 구미시 아파트 AR 기반 부동산 탐색 서비스
- 발표 일자 / 팀원 5명 (R1~R5) 명시
- 📸 **(선택)** 카카오맵에 현재 위치 마커가 찍힌 화면을 흐리게 깔아 배경으로 사용

---

### 슬라이드 2 — 프로젝트 개요

- 한 줄 요약: "구미 아파트 단지/동을 ARCore Geospatial로 식별 → 실거래가·생활 인프라 점수를 즉시 조회"
- 타깃 지역: 구미시(`sigungu_cd 47190`), 1차 검증지 옥계동(`4719012800`)
- 좌표 범위: 위도 36.05~36.25 / 경도 128.20~128.50
- 팀 구성표

| ID | 이름 | 1차 책임 |
|---|---|---|
| R1 | 여창훈 | PM / 데이터 수집 / QA |
| R2 | 강형준 | 기술 리드 / AR 핵심 흐름 |
| R3 | 엄태원 | 백엔드 통합 / DB / API |
| R4 | 권희성 | 프론트 - 상세 UI |
| R5 | 나웅주 | 프론트 - 지도 / 생활 인프라 |

---

### 슬라이드 3 — 전체 진행률 대시보드

- **막대그래프 4개**: Android 65% / Backend 50% / Data 30% / Docs 95%
- 중앙에 **"전체 약 60%"** 강조
- 한 줄 메시지: "인프라·설계는 안정 궤도, 실데이터 연동·수집이 다음 마일스톤"
- 📸 불필요 (그래프 위주 슬라이드)

---

### 슬라이드 4 — 기술 스택 & 아키텍처

- **Frontend**: Android Kotlin + Jetpack Compose + ARCore 1.53 + ArSceneView 2.3 + 카카오맵 SDK 2.13
- **Backend**: Spring Boot 3.3.5 / Java 17 / Spring Data JPA / Spring Data Redis 의존성
- **DB·캐시**: PostgreSQL 16 + PostGIS 3.4 / Redis 7 컨테이너 (서비스 캐시 로직은 예정)
- **Data 수집**: Python 3 + requests + pandas
- **외부 API**: 공공데이터포털(실거래가/건축물대장) / VWorld(건물 폴리곤·지오코딩) / Kakao Local
- 📸 **`docs/backend-architecture.md` 의 Mermaid 다이어그램**을 마크다운 미리보기에서 캡처 → 슬라이드에 삽입

---

### 슬라이드 5 — Android 파트 ① AR 화면

- **담당**: R2 강형준
- **파트 진행률**: AR 카메라/Geospatial 데모 **80%**
- **구현 완료**
  - ARCore 세션 초기화 + AUTO 포커스 (`ArRoute.kt:161`)
  - Geospatial API: 위도/경도/고도 실시간 표시 (`ArRoute.kt:189-192`)
  - 옥계동 좌표(36.13, 128.34)에 Geospatial Anchor + 빨간 큐브 1×1×1 렌더링 (`ArRoute.kt:195-207`)
  - 추적 상태(Tracking / Geospatial / Anchor) 디버그 패널 (`ArRoute.kt:220-247`)
  - 카메라 + 위치 권한 처리 + ARCore 미지원 단말 분기 (`ArRoute.kt:79-94`)
- **미구현**
  - API 기반 동적 건물 마커 (현재는 데모 큐브 1개만)
  - AR 객체 탭/상호작용
- 📸 **필수 캡처 #1**: 앱 실행 → AR 탭 → 옥계동 데모 큐브가 카메라뷰에 뜬 화면 + 우측 상단 디버그 텍스트(위경도, Tracking 상태)

---

### 슬라이드 6 — Android 파트 ② 지도 & 상세 UI

- **담당**: R5 나웅주(지도) / R4 권희성(상세 UI) / R2 강형준(네비게이션)
- **파트 진행률**: 카카오맵 **70%** / 상세화면 UI **30%** / 네비게이션·테마 **90%** / 네트워크 계층 **80%**
- **구현 완료**
  - 카카오맵 렌더링 + 현재 위치 마커(`current_location_marker.png`) (`MapRoute.kt:88-140`)
  - 초기 중심 좌표 구미시(36.1195, 128.3445) (`MapRoute.kt:62`)
  - AR 화면 BottomSheet 패널: 건물명·최근 거래가·전용면적·버튼 (`PanelContent.kt`)
  - 건물 상세 팝업 UI: 건축물대장·가격 추세 그래프·거래이력 리스트 (`DetailPopupContent.kt`)
  - 5개 라우트 + BottomBar(AR/Map) 네비게이션 (`AppNavHost.kt`)
  - Retrofit 3.0 + Kotlinx Serialization + Repository 4종 정의 (`ApiServices.kt`, `AppContainer.kt`)
- **미구현**
  - 지도 위 건물 마커 / 편의시설 레이어
  - 필터칩(의료/교통/편의/안전) 동작 — 현재 `onClick` 빈 함수
  - 건물·단지·생활점수 화면 → 모두 `PlaceholderCard` 골격뿐
  - ViewModel ↔ Repository 연결 (Retrofit 정의는 있으나 호출 안 됨)
- 📸 **필수 캡처 #2**: 지도 탭 → 현재 위치 파란 마커
- 📸 **필수 캡처 #3**: AR 화면 하단 BottomSheet 패널
- 📸 **필수 캡처 #4**: 건물 상세 팝업(거래이력 리스트 + 가격 추세 그래프)

---

### 슬라이드 7 — Backend 파트 ① 인프라·DB

- **담당**: R3 엄태원
- **파트 진행률**: 빌드/설정/Docker **100%** / DB 스키마 **100%** / 엔티티·DTO·Repository **70%**
- **DB 7개 테이블** (`backend/scripts/init_db.sql`)

| 테이블 | 용도 |
|---|---|
| `apt_complex_master` | 아파트 단지 마스터 (centroid Point) |
| `apt_building_master` | 아파트 동 마스터 (polygon_geom Polygon) |
| `apt_trade_history` | 매매/전세/월세 거래 이력 |
| `living_infra_gumi` | 생활 인프라 POI (의료·교통·편의·안전 등 6종) |
| `building_livability_score` | 건물별 점수 캐시용 테이블 (계산 로직 연결 예정) |
| `bjdong_code_mapping` | 법정동코드 보조 매핑 |
| `api_call_log` | 외부 API 호출 한도 관리용 테이블 (카운트 로직 미작성) |

- PostGIS 확장 + GIST 공간 인덱스 적용
- 구미시 법정동 24건 초기 데이터 INSERT
- 📸 **필수 캡처 #5**: `docker compose up -d db redis` 실행 후 `psql` 접속 → `\dt` 결과 (테이블 7개 + 매핑 테이블이 존재함을 증빙. Redis 연동 로직 시연은 제외)

---

### 슬라이드 8 — Backend 파트 ② API·외부 연동

- **API 엔드포인트** 중 `/health`, `buildings/nearby`, `buildings/{id}`, `buildings/{id}/trades`, `livability/infra/nearby`는 구현됨. 단지 계열은 아직 스텁 또는 미구현
- **외부 API 클라이언트 9개** 중 2개 구현 → **약 30%**

| 엔드포인트 | 상태 |
|---|---|
| `GET /health` | ✅ 구현 (서비스명/버전 응답. DB·Redis 헬스 체크는 미포함) |
| `GET /api/v1/buildings/nearby` | ✅ 구현 |
| `GET /api/v1/buildings/{id}` | ✅ 구현 |
| `GET /api/v1/buildings/{id}/trades` | ✅ 구현 |
| `GET /api/v1/complexes` / `/{id}` / `/{id}/buildings` / `/{id}/trades` | 📋 스켈레톤 |
| `GET /api/v1/livability/{id}` / `/compare` | 📋 스켈레톤 (Phase 4) |
| `GET /api/v1/livability/infra/nearby` | ✅ 구현 (radius clamp, page/page_size, total_count) |

| 외부 클라이언트 | 상태 |
|---|---|
| `TradeApiClient` (공공데이터 실거래가) | ✅ XML 파싱 동작 |
| `BuildingDataClient` (VWorld 건물 폴리곤) | ✅ GeoJSON 파싱 동작 |
| `RentApiClient` / `BuildingRegisterApiClient` / `AptListApiClient` / `AptInfoApiClient` / `BaseDataGoKrClient` | 📋 스켈레톤 |
| `GeocoderClient` (VWorld) / `LocalApiClient` (Kakao) | 📋 스켈레톤 |

- 📸 **필수 캡처 #6**: 터미널에서 `curl http://localhost:8080/health` 응답 JSON (서버 동작 증빙)

---

### 슬라이드 9 — Data 파트

- **담당**: R1 여창훈
- **파트 진행률**: 의존성/매핑 **100%** / 구현된 편의시설 보조 스크립트와 placeholder 수집 스크립트 혼재
- **준비 완료**
  - `requirements.txt`: requests 2.32 / pandas 2.2 / openpyxl 3.1 / python-dotenv 1.0
  - `data/mapping/gumi_bjdong_codes.csv`: 구미시 법정동 30개 매핑
  - 디렉토리 구조: `collectors/` `mapping/` `raw/` `processed/`
- **placeholder 수집 스크립트 4종 (`PLACEHOLDER` 표시, 실행 로직 없음)**

| 스크립트 | 목적 |
|---|---|
| `collect_apt_list.py` | 구미시 공동주택 단지 목록 → CSV |
| `collect_trade_history.py` | 구미 아파트 매매·전월세 실거래가 → CSV |
| `collect_building_register.py` | 구미 건축물대장 표제부 → CSV |
| `collect_gumi_infra.py` | 생활 인프라(버스정거장·CCTV·어린이집 등) → CSV |

- 📸 **필수 캡처 #7**: `gumi_bjdong_codes.csv` 앞 10행을 VSCode/엑셀에서 캡처 (매핑 데이터 실재 증빙)

---

### 슬라이드 10 — Docs·설계 파트

- **담당**: 전원 (총괄 R3·R2)
- **파트 진행률**: **약 95%**
- 핵심 문서 목록

| 문서 | 역할 |
|---|---|
| `README.md` | 프로젝트 시작점 + 현재 상태 |
| `docs/project-guide.md` | 범위·기술 결정·리스크·Phase 1~5 |
| `docs/api-spec.md` (v1.1) | 엔드포인트 계약·필드 규칙 |
| `docs/backend-architecture.md` | Mermaid 다이어그램 4종 + 계층 설명 |
| `역할_분담표.md` | 5명 책임/handoff 정의 |

- 📸 **필수 캡처 #8**: `docs/api-spec.md` 의 엔드포인트 표 부분 (계약이 명확히 잡혀 있음을 보여주기)

---

### 슬라이드 11 — 통합 데모 시나리오

- **현재 시연 가능한 흐름**
  1. 앱 실행 → 카메라/위치 권한 허용
  2. AR 탭 → ARCore 세션 시작 → 옥계동 좌표 큐브 표시
  3. 우측 상단 디버그 정보로 Geospatial 위경도/Tracking 상태 확인
  4. 하단 BottomBar → 지도 탭 이동
  5. 카카오맵 + 현재 위치 마커 확인
  6. 지도 위 버튼으로 건물 상세 / 생활 점수 화면 진입 (UI 골격만)
- **백엔드 데모(선택)**: `/health` 응답으로 서비스명·버전 확인
- 📸 **필수 캡처 #9**: AR 화면 ↔ 지도 화면 두 장면을 좌우/상하로 나란히 배치 (Before/After 스타일)

---

### 슬라이드 12 — 미구현 / 리스크 / 차단 이슈

- **공통 차단 이슈**
  - 단지 계열 Backend API는 아직 스텁 또는 미구현
  - Data placeholder 수집 스크립트는 아직 실행 가능한 본문 없음
  - Redis 캐시/API 호출량 카운트는 인프라와 테이블만 있고 서비스 로직 미연결
- **기능 미구현**
  - AR ↔ 지도 선택 동기 세부 UX 보강
  - 지도 건물 마커 / 편의시설 필터칩 동작
  - 건물 상세·단지 상세·생활점수 화면 데이터 표시
  - 면적별/거래유형별 가격 그래프 고도화
- **리스크**
  - ARCore Geospatial API 키 의존 → 키 없을 시 자동 폴백 동작은 확인됨
  - 공공데이터 일일 호출 한도 → `api_call_log` 테이블로 관리 예정 (로직 미작성)
- 📸 불필요 (텍스트 위주)

---

### 슬라이드 13 — 다음 단계 (우선순위)

| 순위 | 작업 | 담당 | 효과 |
|---|---|---|---|
| 1 | `GET /api/v1/buildings/nearby` 컨트롤러·서비스 구현 | R3 | 가장 자주 쓰이는 1차 진입 API |
| 2 | 수집 스크립트 4종 본문 작성 → 옥계동 데이터 적재 | R1 | DB에 시연용 실데이터 확보 |
| 3 | Android ViewModel에 Repository 주입 + `/buildings/nearby` 호출 | R2/R5 | AR·지도에 실 마커 표시 |
| 4 | AR DetailPopup 가격 그래프 고도화 | R4 | 면적별/거래유형별 가격 추세 시각화 |
| 5 | 통합 QA 체크리스트 실기기 실행 | R1 | 다음 발표 시 정량 진척도 측정 가능 |

- 📸 불필요

---

## 2. 사진 캡처 체크리스트

PPT 제작 전에 미리 9장을 준비해두면 슬라이드 작성이 빠릅니다.

| # | 캡처 대상 | 위치 | 슬라이드 |
|---|---|---|---|
| 1 | AR 옥계동 큐브 + 디버그 정보 | 실기기 / 에뮬레이터 | 5 |
| 2 | 카카오맵 + 현재 위치 마커 | 실기기 / 에뮬레이터 | 6 |
| 3 | AR 하단 BottomSheet 패널 | 실기기 / 에뮬레이터 | 6 |
| 4 | 건물 상세 팝업(거래이력+그래프) | 실기기 / 에뮬레이터 | 6 |
| 5 | `psql \dt` 테이블 목록 | 터미널 | 7 |
| 6 | `curl /health` 응답 JSON | 터미널 | 8 |
| 7 | `gumi_bjdong_codes.csv` 일부 | VSCode/엑셀 | 9 |
| 8 | `api-spec.md` 엔드포인트 표 | 마크다운 미리보기 | 10 |
| 9 | AR ↔ 지도 두 장면 나란히 | 실기기 합성 | 11 |
| (선) | `backend-architecture.md` Mermaid 다이어그램 | 마크다운 미리보기 | 4 |

**캡처 팁**
- 안드로이드 캡처는 `adb exec-out screencap -p > shot.png` 또는 단말 전원+볼륨다운
- AR 화면은 카메라가 옥계동 부근을 비추기 어렵다면 **건물 폴리곤이 보이는 야외에서 촬영**하거나, 디버그 패널만 살리고 카메라는 흐리게 처리해도 무방
- 터미널 캡처는 폰트를 키우고(16pt+) 1~2개 명령만 보이게 잘라야 슬라이드에서 가독성 확보

---

## 3. 진행률 산정 근거 (감사용)

발표 중 "이 퍼센트는 어떻게 나온 건가요?" 질문 대비.

### Android (1,801 LOC / 29 Kotlin 파일)

| 세부 항목 | 진행률 | 근거 파일/라인 |
|---|---|---|
| AR 세션·Geospatial·Anchor | 80% | `ArRoute.kt:57-274` |
| 카카오맵·현재 위치 | 70% | `MapRoute.kt:88-323` |
| 네비게이션·테마 | 90% | `AppNavHost.kt`, `theme/` |
| 상세화면 UI/API 연결 | 75% | `BuildingDetailRoute.kt`는 건물 상세·거래 API 연결, AR `DetailPopupContent.kt`는 건물 상세 API 일부·최근 거래 API·가격 추세 그래프 연결 |
| Retrofit·Repository 정의 | 80% | `ApiServices.kt:15-72`, `AppContainer.kt:1-140` |
| ViewModel ↔ Repository 연결 | 45% | `ArRoute.kt`, `BuildingDetailRoute.kt` 등 주요 경로 일부가 Repository를 통해 API 호출 |

### Backend (40 Java 파일)

| 세부 항목 | 진행률 | 근거 |
|---|---|---|
| 빌드/Docker/설정 | 100% | `build.gradle`, `application.yml`, `Dockerfile` |
| DB 스키마 + 초기데이터 | 100% | `backend/scripts/init_db.sql` |
| 엔티티 7종 + DTO 5종 + Repository 5종 | 70% | `entity/`, `dto/`, `repository/` |
| Controller(5) | 10% | `HealthController` 외 4개 스켈레톤 |
| Service(6) | 5% | 모두 스켈레톤 |
| 외부 API 클라이언트(9) | 30% | `TradeApiClient`, `BuildingDataClient` 구현 |

### Data

| 세부 항목 | 진행률 |
|---|---|
| 의존성 / 환경 | 100% |
| 매핑 CSV (구미 30개 동) | 100% |
| placeholder 수집 스크립트 4종 | 5% (`PLACEHOLDER`, 실행 로직 없음) |

### Docs

| 세부 항목 | 진행률 |
|---|---|
| 5개 핵심 문서 (README/project-guide/api-spec/backend-architecture/역할분담표) | 95% (소소한 보정만 남음) |

---

## 4. 자주 받을 질문 대비 메모

- **Q. 왜 ARCore Geospatial을 썼나?** → GPS 단독 오차(±5~10m)로는 같은 단지 내 동(棟) 식별 불가. Geospatial은 ±1m 수준 → 동 단위 정확도 확보.
- **Q. 카카오맵을 쓴 이유?** → 한국 주소·POI 정확도, 무료 사용 한도, Compose `AndroidView` 통합 부담이 Google Maps 대비 작음. (Google Maps 코드는 `MapRoute.kt:213-237` 주석 보존 — 백업 옵션)
- **Q. PostGIS가 꼭 필요했나?** → `ST_DWithin`으로 반경 내 건물·인프라 검색을 SQL 한 줄로 처리. 애플리케이션단 거리 계산 대비 인덱스(GIST) 효과 큼.
- **Q. 60% 진행이라면 마무리는 언제?** → 다음 단계 5개 작업이 모두 끝나면 Phase 1(MVP) 종료. 데이터 적재 + API 5개 + Android 마커 표시까지 완료 시 시연 가능 수준 도달 예상.

---

## 5. 갱신 가이드

- 본 문서는 **발표마다 새로 만들지 말고 같은 파일을 갱신**하는 것을 권장
- 갱신 시 표 상단의 "기준 일자 / 기준 브랜치" 두 줄만 업데이트
- 진행률(%)은 추측이 아니라 위 "산정 근거" 표의 변화에 맞춰 조정
- 큰 기능이 머지될 때마다 해당 섹션의 진행률·근거를 같이 수정 → CLAUDE.md 의 Documentation Rule 준수
