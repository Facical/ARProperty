# 편의시설 데이터 검증 리포트

생성 시각: 2026-05-14
총 데이터: **5,070건** (15개 CSV)

## 표준 컬럼
모든 CSV 동일 11컬럼:
`category, sub_category, name, lat, lon, address, phone, operator, source_type, source_id, data_source`

## DB 적재 필수 속성
- `category` — CHECK enum: medical / education / convenience / transport / safety / leisure
- `sub_category` — 자유 문자열
- `name` — NOT NULL
- `lat`, `lon` — `point_geom`(POINT 4326)으로 변환, NOT NULL
- `data_source` — CHECK enum: kakao / gumi_opendata / data_go_kr / manual

---

## 1️⃣ 의료 (medical) — 454건

| 소분류 | 파일 | 건수 | 데이터 출처 | 좌표 출처 |
|---|---|---:|---|---|
| 병원/의원 | [`medical/hospital.csv`](medical/hospital.csv) | **308** | 카카오 Local API (카테고리 `HP8`) | 카카오 응답 원본 |
| 약국 | [`medical/pharmacy.csv`](medical/pharmacy.csv) | **146** | 카카오 Local API (카테고리 `PM9`) | 카카오 응답 원본 |

> 둘 다 카카오 단일 출처. 도로명 주소·전화번호까지 100% 포함.

---

## 2️⃣ 교육 (education) — 966건

| 소분류 | 파일 | 건수 | 데이터 출처 | 좌표 출처 |
|---|---|---:|---|---|
| 학교 (초/중/고 통합) | [`education/school.csv`](education/school.csv) | **96** | OpenStreetMap Overpass API (`amenity=school`) | OSM 노드/way 중심좌표 |
| 초등(추정) | [`education/school_elementary_guess.csv`](education/school_elementary_guess.csv) | **46** | OpenStreetMap Overpass API (`amenity=school` + 학교 타입 추정) | OSM 노드/way 중심좌표 |
| 어린이집/유치원 | [`education/kindergarten.csv`](education/kindergarten.csv) | **295** | 카카오 Local API (카테고리 `PS3`) | 카카오 응답 원본 |
| 학원 | [`education/academy.csv`](education/academy.csv) | **529** | 카카오 Local API (카테고리 `AC5`) | 카카오 응답 원본 |

> **혼합 출처**: 학교는 OSM, 어린이집/학원은 카카오. OSM `school:type` 태그가 거의 없어 초/중/고 구분 정확도 낮음 → `school_elementary_guess.csv`는 추정값.

---

## 3️⃣ 생활편의 (convenience) — 466건

| 소분류 | 파일 | 건수 | 데이터 출처 | 좌표 출처 |
|---|---|---:|---|---|
| 편의점 | [`convenience/convenience_store.csv`](convenience/convenience_store.csv) | **359** | 카카오 Local API (카테고리 `CS2`) | 카카오 응답 원본 |
| 마트/슈퍼/백화점 | [`convenience/mart.csv`](convenience/mart.csv) | **107** | OpenStreetMap Overpass API (`shop=supermarket/mall/department_store`) | OSM 노드/way 중심좌표 |

> **혼합 출처**: 편의점은 카카오로 재수집 완료(OSM 70 → 카카오 359). 마트는 아직 OSM — 카카오 `MT1`로 보충하면 address 100% + 건수 증가 가능.

---

## 4️⃣ 교통 (transport) — 2,115건

| 소분류 | 파일 | 건수 | 데이터 출처 | 좌표 출처 |
|---|---|---:|---|---|
| 버스정류장 | [`transport/bus_stop.csv`](transport/bus_stop.csv) | **2,115** | 구미시 공공데이터 xlsx 원본 | **원본 xlsx의 `la`/`lo` 컬럼** (la=경도, lo=위도, 컬럼명 반전 주의) |

> **단일 출처(공공 xlsx)**. 좌표가 원본에 포함되어 있어 외부 호출 없이 그대로 사용. 정류장명만 있고 주소 필드는 없음.
> 구미시는 지하철이 없어 transport는 버스정류장 단일.

---

## 5️⃣ 안전 (safety) — 973건

| 소분류 | 파일 | 건수 | 데이터 출처 | 좌표 출처 |
|---|---|---:|---|---|
| 경찰서/지구대 | [`safety/police.csv`](safety/police.csv) | **21** | OpenStreetMap Overpass API (`amenity=police`) | OSM 노드/way 중심좌표 |
| 소방서/119안전센터 | [`safety/fire_station.csv`](safety/fire_station.csv) | **13** | 카카오 Local API 키워드 검색 (`구미 119안전센터` + `구미 소방서` 통합) | 카카오 응답 원본 |
| CCTV | [`safety/cctv.csv`](safety/cctv.csv) | **939** (좌표 채워진 것 886) | 🔀 **메타데이터 = 구미시 공공데이터 xlsx 원본** | 🔀 **좌표 = 카카오 주소→좌표 API** `/v2/local/search/address.json` (886/939 = 94.4% 성공, 실패 53건은 [`archive/cctv_geocode_failures.csv`](archive/cctv_geocode_failures.csv)) |

> **혼합 출처**: 안전 카테고리는 셋 다 다른 출처. 특히 **CCTV는 메타데이터와 좌표 출처가 다름** — 이름·주소·관리번호는 구미시 xlsx, 좌표는 카카오 지오코딩으로 채움.

---

## 6️⃣ 여가 (leisure) — 96건

