# ARProperty API Spec

> 버전: v1.1
> 목적: 프론트엔드와 백엔드가 공유하는 계약 문서
> 로컬 Base URL: `http://localhost:8080`
> 경로 규칙: `/api/v1/...` (`/health` 제외)

이 문서는 응답 형식과 필드 계약을 정의합니다. 실제 구현 진행 상태는 [README.md](../README.md)의 `현재 저장소 상태`를 기준으로 확인합니다.

## 공통 규칙

### 응답 래퍼

성공 응답:

```json
{
  "status": "success",
  "data": {},
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 1
  }
}
```

에러 응답:

```json
{
  "status": "error",
  "error": {
    "code": "INVALID_PARAMETER",
    "message": "year must be between 2006 and 2100"
  }
}
```

### 필드 규칙

- JSON 필드명은 모두 `snake_case`를 사용합니다.
- 날짜는 `YYYY-MM-DD`, 일시는 ISO-8601 UTC 문자열을 사용합니다.
- 가격 단위는 `만원`, 거리 단위는 `m`, 면적 단위는 `m2`입니다.
- 목록형 응답은 `page`, `page_size`, `total_count`를 `meta`에 포함합니다.
- `page` 기본값은 `1`, `page_size` 기본값은 `20`, 최대값은 `100`입니다.

### 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `INVALID_PARAMETER` | 400 | 필수 파라미터 누락 또는 형식 오류 |
| `INVALID_COORDINATES` | 400 | 위도/경도 범위 오류 |
| `BUILDING_NOT_FOUND` | 404 | 건물 ID에 해당하는 데이터 없음 |
| `COMPLEX_NOT_FOUND` | 404 | 단지 ID에 해당하는 데이터 없음 |
| `UNSUPPORTED_PRESET` | 400 | 지원하지 않는 편의시설 가중치 프리셋 |
| `EXTERNAL_API_ERROR` | 502 | 외부 API 호출 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## 엔드포인트 상태

| 메서드 | 경로 | 단계 | 상태 |
|--------|------|------|------|
| `GET` | `/health` | 공통 | 기준 확정 |
| `GET` | `/api/v1/buildings/nearby` | MVP | 기준 확정 |
| `GET` | `/api/v1/buildings/{building_id}` | Phase 2 | 기준 확정 |
| `GET` | `/api/v1/buildings/{building_id}/trades` | MVP | 기준 확정 |
| `GET` | `/api/v1/complexes` | Phase 2 | 기준 확정 |
| `GET` | `/api/v1/complexes/{complex_id}` | Phase 2 | 기준 확정 |
| `GET` | `/api/v1/complexes/{complex_id}/buildings` | Phase 2 | 기준 확정 |
| `GET` | `/api/v1/complexes/{complex_id}/trades` | Phase 2 | 기준 확정 |
| `GET` | `/api/v1/livability/{building_id}` | Phase 4 | 기준 확정 |
| `GET` | `/api/v1/livability/compare` | Phase 4 | 기준 확정 |
| `GET` | `/api/v1/infra/nearby` | Phase 4 | planned |

## 1. Health

### `GET /health`

서비스와 주요 의존성 상태를 반환합니다.

```json
{
  "status": "success",
  "data": {
    "server": "ok",
    "database": "ok",
    "redis": "ok",
    "version": "0.1.0"
  }
}
```

## 2. Building APIs

### `GET /api/v1/buildings/nearby`

