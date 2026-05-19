# 편의시설 API 명세서

> 프론트엔드(안드로이드 / iOS / 웹) 개발자 대상. 구미시 편의시설 좌표 조회 API.

생성일: 2026-05-14
백엔드 버전: 0.1.0

---

## 1. 개요

DB에 적재된 **4,971건의 구미시 편의시설 POI**를 조회하는 REST API입니다.
주어진 좌표에서 일정 반경 안의 편의시설을 카테고리별로 필터링해서 반환하며, 응답은 거리순으로 정렬됩니다.

지도 화면에서 카테고리별 마커 표시, AR 탐색에서 주변 시설 안내, 점수화 엔진의 입력 등에 사용됩니다.

---

## 2. 기본 정보

| 항목 | 값 |
|---|---|
| Base URL (로컬 개발) | `http://localhost:8080` |
| 인증 | 없음 (내부 API) |
| Content-Type | `application/json; charset=UTF-8` |
| 좌표계 | WGS84 (`EPSG:4326`) |
| 시간대 | KST (한국 표준시) |
| CORS | 현재 별도 설정 없음 (안드로이드 네이티브 호출 전제) |

---

## 3. 공통 응답 구조

### 성공
```json
{
  "status": "ok",
  "data": <결과 본문>,
  "meta": {
    "count": 1,
    "total_count": 58,
    "page": 1,
    "page_size": 100,
    "radius_m": 1000,
    "center": { "lat": 36.1195, "lon": 128.3445 }
  }
}
```

### 실패
```json
{
  "status": "error",
  "error": { "code": "INVALID_PARAMETER", "message": "Unknown category: invalid" }
}
```

### 에러 코드

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_PARAMETER` | 잘못된 쿼리 파라미터 (지원하지 않는 카테고리 등) |
| 400 | `INVALID_PARAMETER` | `lat`/`lon` 누락 또는 숫자 아님 |
| 400 | `INVALID_COORDINATES` | 구미 좌표 범위 밖 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

---

## 4. 엔드포인트

### `GET /api/v1/livability/infra/nearby`

특정 좌표에서 반경 내 편의시설을 조회합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `lat` | `double` | ✅ | — | 중심 위도 (구미 범위 `36.05~36.25`) |
| `lon` | `double` | ✅ | — | 중심 경도 (구미 범위 `128.20~128.50`) |
| `radius` | `int` | ❌ | `1000` | 반경(미터). 서버에서 `1~3000`으로 보정 |
| `category` | `string` | ❌ | (전체) | 카테고리 필터. 6종 enum 중 하나 |
| `page` | `int` | ❌ | `1` | 페이지 번호. 서버에서 최소 `1`로 보정 |
| `page_size` | `int` | ❌ | `100` | 페이지 크기. 서버에서 `1~100`으로 보정 |

#### `category` 허용값 (6종 enum)

| 값 | 한글 | 포함되는 소분류(sub_category) |
|---|---|---|
| `medical` | 의료 | `hospital`, `pharmacy` |
| `education` | 교육 | `school`, `kindergarten`, `daycare`, `academy` |
| `convenience` | 생활편의 | `convenience_store`, `mart` |
| `transport` | 교통 | `bus_stop` |
| `safety` | 안전 | `police_station`, `fire_station`, `cctv` |
| `leisure` | 여가 | `park`, `library`, `cultural_center` |

> `category`를 생략하면 6종 전체를 한 번에 반환합니다.

#### 응답 (200 OK)

```json
{
  "status": "ok",
  "data": [
    {
      "infra_id": 4521,
      "category": "medical",
      "sub_category": "pharmacy",
      "name": "참약사 더봄약국",
      "lat": 36.117338943167,
      "lon": 128.342620085379,
      "address": "경북 구미시 송정대로 34",
      "distance_m": 293.5
    }
  ],
  "meta": {
    "count": 1,
    "total_count": 58,
    "page": 1,
    "page_size": 100,
    "radius_m": 1000,
    "center": { "lat": 36.1195, "lon": 128.3445 }
  }
}
```

#### 응답 데이터 모델 (`InfraNearby`)

| 필드 | 타입 | 항상 존재 | 설명 |
|---|---|---|---|
| `infra_id` | `int` | ✅ | DB PK |
| `category` | `string` | ✅ | 6종 enum |
| `sub_category` | `string` | ✅ | 소분류 |
| `name` | `string` | ✅ | 시설명 |
| `lat` | `double` | ✅ | 위도 (WGS84) |
| `lon` | `double` | ✅ | 경도 (WGS84) |
| `address` | `string \| null` | ❌ | 도로명 주소 (없을 수 있음 — 버스정류장 등) |
| `distance_m` | `double` | ✅ | 중심점에서의 거리(미터) |

> 거리순(가까운 → 먼) 정렬되어 반환됩니다.
> `count`는 현재 페이지에 포함된 응답 건수이고, `total_count`는 같은 좌표/반경/카테고리 조건의 전체 건수입니다.

#### 에러 응답 예시

**잘못된 카테고리**
```
GET /api/v1/livability/infra/nearby?lat=36.1195&lon=128.3445&category=invalid
→ HTTP 400
{
  "status": "error",
  "error": { "code": "INVALID_PARAMETER", "message": "Unknown category: invalid" }
}
```

**필수 파라미터 누락**
```
GET /api/v1/livability/infra/nearby?lat=36.1195
→ HTTP 400
{
  "status": "error",
  "error": { "code": "INVALID_PARAMETER", "message": "lon is required" }
}
```

---

## 5. 데이터 현황 (DB 적재 기준)

전체 4,971건, 구미시 행정구역 내부.

| 카테고리 | 소분류 | 건수 |
|---|---|---:|
| medical | hospital | 308 |
| medical | pharmacy | 146 |
| education | school | 96 |
| education | kindergarten | 67 |
| education | daycare | 228 |
| education | academy | 529 |
| convenience | convenience_store | 359 |
| convenience | mart | 107 |
| transport | bus_stop | 2,115 |
| safety | police_station | 21 |
| safety | fire_station | 13 |
| safety | cctv | 886 |
| leisure | park | 66 |
| leisure | library | 15 |
| leisure | cultural_center | 15 |

데이터 출처와 수집 방식은 [`data/편의시설/REPORT.md`](../data/편의시설/REPORT.md) 참고.

---

## 6. 사용 예시

### cURL
```bash
# 1) 의료 카테고리 반경 500m
curl "http://localhost:8080/api/v1/livability/infra/nearby?lat=36.1195&lon=128.3445&radius=500&category=medical"

