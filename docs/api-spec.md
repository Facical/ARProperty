# ARProperty REST API 명세서

> 버전: v1.0 (초안)
> 작성일: 2026-04-14
> 백엔드: Spring Boot (Java 17)
> Base URL: `http://localhost:8000`

---

## 공통 사항

### 응답 형식

모든 응답은 아래 래퍼로 감싼다.

**성공 응답**:
```json
{
  "status": "success",
  "data": { ... },
  "meta": {
    "total_count": 150,
    "page": 1,
    "page_size": 20
  }
}
```

**에러 응답**:
```json
{
  "status": "error",
  "error": {
    "code": "BUILDING_NOT_FOUND",
    "message": "건물을 찾을 수 없습니다"
  }
}
```

### 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `BUILDING_NOT_FOUND` | 404 | 건물 ID에 해당하는 데이터 없음 |
| `COMPLEX_NOT_FOUND` | 404 | 단지 ID에 해당하는 데이터 없음 |
| `INVALID_COORDINATES` | 400 | 위도/경도 값이 유효 범위 밖 |
| `INVALID_PARAMETER` | 400 | 필수 파라미터 누락 또는 형식 오류 |
| `EXTERNAL_API_ERROR` | 502 | 외부 공공데이터 API 호출 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### 인증

초기 개발 단계에서는 인증 없이 운영한다. 공공데이터 API 키는 서버에서만 관리한다.

### 페이지네이션

리스트 API는 `page`와 `page_size` 쿼리 파라미터를 지원한다.
- `page`: 페이지 번호 (기본값: 1)
- `page_size`: 페이지당 항목 수 (기본값: 20, 최대: 100)

---

## 엔드포인트 상세

---

### 1. 헬스체크

#### `GET /health`

서버 및 DB 연결 상태 확인.

**응답 예시**:
```json
{
  "status": "success",
  "data": {
    "server": "ok",
    "database": "ok",
    "version": "0.1.0"
  }
}
```

---

### 2. 건물 API

#### `GET /api/v1/buildings/nearby`

주변 건물 목록 조회 (PostGIS 반경 검색).

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `lat` | float | O | 위도 (36.05~36.25) |
| `lon` | float | O | 경도 (128.20~128.50) |
| `radius` | int | X | 반경(m), 기본값 500, 최대 2000 |
| `min_price` | int | X | 최소 거래가(만원) |
| `max_price` | int | X | 최대 거래가(만원) |
| `min_area` | float | X | 최소 전용면적(m2) |
| `max_area` | float | X | 최대 전용면적(m2) |
| `min_grade` | string | X | 최소 편의시설 등급 (S/A/B/C/D/F) |

**응답 예시**:
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
      "recent_trade": {
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
    "total_count": 15,
    "center_lat": 36.1195,
    "center_lon": 128.3445,
    "radius": 500
  }
}
```

---

#### `GET /api/v1/buildings/{building_id}`

건물 상세 정보 조회.

**응답 예시**:
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

---

#### `GET /api/v1/buildings/{building_id}/trades`

건물별 거래 이력 조회.

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | X | 거래유형: 매매, 전세, 월세 (미지정 시 전체) |
| `limit` | int | X | 조회 건수 (기본값: 20, 최대: 100) |
| `year` | int | X | 특정 년도 필터 |

**응답 예시**:
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
    "total_count": 45
  }
}
```

---

### 3. 단지 API

#### `GET /api/v1/complexes`

법정동 내 단지 목록 조회.

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `dong_code` | string | X | 법정동코드 10자리 (예: 4719012800) |
| `name` | string | X | 단지명 검색 (부분 일치) |

**응답 예시**:
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
    "total_count": 25
  }
}
```

---

#### `GET /api/v1/complexes/{complex_id}`

단지 상세 정보 조회.

---

#### `GET /api/v1/complexes/{complex_id}/buildings`

단지 내 동 목록 조회.

**응답 예시**:
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
  ]
}
```

---

#### `GET /api/v1/complexes/{complex_id}/trades`

단지 전체 거래 이력 조회.

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | X | 매매/전세/월세 |
| `year` | int | X | 년도 |
| `month` | int | X | 월 |

---

### 4. 편의시설 점수 API

