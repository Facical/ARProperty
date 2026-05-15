-- =============================================================
-- 옥계동(법정동코드 4719012800) 데모 시드 데이터
-- AR/지도 프로토타입용. 멱등성 보장(ON CONFLICT DO NOTHING / WHERE NOT EXISTS).
--
-- ⚠️ 단지(apt_complex_master)는 더 이상 이 파일에서 시드하지 않는다.
--    공공데이터 apt_list + Kakao Local 좌표 조회로 실제 단지를 채운다:
--      ./gradlew bootRun --args='--sync-okgye-complex'
--    아래의 동(棟)/거래/생활점수 INSERT는 DEMO_OKGYE_A/B/C kapt_code를
--    참조하는데, 해당 단지가 sync 결과에 존재하지 않으므로 JOIN에서 0행이
--    되어 무해하다. 실제 단지가 정해진 뒤에는 별도 시드/스크립트로 대체.
--
-- 적재:
--   docker exec -i $(docker-compose ps -q db) psql -U arproperty -d arproperty \
--     < backend/scripts/seed_demo_okgye.sql
-- =============================================================

BEGIN;

-- -------------------------------------------------------------
-- 2) 단지별 동(building) — UNIQUE(complex_id, dong_name)로 멱등
-- -------------------------------------------------------------
INSERT INTO apt_building_master
    (complex_id, dong_name, centroid,
     ground_floors, underground_floors, highest_floor, building_height, structure_type)
SELECT c.complex_id, v.dong_name, v.centroid,
       v.gf, v.uf, v.hf, v.bh, v.st
FROM (VALUES
    -- 단지 A: 동 3개
    ('DEMO_OKGYE_A', '101동',
        ST_SetSRID(ST_MakePoint(128.3401, 36.1306), 4326)::geometry,
        25, 2, 25, 78.0::numeric, '철근콘크리트'),
    ('DEMO_OKGYE_A', '102동',
        ST_SetSRID(ST_MakePoint(128.3406, 36.1304), 4326)::geometry,
        22, 2, 22, 68.0::numeric, '철근콘크리트'),
    ('DEMO_OKGYE_A', '103동',
        ST_SetSRID(ST_MakePoint(128.3408, 36.1308), 4326)::geometry,
        20, 1, 20, 62.0::numeric, '철근콘크리트'),
    -- 단지 B: 동 2개
    ('DEMO_OKGYE_B', '201동',
        ST_SetSRID(ST_MakePoint(128.3414, 36.1300), 4326)::geometry,
        18, 1, 18, 56.0::numeric, '철근콘크리트'),
    ('DEMO_OKGYE_B', '202동',
        ST_SetSRID(ST_MakePoint(128.3417, 36.1296), 4326)::geometry,
        15, 1, 15, 48.0::numeric, '철근콘크리트'),
    -- 단지 C: 동 3개
    ('DEMO_OKGYE_C', '301동',
        ST_SetSRID(ST_MakePoint(128.3386, 36.1313), 4326)::geometry,
        24, 2, 24, 74.0::numeric, '철근콘크리트'),
    ('DEMO_OKGYE_C', '302동',
        ST_SetSRID(ST_MakePoint(128.3390, 36.1311), 4326)::geometry,
        24, 2, 24, 74.0::numeric, '철근콘크리트'),
    ('DEMO_OKGYE_C', '303동',
        ST_SetSRID(ST_MakePoint(128.3385, 36.1309), 4326)::geometry,
        21, 1, 21, 65.0::numeric, '철근콘크리트')
) AS v(kapt, dong_name, centroid, gf, uf, hf, bh, st)
JOIN apt_complex_master c ON c.kapt_code = v.kapt
ON CONFLICT (complex_id, dong_name) DO NOTHING;

-- -------------------------------------------------------------
-- 3) 거래 이력 — apt_trade_history는 unique key 없음 → NOT EXISTS로 멱등
--    (complex_id + dong_name + deal_date + deal_amount 조합)
-- -------------------------------------------------------------
INSERT INTO apt_trade_history
    (complex_id, dong_name, floor, exclusive_area, deal_amount,
     deal_date, deal_year, deal_month, trade_type, apt_name)
SELECT c.complex_id, v.dong_name, v.floor, v.area, v.amount,
       v.dd, EXTRACT(YEAR FROM v.dd)::int, EXTRACT(MONTH FROM v.dd)::int,
       v.tt, c.complex_name
FROM (VALUES
    -- 단지 A
    ('DEMO_OKGYE_A', '101동', 12,  84.0::numeric, 35000, DATE '2026-03-15', '매매'),
    ('DEMO_OKGYE_A', '101동',  7,  59.8::numeric, 26500, DATE '2025-12-02', '매매'),
    ('DEMO_OKGYE_A', '102동', 15,  84.0::numeric, 36800, DATE '2026-02-08', '매매'),
    ('DEMO_OKGYE_A', '103동',  3,  59.8::numeric, 24800, DATE '2025-11-20', '매매'),
    -- 단지 B
    ('DEMO_OKGYE_B', '201동', 10,  74.6::numeric, 28000, DATE '2026-01-18', '매매'),
    ('DEMO_OKGYE_B', '202동',  6,  59.8::numeric, 23200, DATE '2025-10-30', '매매'),
    -- 단지 C
    ('DEMO_OKGYE_C', '301동', 18, 101.5::numeric, 52000, DATE '2026-04-02', '매매'),
    ('DEMO_OKGYE_C', '302동', 11,  84.0::numeric, 41500, DATE '2026-02-22', '매매'),
    ('DEMO_OKGYE_C', '303동',  8,  84.0::numeric, 39800, DATE '2025-12-15', '매매')
) AS v(kapt, dong_name, floor, area, amount, dd, tt)
JOIN apt_complex_master c ON c.kapt_code = v.kapt
WHERE NOT EXISTS (
    SELECT 1 FROM apt_trade_history th
    WHERE th.complex_id = c.complex_id
      AND th.dong_name  = v.dong_name
      AND th.deal_date  = v.dd
      AND th.deal_amount = v.amount
);

-- -------------------------------------------------------------
-- 4) 생활점수 (preset='default') — UNIQUE(building_id, weight_preset)
-- -------------------------------------------------------------
INSERT INTO building_livability_score
    (building_id, total_score, grade, weight_preset, calculated_at, expires_at)
SELECT b.building_id, v.score, v.grade, 'default',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days'
FROM (VALUES
    ('DEMO_OKGYE_A', '101동', 88.0::numeric, 'A'),
    ('DEMO_OKGYE_A', '102동', 82.4::numeric, 'A'),
    ('DEMO_OKGYE_A', '103동', 74.1::numeric, 'B'),
    ('DEMO_OKGYE_B', '201동', 69.5::numeric, 'B'),
    ('DEMO_OKGYE_B', '202동', 58.2::numeric, 'C'),
    ('DEMO_OKGYE_C', '301동', 92.7::numeric, 'S'),
    ('DEMO_OKGYE_C', '302동', 90.1::numeric, 'S'),
    ('DEMO_OKGYE_C', '303동', 45.0::numeric, 'D')
) AS v(kapt, dong_name, score, grade)
JOIN apt_complex_master c ON c.kapt_code = v.kapt
JOIN apt_building_master b
     ON b.complex_id = c.complex_id AND b.dong_name = v.dong_name
ON CONFLICT (building_id, weight_preset) DO NOTHING;

COMMIT;
