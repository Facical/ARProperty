# 연동 데모 - 삼구트리니엔(옥계)

## 1) 목적
- 단일 대상 단지 `삼구트리니엔(옥계동 907)` 기준으로 데이터 연동 결과를 검증한다.
- 범위:
  - `VWorld API -> apt_building_master` (지오메트리 + 건물관리번호)
  - `로컬 CSV -> apt_building_master` (건축물대장 주요 필드)
  - `로컬 CSV -> apt_trade_history` (매매 실거래 이력)

## 2) 입력 소스
- VWorld API
  - 레이어: `LT_C_SPBD`
  - 필터: `bd_mgt_sn:like:4719012800109070000`
- 로컬 파일
  - `data/삼구트리니엔 API/gumi_building_data 건축물 대장 표제부 (옥계 삼구트리니엔).csv`
  - `data/삼구트리니엔 API/gumi_apt_trade_2020_2026 아파트 매매 실거래가 (옥계 삼구 트리니엔).csv`

## 3) 실행 명령어
```bash
# 1) VWorld -> 건물 마스터 반영
cd backend
./gradlew.bat bootRun --args="--sync-vworld-samgu --spring.main.web-application-type=none"

# 2) 로컬 CSV -> 건물/거래 반영
./gradlew.bat bootRun --args="--sync-local-samgu --spring.main.web-application-type=none"
```

## 4) 검증 SQL
```sql
-- A. VWorld 동기화 결과(건물관리번호/폴리곤/중심점)
SELECT dong_name,
       building_management_number IS NOT NULL AS has_mgmt_no,
       polygon_geom IS NOT NULL AS has_polygon,
       centroid IS NOT NULL AS has_centroid
FROM apt_building_master
ORDER BY dong_name;

-- B. 건축물대장 필드 반영 결과
SELECT dong_name, ground_floors, underground_floors, total_area, use_approval_date, structure_type
FROM apt_building_master
ORDER BY dong_name;

-- C. 실거래 적재 요약
SELECT COUNT(*) AS trade_total, MIN(deal_date) AS min_date, MAX(deal_date) AS max_date
FROM apt_trade_history;
```

## 5) 현재 결과 스냅샷
- 대상 건물 행: `8개 동 (101,102,103,105,106,107,108,109)`
- VWorld 동기화: `8/8` 반영
- 건축물대장 필드: 대상 8개 동 반영 완료
- 매매 실거래 적재(로컬 CSV 기준):
  - `trade_total = 650`
  - `min_date = 2020-01-03`
  - `max_date = 2026-04-13`

## 6) 참고 / 제한사항
- 본 데모의 실거래/건축물대장 반영은 **실시간 API가 아니라 로컬 CSV 기반**이다.
- 반복 실행으로 `apt_trade_history` 중복 데이터가 포함될 수 있다.
- 후속 권장사항:
  - `apt_trade_history` 중복 방지 키/제약 강화
  - `dry-run` 모드 및 실행 요약 로그 추가
