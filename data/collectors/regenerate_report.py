"""모든 표준화 CSV를 재검증하고 REPORT.md 갱신"""

import csv
from pathlib import Path
from datetime import datetime

ROOT = Path(__file__).resolve().parents[1] / "편의시설"

STD_COLS = ["category", "sub_category", "name", "lat", "lon",
            "address", "phone", "operator", "source_type", "source_id", "data_source"]
ALLOWED_CATEGORIES = {"medical", "education", "convenience", "transport", "safety", "leisure"}
ALLOWED_SOURCES = {"kakao", "gumi_opendata", "data_go_kr", "manual"}


def read_csv(path: Path):
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def is_number(v):
    if v is None or v == "":
        return False
    try:
        float(v); return True
    except (ValueError, TypeError):
        return False


def validate(rows):
    n = len(rows)
    miss_lat = sum(1 for r in rows if not is_number(r.get("lat")))
    miss_lon = sum(1 for r in rows if not is_number(r.get("lon")))
    miss_name = sum(1 for r in rows if not (r.get("name") or "").strip())
    miss_addr = sum(1 for r in rows if not (r.get("address") or "").strip())
    bad_cat = sum(1 for r in rows if r.get("category") not in ALLOWED_CATEGORIES)
    bad_src = sum(1 for r in rows if r.get("data_source") not in ALLOWED_SOURCES)
    bad_coord = 0
    for r in rows:
        if is_number(r.get("lat")) and is_number(r.get("lon")):
            la = float(r["lat"]); lo = float(r["lon"])
            if not (33 <= la <= 39 and 124 <= lo <= 132):
                bad_coord += 1
    return {"total": n, "missing_lat": miss_lat, "missing_lon": miss_lon,
            "missing_name": miss_name, "missing_address": miss_addr,
            "invalid_category": bad_cat, "invalid_data_source": bad_src,
            "out_of_korea_bbox": bad_coord}


def main():
    files = sorted([p for p in ROOT.rglob("*.csv") if "archive" not in p.parts])
    summary = []
    for p in files:
        rows = read_csv(p)
        v = validate(rows)
        rel = str(p.relative_to(ROOT)).replace("\\", "/")
        summary.append((rel, v))

    md = ["# 편의시설 데이터 검증 리포트", "",
          f"생성 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}", "",
          "## 표준 컬럼", f"`{', '.join(STD_COLS)}`", "",
          "## DB 적재 필수 속성",
          "- `category` — CHECK enum (medical/education/convenience/transport/safety/leisure)",
          "- `sub_category` — 자유 문자열",
          "- `name` — NOT NULL",
          "- `lat`, `lon` — `point_geom`(POINT 4326)으로 변환, NOT NULL",
          "- `data_source` — CHECK enum (kakao/gumi_opendata/data_go_kr/manual)",
          "",
          "## 파일별 검증 결과", "",
          "| 파일 | 출처 | 행수 | lat 누락 | lon 누락 | name 누락 | address 누락 | 좌표이상 |",
          "|---|---|---:|---:|---:|---:|---:|---:|"]
    grand = 0
    src_label = {
        "medical/hospital.csv": "Kakao HP8",
        "medical/pharmacy.csv": "Kakao PM9",
        "education/school.csv": "OSM",
        "education/school_elementary_guess.csv": "OSM (분류 부정확)",
        "education/kindergarten.csv": "Kakao PS3",
        "education/academy.csv": "Kakao AC5",
        "convenience/convenience_store.csv": "Kakao CS2",
        "convenience/mart.csv": "OSM",
        "transport/bus_stop.csv": "구미시 xlsx",
        "safety/police.csv": "OSM",
        "safety/fire_station.csv": "Kakao keyword",
        "safety/cctv.csv": "구미시 xlsx + 카카오 지오코딩",
        "leisure/park.csv": "OSM",
        "leisure/library.csv": "OSM",
        "leisure/cultural.csv": "OSM",
    }
    for rel, v in summary:
        src = src_label.get(rel, "—")
        md.append(f"| `{rel}` | {src} | {v['total']} | {v['missing_lat']} | {v['missing_lon']} | {v['missing_name']} | {v['missing_address']} | {v['out_of_korea_bbox']} |")
        grand += v["total"]
    md.append(f"\n**합계: {grand}건**\n")

    # 필수 누락
    md.append("## 🔴 필수 속성 누락")
    miss = [(rel, v) for rel, v in summary if v["missing_lat"] > 0 or v["missing_lon"] > 0 or v["missing_name"] > 0]
    if not miss:
        md.append("좌표/이름 누락 행 없음. ✅\n")
    else:
        md.append("")
        for rel, v in miss:
            md.append(f"- `{rel}`: lat {v['missing_lat']} / lon {v['missing_lon']} / name {v['missing_name']} (총 {v['total']})")
        md.append("")

    # 보조 누락 (address)
    md.append("## 🟡 보조 속성(address) 누락 — 필수 아님")
    md.append("")
    addr_missing = [(rel, v) for rel, v in summary if v["missing_address"] > 0]
    for rel, v in sorted(addr_missing, key=lambda x: -x[1]["missing_address"]):
        pct = v["missing_address"] / max(v["total"], 1) * 100
        md.append(f"- `{rel}`: {v['missing_address']}/{v['total']} ({pct:.0f}%)")
    md.append("")

    md.append("## 📂 최종 폴더 구조")
    md.append("```")
    md.append("data/편의시설/")
    md.append("├── archive/             사용 안 하는 보조 데이터 + 지오코딩 실패 로그")
    md.append("├── medical/")
    md.append("│   ├── hospital.csv             (Kakao HP8)")
    md.append("│   └── pharmacy.csv             (Kakao PM9)")
    md.append("├── education/")
    md.append("│   ├── school.csv               (OSM)")
    md.append("│   ├── school_elementary_guess.csv  (OSM, 분류 부정확)")
    md.append("│   ├── kindergarten.csv         (Kakao PS3)")
    md.append("│   └── academy.csv              (Kakao AC5)")
    md.append("├── convenience/")
    md.append("│   ├── convenience_store.csv    (Kakao CS2)")
    md.append("│   └── mart.csv                 (OSM)")
    md.append("├── transport/")
    md.append("│   └── bus_stop.csv             (구미시 xlsx)")
    md.append("├── safety/")
    md.append("│   ├── police.csv               (OSM)")
    md.append("│   ├── fire_station.csv         (Kakao 키워드)")
    md.append("│   └── cctv.csv                 (구미시 xlsx + 카카오 지오코딩)")
    md.append("├── leisure/")
    md.append("│   ├── park.csv                 (OSM)")
    md.append("│   ├── library.csv              (OSM)")
    md.append("│   └── cultural.csv             (OSM)")
    md.append("└── REPORT.md")
    md.append("```")

    (ROOT / "REPORT.md").write_text("\n".join(md), encoding="utf-8")
    print(f"[report] {ROOT/'REPORT.md'} updated, grand total = {grand}")


if __name__ == "__main__":
    main()
