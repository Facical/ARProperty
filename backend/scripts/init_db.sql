-- =============================================================
-- ARProperty DB 초기화 스크립트
-- PostgreSQL 16 + PostGIS 3.4
-- =============================================================

-- PostGIS 확장 활성화
CREATE EXTENSION IF NOT EXISTS postgis;

-- =============================================================
-- 1. apt_complex_master (아파트 단지 마스터)
-- =============================================================
CREATE TABLE IF NOT EXISTS apt_complex_master (
    complex_id        SERIAL PRIMARY KEY,
    kapt_code         VARCHAR(20) UNIQUE,               -- 공동주택단지코드
    reb_complex_id    VARCHAR(20),                       -- 한국부동산원 단지고유번호
    complex_name      VARCHAR(100) NOT NULL,             -- 단지명
    legal_dong_code   CHAR(10) NOT NULL,                 -- 법정동코드 10자리
    sigungu_cd        CHAR(5) NOT NULL DEFAULT '47190',  -- 시군구코드
    bjdong_cd         CHAR(5) NOT NULL,                  -- 법정동코드 뒤5자리
    road_address      VARCHAR(200),                      -- 도로명주소
    parcel_address    VARCHAR(200),                      -- 지번주소
    households        INTEGER,                           -- 총 세대수
    building_count    INTEGER,                           -- 동 수
    completion_date   DATE,                              -- 사용승인일/준공일
    constructor       VARCHAR(100),                      -- 시공사
    heating_type      VARCHAR(50),                       -- 난방방식
    management_type   VARCHAR(50),                       -- 관리방식
    parking_count     INTEGER,                           -- 총 주차대수
    elevator_count    INTEGER,                           -- 총 승강기대수
    centroid          GEOMETRY(POINT, 4326),             -- 단지 중심 좌표
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_complex_kapt_code ON apt_complex_master(kapt_code);
CREATE INDEX IF NOT EXISTS idx_complex_dong_code ON apt_complex_master(legal_dong_code);
CREATE INDEX IF NOT EXISTS idx_complex_sigungu ON apt_complex_master(sigungu_cd);
CREATE INDEX IF NOT EXISTS idx_complex_centroid ON apt_complex_master USING GIST(centroid);
CREATE INDEX IF NOT EXISTS idx_complex_name ON apt_complex_master(complex_name);

-- =============================================================
-- 2. apt_building_master (아파트 동 마스터)
-- =============================================================
CREATE TABLE IF NOT EXISTS apt_building_master (
    building_id       SERIAL PRIMARY KEY,
    complex_id        INTEGER NOT NULL REFERENCES apt_complex_master(complex_id) ON DELETE CASCADE,
    dong_name         VARCHAR(50) NOT NULL,               -- 동명 (101동, 102동 등)
    polygon_geom      GEOMETRY(POLYGON, 4326),            -- 건물 폴리곤 (Vworld/GIS)
    centroid          GEOMETRY(POINT, 4326),               -- 건물 중심 좌표
    ground_floors     INTEGER,                             -- 지상 층수
    underground_floors INTEGER DEFAULT 0,                  -- 지하 층수
    highest_floor     INTEGER,                             -- 최고층
    building_height   NUMERIC(8,2),                        -- 건물 높이(m) - 층 추정용
    structure_type    VARCHAR(50),                         -- 구조 (RC, SRC 등)
    total_area        NUMERIC(12,2),                       -- 연면적(m2)
    use_approval_date DATE,                                -- 사용승인일
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(complex_id, dong_name)
);

CREATE INDEX IF NOT EXISTS idx_building_complex ON apt_building_master(complex_id);
CREATE INDEX IF NOT EXISTS idx_building_polygon ON apt_building_master USING GIST(polygon_geom);
CREATE INDEX IF NOT EXISTS idx_building_centroid ON apt_building_master USING GIST(centroid);
CREATE INDEX IF NOT EXISTS idx_building_dong ON apt_building_master(dong_name);

-- =============================================================
-- 3. apt_trade_history (거래 이력)
-- =============================================================
CREATE TABLE IF NOT EXISTS apt_trade_history (
    trade_id          SERIAL PRIMARY KEY,
    complex_id        INTEGER NOT NULL REFERENCES apt_complex_master(complex_id) ON DELETE CASCADE,
    dong_name         VARCHAR(50),                         -- 동명
    floor             INTEGER,                             -- 층
    exclusive_area    NUMERIC(8,2),                        -- 전용면적(m2)
    deal_amount       INTEGER,                             -- 거래금액(만원) - 매매
    deposit           INTEGER,                             -- 보증금(만원) - 전월세
    monthly_rent      INTEGER,                             -- 월세(만원) - 월세
    deal_date         DATE NOT NULL,                       -- 계약일
    deal_year         INTEGER NOT NULL,                    -- 계약년도
    deal_month        INTEGER NOT NULL,                    -- 계약월
    trade_type        VARCHAR(10) NOT NULL                 -- 매매/전세/월세
                      CHECK (trade_type IN ('매매','전세','월세')),
    building_year     INTEGER,                             -- 건축년도
    jibun             VARCHAR(20),                         -- 지번
    apt_name          VARCHAR(100),                        -- API 응답 단지명 (원본)
    dealing_type      VARCHAR(20),                         -- 중개/직거래
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trade_complex ON apt_trade_history(complex_id);
CREATE INDEX IF NOT EXISTS idx_trade_date ON apt_trade_history(deal_date DESC);
CREATE INDEX IF NOT EXISTS idx_trade_type ON apt_trade_history(trade_type);
CREATE INDEX IF NOT EXISTS idx_trade_dong_floor ON apt_trade_history(complex_id, dong_name, floor);
CREATE INDEX IF NOT EXISTS idx_trade_area ON apt_trade_history(exclusive_area);
CREATE INDEX IF NOT EXISTS idx_trade_year_month ON apt_trade_history(deal_year, deal_month);

-- =============================================================
-- 4. living_infra_gumi (구미시 생활 인프라)
-- =============================================================
CREATE TABLE IF NOT EXISTS living_infra_gumi (
    infra_id          SERIAL PRIMARY KEY,
    category          VARCHAR(20) NOT NULL                 -- 대분류
                      CHECK (category IN ('medical','education','convenience','transport','safety','leisure')),
    sub_category      VARCHAR(50) NOT NULL,                -- 소분류 (hospital, pharmacy, bus_stop 등)
    name              VARCHAR(200) NOT NULL,               -- 시설명
    point_geom        GEOMETRY(POINT, 4326) NOT NULL,      -- 좌표
    address           VARCHAR(300),                        -- 주소
    detail_json       JSONB,                               -- 부가 정보 (운영시간, 전화번호 등)
    data_source       VARCHAR(50) NOT NULL                 -- 데이터 출처
                      CHECK (data_source IN ('kakao','gumi_opendata','data_go_kr','manual')),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_infra_geom ON living_infra_gumi USING GIST(point_geom);
CREATE INDEX IF NOT EXISTS idx_infra_category ON living_infra_gumi(category);
CREATE INDEX IF NOT EXISTS idx_infra_sub_category ON living_infra_gumi(sub_category);
CREATE INDEX IF NOT EXISTS idx_infra_source ON living_infra_gumi(data_source);

-- =============================================================
-- 5. building_livability_score (건물별 편의시설 점수 캐시)
-- =============================================================
CREATE TABLE IF NOT EXISTS building_livability_score (
    score_id          SERIAL PRIMARY KEY,
    building_id       INTEGER NOT NULL REFERENCES apt_building_master(building_id) ON DELETE CASCADE,
    total_score       NUMERIC(5,1) NOT NULL,               -- 종합 점수 (0~100)
    grade             CHAR(1) NOT NULL                     -- S/A/B/C/D/F
                      CHECK (grade IN ('S','A','B','C','D','F')),
    medical_score     NUMERIC(5,1) DEFAULT 0,              -- 의료 점수
    education_score   NUMERIC(5,1) DEFAULT 0,              -- 교육 점수
    convenience_score NUMERIC(5,1) DEFAULT 0,              -- 생활편의 점수
    transport_score   NUMERIC(5,1) DEFAULT 0,              -- 교통 점수
    safety_score      NUMERIC(5,1) DEFAULT 0,              -- 안전 점수
    leisure_score     NUMERIC(5,1) DEFAULT 0,              -- 여가 점수
    weight_preset     VARCHAR(20) DEFAULT 'default',       -- 가중치 프리셋
    nearest_json      JSONB,                               -- 카테고리별 최근접 시설 상세
    calculated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        TIMESTAMP NOT NULL,                  -- 캐시 만료 시점
    UNIQUE(building_id, weight_preset)
);

CREATE INDEX IF NOT EXISTS idx_score_building ON building_livability_score(building_id);
CREATE INDEX IF NOT EXISTS idx_score_expires ON building_livability_score(expires_at);
CREATE INDEX IF NOT EXISTS idx_score_grade ON building_livability_score(grade);

-- =============================================================
-- 6. bjdong_code_mapping (법정동코드 매핑 - 보조 테이블)
-- =============================================================
CREATE TABLE IF NOT EXISTS bjdong_code_mapping (
    code_id           SERIAL PRIMARY KEY,
    legal_dong_code   CHAR(10) UNIQUE NOT NULL,            -- 법정동코드 10자리
    sido_name         VARCHAR(20) NOT NULL,                -- 시도명
    sigungu_name      VARCHAR(20) NOT NULL,                -- 시군구명
    dong_name         VARCHAR(30) NOT NULL,                -- 읍면동명
    is_active         BOOLEAN DEFAULT TRUE,                -- 활성 여부
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bjdong_code ON bjdong_code_mapping(legal_dong_code);

-- =============================================================
-- 7. api_call_log (API 호출 로그 - 호출 제한 관리용)
-- =============================================================
CREATE TABLE IF NOT EXISTS api_call_log (
    log_id            SERIAL PRIMARY KEY,
    api_name          VARCHAR(50) NOT NULL,                 -- trade_api, rent_api, building_register 등
    call_date         DATE NOT NULL DEFAULT CURRENT_DATE,
    call_count        INTEGER DEFAULT 0,
    daily_limit       INTEGER NOT NULL,                     -- 일일 한도
    UNIQUE(api_name, call_date)
);

CREATE INDEX IF NOT EXISTS idx_api_log_date ON api_call_log(api_name, call_date);

-- =============================================================
-- 초기 데이터: 구미시 법정동코드
-- =============================================================
INSERT INTO bjdong_code_mapping (legal_dong_code, sido_name, sigungu_name, dong_name) VALUES
('4719010100', '경상북도', '구미시', '원평동'),
('4719010200', '경상북도', '구미시', '지산동'),
('4719010300', '경상북도', '구미시', '도량동'),
('4719010400', '경상북도', '구미시', '형곡동'),
('4719010500', '경상북도', '구미시', '신평동'),
('4719010600', '경상북도', '구미시', '비산동'),
('4719010700', '경상북도', '구미시', '개포동'),
('4719010800', '경상북도', '구미시', '송정동'),
('4719010900', '경상북도', '구미시', '수점동'),
('4719011000', '경상북도', '구미시', '원남동'),
('4719011100', '경상북도', '구미시', '공단동'),
('4719011300', '경상북도', '구미시', '광평동'),
('4719011400', '경상북도', '구미시', '상모동'),
('4719011500', '경상북도', '구미시', '임은동'),
('4719011600', '경상북도', '구미시', '임수동'),
('4719011700', '경상북도', '구미시', '봉곡동'),
('4719011800', '경상북도', '구미시', '남통동'),
('4719011900', '경상북도', '구미시', '인의동'),
('4719012000', '경상북도', '구미시', '양포동'),
('4719012100', '경상북도', '구미시', '황상동'),
('4719012200', '경상북도', '구미시', '오태동'),
('4719012300', '경상북도', '구미시', '구평동'),
('4719012400', '경상북도', '구미시', '사곡동'),
('4719012500', '경상북도', '구미시', '신동'),
('4719012600', '경상북도', '구미시', '인동동'),
('4719012700', '경상북도', '구미시', '진미동'),
('4719012800', '경상북도', '구미시', '옥계동'),
('4719025000', '경상북도', '구미시', '산동읍'),
('4719025300', '경상북도', '구미시', '선산읍'),
('4719031000', '경상북도', '구미시', '고아읍')
ON CONFLICT (legal_dong_code) DO NOTHING;

-- API 호출 제한 초기 설정
INSERT INTO api_call_log (api_name, call_date, call_count, daily_limit) VALUES
('trade_api', CURRENT_DATE, 0, 10000),
('rent_api', CURRENT_DATE, 0, 10000),
('building_register', CURRENT_DATE, 0, 10000),
('apt_list', CURRENT_DATE, 0, 10000),
('apt_info', CURRENT_DATE, 0, 10000),
('vworld_geocode', CURRENT_DATE, 0, 40000),
('vworld_data', CURRENT_DATE, 0, 40000),
('kakao_local', CURRENT_DATE, 0, 30000)
ON CONFLICT (api_name, call_date) DO NOTHING;
