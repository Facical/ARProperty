# Integration Demo - Samgu Trinien (Okgye)

## 1) Goal
- Verify end-to-end data integration for one target complex: `삼구트리니엔(옥계동 907)`.
- Scope:
  - `VWorld API -> apt_building_master` (geometry + building management number)
  - `Local CSV -> apt_building_master` (building register fields)
  - `Local CSV -> apt_trade_history` (sale trade history)

## 2) Input Sources
- VWorld API:
  - Layer: `LT_C_SPBD`
  - Filter: `bd_mgt_sn:like:4719012800109070000`
- Local files:
  - `data/삼구트리니엔 API/gumi_building_data 건축물 대장 표제부 (옥계 삼구트리니엔).csv`
  - `data/삼구트리니엔 API/gumi_apt_trade_2020_2026 아파트 매매 실거래가 (옥계 삼구 트리니엔).csv`

## 3) Run Commands
```bash
# 1) VWorld -> building master
cd backend
./gradlew.bat bootRun --args="--sync-vworld-samgu --spring.main.web-application-type=none"

# 2) Local CSV -> building/trade
./gradlew.bat bootRun --args="--sync-local-samgu --spring.main.web-application-type=none"
```

## 4) Verification SQL
```sql
-- A. Building geometry + key sync
SELECT dong_name,
       building_management_number IS NOT NULL AS has_mgmt_no,
       polygon_geom IS NOT NULL AS has_polygon,
       centroid IS NOT NULL AS has_centroid
FROM apt_building_master
ORDER BY dong_name;

-- B. Building register fields
SELECT dong_name, ground_floors, underground_floors, total_area, use_approval_date, structure_type
FROM apt_building_master
ORDER BY dong_name;

-- C. Trade load summary
SELECT COUNT(*) AS trade_total, MIN(deal_date) AS min_date, MAX(deal_date) AS max_date
FROM apt_trade_history;
```

## 5) Current Result Snapshot
- Building target rows: `8개 동 (101,102,103,105,106,107,108,109)`
- VWorld sync: `8/8` updated
- Building register fields: updated from local CSV for all target rows
- Trade history loaded from local CSV:
  - `trade_total = 650`
  - `min_date = 2020-01-03`
  - `max_date = 2026-04-13`

## 6) Notes / Limitations
- This demo uses **local CSV** for trade/building-register data (not real-time Data.go.kr trade API).
- Current trade table may include duplicates from repeated runs.
- Recommended follow-up:
  - add stronger duplicate key/constraint for `apt_trade_history`
  - add a dry-run mode and summary log report
