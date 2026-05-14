"""data/편의시설/*/*.csv → living_infra_gumi 테이블 적재 (Docker PG 5433)"""

import csv
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "편의시설"
SQL_OUT = Path(__file__).resolve().parents[1] / "exports" / "load_living_infra.sql"
SQL_OUT.parent.mkdir(exist_ok=True)

ALLOWED_CATEGORIES = {"medical", "education", "convenience", "transport", "safety", "leisure"}
ALLOWED_SOURCES = {"kakao", "gumi_opendata", "data_go_kr", "manual"}

CONTAINER = "arproperty-db"
DB_USER = "arproperty"
DB_NAME = "arproperty"
BATCH = 500


def is_number(v):
    if v is None or v == "":
        return False
    try:
        float(v); return True
    except (ValueError, TypeError):
        return False


def sql_escape(s) -> str:
    if s is None:
        return ""
    return str(s).replace("'", "''")


def collect_rows():
    rows = []
    skipped = {"missing_coord": 0, "missing_name": 0, "bad_category": 0, "bad_data_source": 0}
    for csv_path in sorted(p for p in ROOT.rglob("*.csv") if "archive" not in p.parts):
        with open(csv_path, "r", encoding="utf-8-sig", newline="") as f:
            for r in csv.DictReader(f):
                if not is_number(r.get("lat")) or not is_number(r.get("lon")):
                    skipped["missing_coord"] += 1; continue
                name = (r.get("name") or "").strip()
                if not name:
                    skipped["missing_name"] += 1; continue
                cat = (r.get("category") or "").strip()
                if cat not in ALLOWED_CATEGORIES:
                    skipped["bad_category"] += 1; continue
                ds = (r.get("data_source") or "").strip()
                if ds not in ALLOWED_SOURCES:
                    skipped["bad_data_source"] += 1; continue
                detail = {}
                for k in ("phone", "operator", "source_type", "source_id"):
                    v = (r.get(k) or "").strip()
                    if v:
                        detail[k] = v
                rows.append({
                    "category": cat,
                    "sub_category": (r.get("sub_category") or "").strip()[:50],
                    "name": name[:200],
                    "lat": float(r["lat"]),
                    "lon": float(r["lon"]),
                    "address": (r.get("address") or "").strip()[:300],
                    "detail_json": json.dumps(detail, ensure_ascii=False) if detail else None,
                    "data_source": ds,
                })
    return rows, skipped


def build_sql(rows):
    lines = [
        "-- 편의시설 living_infra_gumi 적재 (자동생성)",
        "BEGIN;",
        "TRUNCATE TABLE living_infra_gumi RESTART IDENTITY;",
        "",
    ]
    for i in range(0, len(rows), BATCH):
        chunk = rows[i:i+BATCH]
        lines.append("INSERT INTO living_infra_gumi (category, sub_category, name, point_geom, address, detail_json, data_source) VALUES")
        values = []
        for r in chunk:
            addr_sql = "NULL" if not r["address"] else f"'{sql_escape(r['address'])}'"
            detail_sql = "NULL" if not r["detail_json"] else f"'{sql_escape(r['detail_json'])}'::jsonb"
            values.append(
                "('{cat}','{sub}','{name}',ST_SetSRID(ST_MakePoint({lon},{lat}),4326),{addr},{detail},'{ds}')".format(
                    cat=sql_escape(r["category"]),
                    sub=sql_escape(r["sub_category"]),
                    name=sql_escape(r["name"]),
                    lon=r["lon"], lat=r["lat"],
                    addr=addr_sql, detail=detail_sql,
                    ds=sql_escape(r["data_source"]),
                )
            )
        lines.append(",\n".join(values) + ";")
        lines.append("")
    lines.append("COMMIT;")
    lines.append("")
    lines.append("SELECT category, COUNT(*) FROM living_infra_gumi GROUP BY category ORDER BY 1;")
    lines.append("SELECT data_source, COUNT(*) FROM living_infra_gumi GROUP BY data_source ORDER BY 1;")
    lines.append("SELECT COUNT(*) AS total FROM living_infra_gumi;")
    return "\n".join(lines)


def main():
    print("[1/4] reading CSVs...")
    rows, skipped = collect_rows()
    print(f"  total valid rows: {len(rows)}")
    for k, v in skipped.items():
        print(f"  skipped {k}: {v}")

    print(f"\n[2/4] writing SQL to {SQL_OUT}")
    sql = build_sql(rows)
    SQL_OUT.write_text(sql, encoding="utf-8")
    print(f"  {SQL_OUT.stat().st_size:,} bytes")

    print(f"\n[3/4] copying SQL into container...")
    subprocess.run(["docker", "cp", str(SQL_OUT), f"{CONTAINER}:/tmp/load_living_infra.sql"], check=True)

    print(f"\n[4/4] executing via psql...")
    result = subprocess.run(
        ["docker", "exec", "-i", CONTAINER, "psql", "-U", DB_USER, "-d", DB_NAME,
         "-v", "ON_ERROR_STOP=1", "-f", "/tmp/load_living_infra.sql"],
        capture_output=True, text=True, encoding="utf-8",
    )
    print("--- stdout (tail) ---")
    print("\n".join(result.stdout.splitlines()[-30:]))
    if result.returncode != 0:
        print("--- stderr ---")
        print(result.stderr)
        sys.exit(result.returncode)


if __name__ == "__main__":
    main()
