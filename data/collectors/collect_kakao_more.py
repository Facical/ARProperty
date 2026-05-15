"""카카오 Local API로 병원(HP8)·편의점(CS2) 재수집 → 기존 OSM 파일 교체"""

import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "편의시설"


def load_env():
    env_path = Path(__file__).resolve().parents[2] / ".env"
    if env_path.exists():
        for line in env_path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip())


load_env()
KAKAO_KEY = os.environ.get("KAKAO_REST_API_KEY", "").strip()
if not KAKAO_KEY:
    sys.exit("KAKAO_REST_API_KEY 없음")

GUMI_BBOX = {"min_lat": 35.95, "max_lat": 36.30, "min_lon": 128.18, "max_lon": 128.55}
GRID_STEP_DEG = 0.05
SEARCH_RADIUS_M = 10000

# (code, category, sub_category, output relative path under ROOT)
TARGETS = [
    ("HP8", "medical",     "hospital",          "medical/hospital.csv"),
    ("CS2", "convenience", "convenience_store", "convenience/convenience_store.csv"),
]

STD_COLS = [
    "category", "sub_category", "name", "lat", "lon",
    "address", "phone", "operator", "source_type", "source_id", "data_source",
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


def fetch_category(code: str, center_lat: float, center_lon: float):
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
        try:
            data = kakao_get("/v2/local/search/category.json", params)
        except Exception as e:
            print(f"  ! ({center_lat:.4f},{center_lon:.4f}) p{page} err: {e}")
            return out
        docs = data.get("documents", [])
        out.extend(docs)
        meta = data.get("meta") or {}
        if meta.get("is_end") or not docs:
            break
        time.sleep(0.04)
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
        "source_type": "kakao",
        "source_id": str(doc.get("id", "")),
        "data_source": "kakao",
    }


def main():
    grid = list(sweep_grid())
    print(f"[grid] {len(grid)} sweep points")

    archive_dir = ROOT / "archive"
    archive_dir.mkdir(exist_ok=True)

    for code, cat, sub, rel in TARGETS:
        print(f"\n[category] {code} ({cat}/{sub}) -> {rel}")
        seen_ids = set()
        rows = []
        for i, (lat, lon) in enumerate(grid, 1):
            docs = fetch_category(code, lat, lon)
            new = 0
            for d in docs:
                kid = d.get("id")
                if not kid or kid in seen_ids or not is_in_gumi(d):
                    continue
                seen_ids.add(kid)
                rows.append(to_row(d, cat, sub))
                new += 1
            if new > 0 or i % 8 == 0 or i == len(grid):
                print(f"  [{i:>3}/{len(grid)}] ({lat:.4f},{lon:.4f}) +{new} total={len(rows)}", flush=True)
            time.sleep(0.04)

        # 기존 OSM 파일을 archive로 백업 (이미 있으면 덮어쓰기)
        old_path = ROOT / rel
        if old_path.exists():
            backup = archive_dir / f"prev_osm_{old_path.name}"
            print(f"  [archive] {rel} -> archive/{backup.name}")
            old_path.replace(backup)

        # 새 카카오 결과 저장
        old_path.parent.mkdir(parents=True, exist_ok=True)
        with open(old_path, "w", encoding="utf-8-sig", newline="") as f:
            w = csv.DictWriter(f, fieldnames=STD_COLS)
            w.writeheader()
            w.writerows(rows)
        print(f"  [write] {rel}: {len(rows)} rows")


if __name__ == "__main__":
    main()