사용자 좌표 기준으로 주변 아파트 동 목록을 조회합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `lat` | float | O | 위도 |
| `lon` | float | O | 경도 |
| `radius` | int | X | 반경(m), 기본값 `500`, 최대 `2000` |
| `min_price` | int | X | 최소 거래가 |
| `max_price` | int | X | 최대 거래가 |
| `min_area` | float | X | 최소 전용면적 |
| `max_area` | float | X | 최대 전용면적 |
| `min_grade` | string | X | 최소 편의시설 등급 (`S`, `A`, `B`, `C`, `D`, `F`) |
| `page` | int | X | 페이지 번호 |
| `page_size` | int | X | 페이지 크기 |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "building_id": 42,
      "complex_id": 10,
      "complex_name": "구미 래미안",
      "dong_name": "101동",
      "lat": 36.1195,
      "lon": 128.3445,
      "ground_floors": 25,
      "latest_trade": {
        "deal_amount": 58000,
        "exclusive_area": 84.0,
        "floor": 15,
        "deal_date": "2026-03-15",
        "trade_type": "매매"
      },
      "livability_grade": "A",
      "livability_score": 82.0,
      "distance_m": 120.5
    }
  ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 15,
    "center_lat": 36.1195,
    "center_lon": 128.3445,
    "radius": 500
  }
}
```

### `GET /api/v1/buildings/{building_id}`

특정 동의 상세 정보를 반환합니다.

#### Response Example

```json
{
  "status": "success",
  "data": {
    "building_id": 42,
    "complex_id": 10,
    "complex_name": "구미 래미안",
    "dong_name": "101동",
    "lat": 36.1195,
    "lon": 128.3445,
    "ground_floors": 25,
    "underground_floors": 2,
    "highest_floor": 25,
    "building_height": 75.0,
    "structure_type": "철근콘크리트구조",
    "total_area": 12500.5,
    "use_approval_date": "2015-06-20",
    "complex_info": {
      "kapt_code": "A12345678",
      "households": 850,
      "building_count": 8,
      "parking_count": 1200,
      "elevator_count": 16,
      "heating_type": "지역난방",
      "constructor": "삼성물산"
    },
    "livability": {
      "total_score": 82.0,
      "grade": "A"
    }
  }
}
```

### `GET /api/v1/buildings/{building_id}/trades`

건물 단위 거래 이력을 조회합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | X | `매매`, `전세`, `월세` |
| `year` | int | X | 특정 연도 필터 |
| `page` | int | X | 페이지 번호 |
| `page_size` | int | X | 페이지 크기 |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "trade_id": 1234,
      "dong_name": "101동",
      "floor": 15,
      "exclusive_area": 84.0,
      "deal_amount": 58000,
      "deposit": null,
      "monthly_rent": null,
      "deal_date": "2026-03-15",
      "trade_type": "매매",
      "dealing_type": "중개거래"
    },
    {
      "trade_id": 1235,
      "dong_name": "101동",
      "floor": 8,
      "exclusive_area": 59.0,
      "deal_amount": null,
      "deposit": 30000,
      "monthly_rent": 50,
      "deal_date": "2026-02-20",
      "trade_type": "월세",
      "dealing_type": "중개거래"
    }
  ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 45
  }
}
```

## 3. Complex APIs

### `GET /api/v1/complexes`

단지 목록을 조회합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `dong_code` | string | X | 법정동코드 10자리 |
| `name` | string | X | 단지명 부분 검색 |
| `page` | int | X | 페이지 번호 |
| `page_size` | int | X | 페이지 크기 |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "complex_id": 10,
      "kapt_code": "A12345678",
      "complex_name": "구미 래미안",
      "road_address": "경상북도 구미시 옥계북로 100",
      "households": 850,
      "building_count": 8,
      "completion_date": "2015-06-20"
    }
  ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 25
  }
}
```

### `GET /api/v1/complexes/{complex_id}`

특정 단지의 메타데이터를 조회합니다.

#### Response Example

```json
{
  "status": "success",
  "data": {
    "complex_id": 10,
    "kapt_code": "A12345678",
    "complex_name": "구미 래미안",
    "road_address": "경상북도 구미시 옥계북로 100",
    "parcel_address": "경상북도 구미시 옥계동 100",
    "households": 850,
    "building_count": 8,
    "completion_date": "2015-06-20",
    "constructor": "삼성물산",
    "heating_type": "지역난방",
    "parking_count": 1200,
    "elevator_count": 16
  }
}
```

### `GET /api/v1/complexes/{complex_id}/buildings`

특정 단지에 속한 동 목록을 반환합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `page` | int | X | 페이지 번호 |
| `page_size` | int | X | 페이지 크기 |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "building_id": 42,
      "dong_name": "101동",
      "ground_floors": 25,
      "highest_floor": 25,
      "lat": 36.1195,
      "lon": 128.3445
    },
    {
      "building_id": 43,
      "dong_name": "102동",
      "ground_floors": 20,
      "highest_floor": 20,
      "lat": 36.1198,
      "lon": 128.3450
    }
  ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 8
  }
}
```

