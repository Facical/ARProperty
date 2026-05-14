"""OpenStreetMap(Overpass API)에서 구미시 편의시설 9종 일괄 수집 → 카테고리별 CSV 저장"""

import csv
import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

OUTPUT_DIR = Path(__file__).resolve().parents[1] / "편의시설" / "osm"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

OVERPASS_ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass.openstreetmap.fr/api/interpreter",
]

QUERY = """
[out:json][timeout:180];
area["name"="구미시"]["admin_level"="6"]->.gumi;
(
  nwr["amenity"~"^(hospital|clinic|doctors|pharmacy)$"](area.gumi);
  nwr["healthcare"~"^(hospital|clinic|doctor|pharmacy)$"](area.gumi);
  nwr["amenity"~"^(school|kindergarten|college|university)$"](area.gumi);
  nwr["amenity"="cram_school"](area.gumi);
  nwr["shop"~"^(convenience|supermarket|mall|department_store)$"](area.gumi);
  nwr["amenity"~"^(police|fire_station)$"](area.gumi);
  nwr["amenity"~"^(library|community_centre|arts_centre|theatre|cinema)$"](area.gumi);
  nwr["leisure"~"^(park|garden)$"](area.gumi);
);
out center tags;
"""

CATEGORY_MAP = [
    # (predicate(tags) -> bool, category, sub_category, output filename suffix)
    (lambda t: t.get("amenity") in ("hospital", "clinic", "doctors") or t.get("healthcare") in ("hospital", "clinic", "doctor"),
     "medical", "hospital", "medical_hospital.csv"),
    (lambda t: t.get("amenity") == "pharmacy" or t.get("healthcare") == "pharmacy",
     "medical", "pharmacy", "medical_pharmacy.csv"),
    (lambda t: t.get("amenity") == "school" and (t.get("school:type") or t.get("isced:level") or t.get("school") or "").lower() in ("elementary", "1", "primary"),
     "education", "elementary_school", "education_school_elementary.csv"),
    (lambda t: t.get("amenity") == "school",
     "education", "school", "education_school_all.csv"),
    (lambda t: t.get("amenity") == "kindergarten",
     "education", "kindergarten_or_daycare", "education_kindergarten.csv"),
    (lambda t: t.get("amenity") == "cram_school",
     "education", "academy", "education_academy.csv"),
    (lambda t: t.get("shop") in ("convenience",),
     "convenience", "convenience_store", "convenience_store.csv"),
    (lambda t: t.get("shop") in ("supermarket", "mall", "department_store"),
     "convenience", "supermarket_or_mart", "convenience_mart.csv"),
    (lambda t: t.get("amenity") == "police",
     "safety", "police_station", "safety_police.csv"),
    (lambda t: t.get("amenity") == "fire_station",
     "safety", "fire_station", "safety_fire.csv"),
    (lambda t: t.get("leisure") in ("park", "garden"),
     "leisure", "park", "leisure_park.csv"),
    (lambda t: t.get("amenity") == "library",
     "leisure", "library", "leisure_library.csv"),
    (lambda t: t.get("amenity") in ("community_centre", "arts_centre", "theatre", "cinema"),
     "leisure", "cultural_center", "leisure_cultural.csv"),
]


def overpass_call(query: str) -> dict:
    body = urllib.parse.urlencode({"data": query}).encode("utf-8")
    last_err = None
    for url in OVERPASS_ENDPOINTS:
        try:
            print(f"[overpass] POST {url}", flush=True)
            req = urllib.request.Request(url, data=body, headers={"User-Agent": "ar-property-collector/0.1"})
            with urllib.request.urlopen(req, timeout=240) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception as e:
            print(f"[overpass] failed: {e}", flush=True)
            last_err = e
            time.sleep(2)
    raise RuntimeError(f"All Overpass endpoints failed: {last_err}")


def extract_latlon(el: dict):
    if el.get("type") == "node":
        return el.get("lat"), el.get("lon")
    c = el.get("center")
    if c:
        return c.get("lat"), c.get("lon")
    return None, None


def build_address(tags: dict) -> str:
    parts = []
    for k in ("addr:province", "addr:city", "addr:district", "addr:street", "addr:housenumber"):
        v = tags.get(k)
        if v:
            parts.append(v)
    if parts:
        return " ".join(parts)
    return tags.get("addr:full") or ""


def main():
    print("[1/3] Overpass query...", flush=True)
    data = overpass_call(QUERY)
    elements = data.get("elements", [])
    print(f"[1/3] received {len(elements)} elements", flush=True)

    print("[2/3] classifying...", flush=True)
    buckets: dict[str, list[dict]] = {}
    seen: dict[str, set] = {}
    for el in elements:
        tags = el.get("tags") or {}
        lat, lon = extract_latlon(el)
        if lat is None or lon is None:
            continue
        name = tags.get("name") or tags.get("name:ko") or tags.get("operator")
        if not name:
            continue
        for predicate, category, sub_category, filename in CATEGORY_MAP:
            try:
                if not predicate(tags):
                    continue
            except Exception:
                continue
            # dedupe within bucket by (name, round(lat,6), round(lon,6))
            key = (name.strip(), round(float(lat), 6), round(float(lon), 6))
            seen.setdefault(filename, set())
            if key in seen[filename]:
                continue
            seen[filename].add(key)
            buckets.setdefault(filename, []).append({
                "category": category,
                "sub_category": sub_category,
                "name": name.strip(),
                "lat": lat,
                "lon": lon,
                "address": build_address(tags),
                "phone": tags.get("phone") or tags.get("contact:phone") or "",
                "operator": tags.get("operator") or "",
                "osm_type": el.get("type"),
                "osm_id": el.get("id"),
                "data_source": "manual",  # OSM은 4종 enum에 없어 임시 'manual'로 표시; 적재 시 검토 필요
            })

    print("[3/3] writing CSVs to", OUTPUT_DIR, flush=True)
    totals = []
    for predicate, category, sub_category, filename in CATEGORY_MAP:
        rows = buckets.get(filename, [])
        path = OUTPUT_DIR / filename
        with open(path, "w", encoding="utf-8-sig", newline="") as f:
            w = csv.DictWriter(f, fieldnames=[
                "category", "sub_category", "name", "lat", "lon",
                "address", "phone", "operator", "osm_type", "osm_id", "data_source",
            ])
            w.writeheader()
            w.writerows(rows)
        totals.append((filename, len(rows)))
        print(f"  - {filename}: {len(rows)} rows", flush=True)

    print()
    print("==== SUMMARY ====")
    for fn, n in totals:
        print(f"{fn:40s} {n:6d}")
    print(f"Total unique POIs written: {sum(n for _, n in totals)}")


if __name__ == "__main__":
    sys.exit(main())
