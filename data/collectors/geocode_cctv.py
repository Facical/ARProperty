"""raw CCTV xlsx의 주소를 카카오 주소→좌표 API로 변환 → safety/cctv.csv에 lat/lon 채움"""

import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

import openpyxl

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


def kakao_address_to_coord(query: str):
    """카카오 주소 API 호출. 성공 시 (lat, lon, road_addr) 튜플, 실패 시 None."""
    params = {"query": query, "size": 1}
    url = "https://dapi.kakao.com/v2/local/search/address.json?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={
        "Authorization": f"KakaoAK {KAKAO_KEY}",
        "User-Agent": "ar-property-collector/0.1",
    })
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        return None, str(e)
    docs = data.get("documents") or []
    if not docs:
        return None, "no_result"
    d = docs[0]
    road = d.get("road_address") or {}
    lat = d.get("y")
    lon = d.get("x")
    road_addr = (road.get("address_name") if road else "") or d.get("address_name") or ""
    if not lat or not lon:
        return None, "no_coord"
    return (float(lat), float(lon), road_addr), None


STD_COLS = [
    "category", "sub_category", "name", "lat", "lon",
    "address", "phone", "operator", "source_type", "source_id", "data_source",
]


def main():
    raw_xlsx = ROOT / "raw" / "구미시_생활방범cctv_전체데이터O_20260513_102038.xlsx"
    if not raw_xlsx.exists():
        sys.exit(f"raw CCTV xlsx 없음: {raw_xlsx}")

    wb = openpyxl.load_workbook(raw_xlsx, read_only=True, data_only=True)
    ws = wb.active

    total = 0
    rows = []
    failures = []
    seen = set()

    for i, row in enumerate(ws.iter_rows(values_only=True)):
        if i == 0:
            continue
        if not row or row[0] is None:
            continue
        sn = row[0]
        emd = (row[1] or "").strip()
        old_addr = (row[2] or "").strip()
        new_addr = (row[3] or "").strip()
        detail_loc = (row[4] or "").strip()
        manage_no = (row[6] or "").strip()
        key = (str(sn), new_addr or old_addr)
        if key in seen:
            continue
        seen.add(key)
        total += 1

        name = manage_no if manage_no else f"CCTV-{sn}"

        # 도로명 주소 우선 → 지번 주소 → 동명만 + 지번
        candidates = []
        if new_addr:
            candidates.append(f"경상북도 구미시 {new_addr}")
        if old_addr:
            candidates.append(f"경상북도 구미시 {emd} {old_addr}".strip())
            candidates.append(f"경상북도 구미시 {old_addr}")

        result = None
        last_err = None
        used_query = ""
        for q in candidates:
            result, err = kakao_address_to_coord(q)
            time.sleep(0.04)
            if result:
                used_query = q
                break
            last_err = err

        if result is None:
            failures.append({
                "sn": sn, "emd": emd, "old": old_addr, "new": new_addr,
                "reason": last_err or "no_match",
            })
            # 좌표 없는 행도 포함은 시키되 lat/lon 빈 채로 저장
            rows.append({
                "category": "safety",
                "sub_category": "cctv",
                "name": name,
                "lat": "", "lon": "",
                "address": f"{emd} {new_addr or old_addr} {detail_loc}".strip(),
                "phone": "", "operator": "",
                "source_type": "gumi_opendata_xlsx",
                "source_id": str(sn),
                "data_source": "gumi_opendata",
            })
        else:
            lat, lon, road_addr = result
            rows.append({
                "category": "safety",
                "sub_category": "cctv",
                "name": name,
                "lat": lat, "lon": lon,
                "address": road_addr or (new_addr or old_addr),
                "phone": "", "operator": "",
                "source_type": "gumi_opendata_xlsx",
                "source_id": str(sn),
                "data_source": "gumi_opendata",
            })

        if total % 100 == 0:
            ok = total - len(failures)
            print(f"  [{total:>4}] processed (ok={ok}, fail={len(failures)})", flush=True)

    # 저장
    out_path = ROOT / "safety/cctv.csv"
    with open(out_path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=STD_COLS)
        w.writeheader()
        w.writerows(rows)

    # 실패 로그
    fail_log = ROOT / "archive" / "cctv_geocode_failures.csv"
    fail_log.parent.mkdir(exist_ok=True)
    if failures:
        with open(fail_log, "w", encoding="utf-8-sig", newline="") as f:
            w = csv.DictWriter(f, fieldnames=["sn", "emd", "old", "new", "reason"])
            w.writeheader()
            w.writerows(failures)

    ok = total - len(failures)
    print(f"\n==== CCTV Geocoding Summary ====")
    print(f"total:     {total}")
    print(f"succeeded: {ok} ({ok/max(total,1)*100:.1f}%)")
    print(f"failed:    {len(failures)} -> {fail_log if failures else '(none)'}")


if __name__ == "__main__":
    main()