### `GET /api/v1/complexes/{complex_id}/trades`

단지 전체 거래 이력을 조회합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | X | `매매`, `전세`, `월세` |
| `year` | int | X | 특정 연도 필터 |
| `month` | int | X | 특정 월 필터 |
| `page` | int | X | 페이지 번호 |
| `page_size` | int | X | 페이지 크기 |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "trade_id": 1234,
      "building_id": 42,
      "dong_name": "101동",
      "floor": 15,
      "exclusive_area": 84.0,
      "deal_amount": 58000,
      "deposit": null,
      "monthly_rent": null,
      "deal_date": "2026-03-15",
      "trade_type": "매매"
    }
  ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 120
  }
}
```

## 4. Livability APIs

### `GET /api/v1/livability/{building_id}`

건물별 생활 인프라 종합 점수와 카테고리별 상세 정보를 반환합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `preset` | string | X | `default`, `childcare`, `worker`, `senior` |

#### Response Example

```json
{
  "status": "success",
  "data": {
    "building_id": 42,
    "total_score": 82.0,
    "grade": "A",
    "weight_preset": "default",
    "categories": {
      "medical": {
        "score": 95.0,
        "weight": 0.2,
        "count_500m": 5,
        "nearest": [
          {
            "name": "구미제일의원",
            "sub_category": "hospital",
            "distance_m": 120,
            "walk_minutes": 2
          }
        ]
      },
      "education": {
        "score": 78.0,
        "weight": 0.2,
        "count_500m": 3,
        "nearest": [
          {
            "name": "옥계초등학교",
            "sub_category": "elementary_school",
            "distance_m": 350,
            "walk_minutes": 4
          }
        ]
      },
      "convenience": {
        "score": 88.0,
        "weight": 0.2,
        "count_500m": 7,
        "nearest": []
      },
      "transport": {
        "score": 72.0,
        "weight": 0.2,
        "count_500m": 3,
        "nearest": []
      },
      "safety": {
        "score": 92.0,
        "weight": 0.1,
        "count_500m": 12,
        "nearest": []
      },
      "leisure": {
        "score": 65.0,
        "weight": 0.1,
        "count_500m": 2,
        "nearest": []
      }
    },
    "calculated_at": "2026-04-10T12:00:00Z"
  }
}
```

### `GET /api/v1/livability/compare`

건물 2~3개의 생활 인프라 점수를 비교합니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `building_ids` | string | O | 쉼표 구분 건물 ID 목록 |
| `preset` | string | X | `default`, `childcare`, `worker`, `senior` |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "building_id": 42,
      "complex_name": "구미 래미안",
      "dong_name": "101동",
      "total_score": 82.0,
      "grade": "A",
      "medical_score": 95.0,
      "education_score": 78.0,
      "convenience_score": 88.0,
      "transport_score": 72.0,
      "safety_score": 92.0,
      "leisure_score": 65.0
    },
    {
      "building_id": 55,
      "complex_name": "옥계 푸르지오",
      "dong_name": "201동",
      "total_score": 75.0,
      "grade": "B",
      "medical_score": 80.0,
      "education_score": 85.0,
      "convenience_score": 70.0,
      "transport_score": 68.0,
      "safety_score": 85.0,
      "leisure_score": 60.0
    }
  ]
}
```

## 5. Infra API

### `GET /api/v1/infra/nearby`

> 상태: `planned`

지도 뷰 또는 생활 인프라 상세 화면에서 주변 시설 목록을 조회하기 위한 API입니다.

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `lat` | float | O | 위도 |
| `lon` | float | O | 경도 |
| `radius` | int | X | 반경(m), 기본값 `1000` |
| `category` | string | X | `medical`, `education`, `convenience`, `transport`, `safety`, `leisure` |
| `page` | int | X | 페이지 번호 |
| `page_size` | int | X | 페이지 크기 |

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "infra_id": 100,
      "category": "medical",
      "sub_category": "hospital",
      "name": "구미제일의원",
      "lat": 36.12,
      "lon": 128.345,
      "address": "경상북도 구미시 옥계북로 50",
      "distance_m": 120,
      "data_source": "kakao"
    }
  ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_count": 35
  }
}
```