# 2) 카테고리 전체 (한 번 호출)
curl "http://localhost:8080/api/v1/livability/infra/nearby?lat=36.1195&lon=128.3445&radius=1000"
```

### Kotlin (Retrofit)
```kotlin
interface LivabilityApiService {
    @GET("api/v1/livability/infra/nearby")
    suspend fun getInfraNearby(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Int? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): ApiResponse<List<InfraNearby>>
}

@Serializable
data class InfraNearby(
    @SerialName("infra_id") val infraId: Int? = null,
    val category: String,
    @SerialName("sub_category") val subCategory: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String? = null,
    @SerialName("distance_m") val distanceMeters: Double? = null,
)
```

### JavaScript (fetch)
```javascript
const params = new URLSearchParams({
  lat: 36.1195, lon: 128.3445, radius: 1000, category: 'medical',
});
const res = await fetch(`http://localhost:8080/api/v1/livability/infra/nearby?${params}`);
const json = await res.json();
console.log(json.meta.count, json.data);
```

---

## 7. 권장 사용 패턴 — 카테고리 토글 UI

지도 화면에서 6개 토글 칩(의료/교육/편의/교통/안전/여가)을 두고, 각 칩을 ON할 때마다 해당 카테고리만 호출하는 방식을 권장합니다.

- **장점**: 사용자가 켠 카테고리만 마커 그림 → 시각적으로 깔끔
- **호출 수**: 사용자가 모든 카테고리를 켜도 최대 6번 (페이지 진입 시 0번)
- **응답 크기**: 기본 `page_size=100`으로 제한됨. 추가 결과는 `page`를 증가시켜 조회

안드로이드 측 참고 구현: [`MapRoute.kt`](../android/app/src/main/java/com/arproperty/android/feature/map/MapRoute.kt) (`MapViewModel.toggleCategory`)

---

## 8. 알려진 제약

- **기본 첫 페이지만 반환** — 반경/카테고리 조건의 전체 건수는 `meta.total_count`를 확인하고, 추가 결과가 필요하면 `page`를 증가시켜 호출
- **CORS 미설정** — 브라우저 기반 호출은 현재 차단됨. 웹 프론트 필요 시 `@CrossOrigin` 또는 `WebMvcConfigurer` 추가 요청
- **인증 없음** — 내부 데모용. 운영 배포 시 토큰 인증 추가 필요
- **CCTV 53건 좌표 누락** — 원본 주소 모호로 지오코딩 실패. DB에는 적재되지 않아 API 응답에는 영향 없음

---

## 9. 향후 추가 후보

- `GET /api/v1/livability/infra/nearby/grouped` — 한 번 호출로 6 카테고리 그룹화된 응답 (단일 라운드트립)
- `GET /api/v1/livability/{building_id}` — 특정 건물 기준 점수 + 카테고리별 최근접 시설 ([`api-spec.md`](api-spec.md) 4.1 참고)
- `GET /api/v1/livability/compare` — 건물 2~3개 점수 비교

---

## 10. 변경 이력

| 날짜 | 변경 |
|---|---|
| 2026-05-19 | `/livability/infra/nearby` radius clamp, `page/page_size`, `total_count` 메타 추가 |
| 2026-05-14 | 최초 작성. `/infra/nearby` 정상 동작 검증 + `GlobalExceptionHandler`로 400 매핑 |
