"""Kakao Local API에서 구미시 편의시설 부족분(약국/어린이집/학원/소방서) 수집 → 카테고리별 CSV"""

import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

OUTPUT_DIR = Path(__file__).resolve().parents[1] / "편의시설" / "kakao"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# 환경변수 우선, 없으면 .env 자동 로드
def load_env_from_dotenv():
    env_path = Path(__file__).resolve().parents[2] / ".env"
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip())

load_env_from_dotenv()
KAKAO_KEY = os.environ.get("KAKAO_REST_API_KEY", "").strip()
if not KAKAO_KEY:
    sys.exit("KAKAO_REST_API_KEY 환경변수가 없습니다. .env를 확인하세요.")

# 구미시 영역 — 동서 약 30km, 남북 약 35km. 카카오 카테고리 검색 반경 최대 20km.
# 6km 간격 격자(반경 10km)로 sweep해서 누락 방지 + 중복은 kakao_id로 dedupe.
GUMI_BBOX = {"min_lat": 35.95, "max_lat": 36.30, "min_lon": 128.18, "max_lon": 128.55}
GRID_STEP_DEG = 0.05  # 약 5.5km, 반경 10km로 충분히 겹침
SEARCH_RADIUS_M = 10000

# 카테고리 검색 대상
CATEGORY_TARGETS = [
    # (category_group_code, our_category, our_sub_category, output_filename)
    ("PM9", "medical", "pharmacy", "medical_pharmacy.csv"),
    ("PS3", "education", "kindergarten_or_daycare", "education_kindergarten.csv"),
    ("AC5", "education", "academy", "education_academy.csv"),
]

# 키워드 검색 대상 (소방서 — 카카오 category_group_code에 소방서가 없어서 키워드)
KEYWORD_TARGETS = [
    # (keyword, our_category, our_sub_category, output_filename)
    ("구미 119안전센터", "safety", "fire_station", "safety_fire_safety_center.csv"),
    ("구미 소방서", "safety", "fire_station", "safety_fire_station.csv"),
]


def kakao_get(path: str, params: dict) -> dict:
    qs = urllib.parse.urlencode(params)
    url = f"https://dapi.kakao.com{path}?{qs}"
    req = urllib.request.Request(url, headers={
        "Authorization": f"KakaoAK {KAKAO_KEY}",
        "User-Agent": "ar-property-collector/0.1",
    })
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def sweep_grid():
    lat = GUMI_BBOX["min_lat"]
    while lat <= GUMI_BBOX["max_lat"] + 1e-9:
        lon = GUMI_BBOX["min_lon"]
        while lon <= GUMI_BBOX["max_lon"] + 1e-9:
            yield round(lat, 6), round(lon, 6)
            lon += GRID_STEP_DEG
        lat += GRID_STEP_DEG


def fetch_category(code: str, center_lat: float, center_lon: float) -> list:
    out = []
    for page in range(1, 46):
        params = {
            "category_group_code": code,
            "x": center_lon,
            "y": center_lat,
            "radius": SEARCH_RADIUS_M,
            "size": 15,
            "page": page,
            "sort": "distance",
        }
        data = kakao_get("/v2/local/search/category.json", params)
        docs = data.get("documents", [])
        out.extend(docs)
        meta = data.get("meta") or {}
        if meta.get("is_end") or not docs:
            break
        time.sleep(0.05)
    return out


def fetch_keyword(keyword: str) -> list:
    out = []
    for page in range(1, 46):
        params = {"query": keyword, "size": 15, "page": page}
        data = kakao_get("/v2/local/search/keyword.json", params)
        docs = data.get("documents", [])
        out.extend(docs)
        meta = data.get("meta") or {}
        if meta.get("is_end") or not docs:
            break
        time.sleep(0.05)
    return out


def is_in_gumi(doc: dict) -> bool:
    addr = (doc.get("address_name") or "") + " " + (doc.get("road_address_name") or "")
    return "구미시" in addr


def to_row(doc: dict, category: str, sub_category: str) -> dict:
    return {
        "category": category,
        "sub_category": sub_category,
        "name": (doc.get("place_name") or "").strip(),
        "lat": doc.get("y"),
        "lon": doc.get("x"),
        "address": doc.get("road_address_name") or doc.get("address_name") or "",
        "phone": doc.get("phone") or "",
        "operator": "",
        "kakao_id": doc.get("id"),
        "category_name": doc.get("category_name") or "",
        "data_source": "kakao",
    }


def write_csv(filename: str, rows: list):
    path = OUTPUT_DIR / filename
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=[
            "category", "sub_category", "name", "lat", "lon",
            "address", "phone", "operator", "kakao_id", "category_name", "data_source",
        ])
        w.writeheader()
        w.writerows(rows)
    print(f"  - {filename}: {len(rows)} rows -> {path}", flush=True)


def main():
    totals = []

    # 1) 카테고리 검색 (격자 sweep)
    grid = list(sweep_grid())
    print(f"[grid] {len(grid)} sweep points (step={GRID_STEP_DEG}deg, radius={SEARCH_RADIUS_M}m)", flush=True)

    for code, cat, sub, filename in CATEGORY_TARGETS:
        print(f"\n[category] {code} ({cat}/{sub})", flush=True)
        seen_ids: set = set()
        rows: list = []
        for i, (lat, lon) in enumerate(grid, 1):
            try:
                docs = fetch_category(code, lat, lon)
            except Exception as e:
                print(f"  ! point ({lat},{lon}) error: {e}", flush=True)
                continue
            new = 0
            for d in docs:
                kid = d.get("id")
                if not kid or kid in seen_ids:
                    continue
                if not is_in_gumi(d):
                    continue
                seen_ids.add(kid)
                rows.append(to_row(d, cat, sub))
                new += 1
            print(f"  [{i:>3}/{len(grid)}] ({lat:.4f},{lon:.4f}) +{new} (total={len(rows)})", flush=True)
            time.sleep(0.05)
        write_csv(filename, rows)
        totals.append((filename, len(rows)))

    # 2) 키워드 검색 (소방서)
    for keyword, cat, sub, filename in KEYWORD_TARGETS:
        print(f"\n[keyword] '{keyword}' ({cat}/{sub})", flush=True)
        try:
            docs = fetch_keyword(keyword)
        except Exception as e:
            print(f"  ! error: {e}", flush=True)
            docs = []
        seen_ids = set()
        rows = []
        for d in docs:
            kid = d.get("id")
            if not kid or kid in seen_ids:
                continue
            if not is_in_gumi(d):
                continue
            seen_ids.add(kid)
            rows.append(to_row(d, cat, sub))
        write_csv(filename, rows)
        totals.append((filename, len(rows)))

    print("\n==== SUMMARY ====")
    for fn, n in totals:
        print(f"{fn:50s} {n:6d}")
    print(f"Total unique POIs written: {sum(n for _, n in totals)}")


if __name__ == "__main__":
    sys.exit(main())