| 소분류 | 파일 | 건수 | 데이터 출처 | 좌표 출처 |
|---|---|---:|---|---|
| 공원 | [`leisure/park.csv`](leisure/park.csv) | **66** | OpenStreetMap Overpass API (`leisure=park/garden`) | OSM 노드/way 중심좌표 |
| 도서관 | [`leisure/library.csv`](leisure/library.csv) | **15** | OpenStreetMap Overpass API (`amenity=library`) | OSM 노드/way 중심좌표 |
| 문화시설 | [`leisure/cultural.csv`](leisure/cultural.csv) | **15** | OpenStreetMap Overpass API (`amenity=community_centre/arts_centre/theatre/cinema`) | OSM 노드/way 중심좌표 |

> 모두 OSM 단일 출처. 큰 시설이라 좌표는 정확하지만 address 누락 많음(약 90%).

---

## 📊 출처별 집계

| 출처 | 건수 | 비율 | 어떤 카테고리에 |
|---|---:|---:|---|
| 카카오 Local API | 1,663 | 32.8% | 병원·약국·편의점·어린이집·학원·소방서 (카테고리/키워드 검색) + **CCTV 좌표 보강** (지오코딩) |
| OpenStreetMap (Overpass) | 381 | 7.5% | 학교·마트·경찰서·공원·도서관·문화시설 |
| 구미시 공공데이터 xlsx | 3,054 | 60.2% | 버스정류장 (2,115) + CCTV 메타데이터 (939) |

> CCTV(939)는 두 출처 혼합 — 메타는 구미시 xlsx, 좌표는 카카오 지오코딩.

> `data_source` 컬럼 기준 분류(DB enum):
> - `kakao` 1,663건 — 카카오에서 좌표까지 받은 것 (CCTV 포함)
> - `gumi_opendata` 3,054건 — 구미시 xlsx 원본 메타데이터
> - `manual` 381건 — OSM (DB enum에 osm이 없어 manual로 매핑)

---

## 🔴 필수 속성 누락

`safety/cctv.csv` — lat/lon 누락 **53건** (5.6%, 939건 중)
- 카카오 지오코딩 실패분
- 원인: "○○공원 입구" 등 도로명/지번 없는 주소, 신축 도로, 표기 오타
- 실패 목록: [`archive/cctv_geocode_failures.csv`](archive/cctv_geocode_failures.csv)
- 적재 시 53건 제외 또는 수동 보완 권장

다른 14개 파일은 lat/lon/name 모두 100% 채워짐 ✅

---

## 🟡 보조 속성(address) 누락 — DB 스키마상 nullable

| 파일 | 누락 비율 | 출처 한계 |
|---|---|---|
| `transport/bus_stop.csv` | 2115/2115 (100%) | xlsx 원본에 주소 컬럼 없음, 정류장명만 |
| `convenience/mart.csv` | 98/107 (92%) | OSM 한계 — 카카오 `MT1`로 재수집하면 보충 가능 |
| `education/school.csv` | 73/96 (76%) | OSM 한계 — 학교명 키워드 검색으로 보충 가능 |
| `leisure/park.csv` | 64/66 (97%) | OSM 한계 |
| `education/school_elementary_guess.csv` | 37/46 (80%) | OSM 한계 |
| `safety/police.csv` | 20/21 (95%) | OSM 한계 |
| `leisure/cultural.csv` | 14/15 (93%) | OSM 한계 |
| `leisure/library.csv` | 13/15 (87%) | OSM 한계 |

> 카카오·구미시 xlsx 출처 파일은 address 누락 거의 없음. **누락은 모두 OSM 출처 파일.**

---

## 📂 최종 폴더 구조

```
data/편의시설/
├── archive/                          이전 OSM 보존본 + 지오코딩 실패 로그
├── medical/         (카카오 단일)
│   ├── hospital.csv                  308
│   └── pharmacy.csv                  146
├── education/       (OSM + 카카오 혼합)
│   ├── school.csv                    96     OSM
│   ├── school_elementary_guess.csv   46     OSM
│   ├── kindergarten.csv              295    Kakao
│   └── academy.csv                   529    Kakao
├── convenience/     (카카오 + OSM 혼합)
│   ├── convenience_store.csv         359    Kakao
│   └── mart.csv                      107    OSM
├── transport/       (구미시 xlsx 단일)
│   └── bus_stop.csv                  2,115
├── safety/          (3개 출처 혼합)
│   ├── police.csv                    21     OSM
│   ├── fire_station.csv              13     Kakao
│   └── cctv.csv                      939    구미시 xlsx + 카카오 지오코딩
├── leisure/         (OSM 단일)
│   ├── park.csv                      66
│   ├── library.csv                   15
│   └── cultural.csv                  15
└── REPORT.md
```

---

## 🛠️ 수집/변환 스크립트 (재실행 가능)

| 단계 | 스크립트 | 처리 내용 |
|---|---|---|
| 1차 OSM 수집 | [`data/collectors/collect_osm_gumi.py`](../collectors/collect_osm_gumi.py) | Overpass API로 학교/공원/도서관 등 13종 일괄 수집 |
| 카카오 1차 수집 | [`data/collectors/collect_kakao_gumi.py`](../collectors/collect_kakao_gumi.py) | 약국·어린이집·학원·소방서 격자 sweep |
| 폴더 정리 | [`data/collectors/normalize_facilities.py`](../collectors/normalize_facilities.py) | xlsx→CSV 변환 + 카테고리 폴더로 표준화 |
| 카카오 2차 보충 | [`data/collectors/collect_kakao_more.py`](../collectors/collect_kakao_more.py) | 병원·편의점 재수집 (OSM 노이즈/과소 해결) |
| CCTV 지오코딩 | [`data/collectors/geocode_cctv.py`](../collectors/geocode_cctv.py) | 카카오 주소→좌표 API로 939건 좌표 보강 |
| 리포트 갱신 | [`data/collectors/regenerate_report.py`](../collectors/regenerate_report.py) | 자동 검증 + REPORT.md 생성 |