#### `GET /api/v1/livability/{building_id}`

건물별 편의시설 종합 점수 + 카테고리별 상세.

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `preset` | string | X | 가중치 프리셋: default, childcare, worker, senior (기본값: default) |

**응답 예시**:
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
        "weight": 0.20,
        "nearest": [
          {
            "name": "구미제일의원",
            "sub_category": "hospital",
            "distance_m": 120,
            "walk_minutes": 2
          },
          {
            "name": "참좋은약국",
            "sub_category": "pharmacy",
            "distance_m": 180,
            "walk_minutes": 2
          }
        ],
        "count_500m": 5
      },
      "education": {
        "score": 78.0,
        "weight": 0.20,
        "nearest": [
          {
            "name": "옥계초등학교",
            "sub_category": "elementary_school",
            "distance_m": 350,
            "walk_minutes": 4
          }
        ],
        "count_500m": 3
      },
      "convenience": {
        "score": 88.0,
        "weight": 0.20,
        "nearest": [],
        "count_500m": 7
      },
      "transport": {
        "score": 72.0,
        "weight": 0.20,
        "nearest": [],
        "count_500m": 3
      },
      "safety": {
        "score": 92.0,
        "weight": 0.10,
        "nearest": [],
        "count_500m": 12
      },
      "leisure": {
        "score": 65.0,
        "weight": 0.10,
        "nearest": [],
        "count_500m": 2
      }
    },
    "calculated_at": "2026-04-10T12:00:00Z"
  }
}
```

---

#### `GET /api/v1/livability/compare`

건물 간 편의시설 점수 비교.

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `building_ids` | string | O | 쉼표 구분 건물 ID (2~3개, 예: 42,43,44) |
| `preset` | string | X | 가중치 프리셋 (기본값: default) |

**응답 예시**:
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

---

#### `GET /api/v1/infra/nearby`

주변 편의시설 목록 조회.

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `lat` | float | O | 위도 |
| `lon` | float | O | 경도 |
| `radius` | int | X | 반경(m), 기본값 1000 |
| `category` | string | X | 카테고리: medical, education, convenience, transport, safety, leisure |

**응답 예시**:
```json
{
  "status": "success",
  "data": [
    {
      "infra_id": 100,
      "category": "medical",
      "sub_category": "hospital",
      "name": "구미제일의원",
      "lat": 36.1200,
      "lon": 128.3450,
      "address": "경상북도 구미시 옥계북로 50",
      "distance_m": 120,
      "data_source": "kakao"
    }
  ],
  "meta": {
    "total_count": 35
  }
}
```

---

### 5. 지오코딩 API

#### `GET /api/v1/geocode`

주소 -> 좌표 변환 (Vworld 프록시).

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `address` | string | O | 검색할 주소 |

**응답 예시**:
```json
{
  "status": "success",
  "data": {
    "lat": 36.1195,
    "lon": 128.3445,
    "address": "경상북도 구미시 옥계북로 100"
  }
}
```

---

#### `GET /api/v1/reverse-geocode`

좌표 -> 주소 변환 (Vworld 프록시).

**파라미터**:

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `lat` | float | O | 위도 |
| `lon` | float | O | 경도 |

**응답 예시**:
```json
{
  "status": "success",
  "data": {
    "road_address": "경상북도 구미시 옥계북로 100",
    "parcel_address": "경상북도 구미시 옥계동 123-4",
    "legal_dong_code": "4719012800",
    "dong_name": "옥계동"
  }
}
```

---

## 가중치 프리셋 정의

| 프리셋 | 의료 | 교육 | 생활편의 | 교통 | 안전 | 여가 |
|--------|------|------|----------|------|------|------|
| `default` | 20% | 20% | 20% | 20% | 10% | 10% |
| `childcare` | 15% | 30% | 15% | 15% | 20% | 5% |
| `worker` | 10% | 5% | 25% | 35% | 10% | 15% |
| `senior` | 35% | 5% | 25% | 20% | 10% | 5% |

## 등급 기준

| 점수 | 등급 |
|------|------|
| 90~100 | S |
| 75~89 | A |
| 60~74 | B |
| 45~59 | C |
| 30~44 | D |
| 0~29 | F |
