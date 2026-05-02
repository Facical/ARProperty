"""
옥계동(구미시) 건물 데이터 공공 API 조회 스크립트

[API 키 발급 방법]
1. https://www.data.go.kr 회원가입 및 로그인
2. 아래 3개 API 페이지에서 각각 "활용신청" 클릭 (자동승인, 10~60분 내 사용 가능)
   - 건축물대장정보: https://www.data.go.kr/data/15134735/openapi.do
   - 아파트 매매 실거래가: https://www.data.go.kr/data/15126469/openapi.do
   - 공동주택 단지 목록: https://www.data.go.kr/data/15057332/openapi.do
3. 마이페이지 > 데이터활용 > 인증키 발급현황 에서 인증키 복사
   (3개 API 모두 같은 인증키를 사용합니다)

[실행 방법]
pip install requests
python okgye_building_data.py
"""

import requests
from urllib.parse import quote_plus
import xml.etree.ElementTree as ET
import json

# ============================================================
# 구미시 / 옥계동 코드 정보
# ============================================================
SIGUNGU_CD = "47190"   # 구미시 시군구코드
BJDONG_CD = "12800"    # 옥계동 법정동코드 (5자리) - code.go.kr 확인: 4719012800


def get_api_key():
    print("=" * 60)
    print("  옥계동(구미시) 건물 데이터 공공 API 조회")
    print("=" * 60)
    print()
    key = input("공공데이터포털(data.go.kr) 인증키를 입력하세요: ").strip()
    if not key:
        print("API 키가 입력되지 않았습니다.")
        exit(1)
    return key


def build_url(base_url, service_key, params_dict):
    """serviceKey를 URL 인코딩하여 URL에 직접 붙입니다."""
    encoded_key = quote_plus(service_key)
    param_str = "&".join(f"{k}={v}" for k, v in params_dict.items())
    return f"{base_url}?serviceKey={encoded_key}&{param_str}"


def is_success_code(code):
    """공공데이터 API 성공 코드 체크 (API마다 00 또는 000 사용)"""
    return code in ("00", "000")


def extract_item_list(body):
    """응답 body에서 item 리스트를 추출합니다. (다양한 응답 구조 대응)"""
    items = body.get("items", {})
    if isinstance(items, list):
        return items
    if isinstance(items, dict):
        item_list = items.get("item", [])
        if isinstance(item_list, dict):
            return [item_list]
        return item_list
    return []


# ============================================================
# 1. 공동주택 단지 목록 조회 (먼저 실행하여 법정동코드 확인)
# ============================================================
def fetch_apt_list(service_key):
    """공동주택 단지 목록에서 옥계동 아파트 단지를 조회합니다."""
    print("\n" + "=" * 60)
    print("  [1] 공동주택 단지 목록 조회 (구미시 → 옥계동 필터)")
    print("=" * 60)

    base_url = "https://apis.data.go.kr/1613000/AptListService3/getSigunguAptList3"
    params = {
        "sigunguCode": SIGUNGU_CD,
        "numOfRows": "500",
        "pageNo": "1",
    }

    full_url = build_url(base_url, service_key, params)
    print(f"\n파라미터: sigunguCode={SIGUNGU_CD}")
    print(f"(구미시 전체 단지 조회 후 옥계동 필터링)\n")

    bjdong_cd = None  # 옥계동 법정동코드를 찾아서 반환

    try:
        resp = requests.get(full_url, timeout=10)
        print(f"HTTP 상태코드: {resp.status_code}")

        if resp.status_code != 200:
            print(f"HTTP 오류: {resp.status_code}")
            print(f"응답 내용: {resp.text[:500]}")
            return bjdong_cd

        data = resp.json()

        header = data.get("response", {}).get("header", {})
        result_code = header.get("resultCode")
        result_msg = header.get("resultMsg")
        print(f"응답 코드: {result_code} ({result_msg})")

        if not is_success_code(result_code):
            print(f"오류 발생: {result_msg}")
            print(f"전체 응답: {json.dumps(data, ensure_ascii=False, indent=2)}")
            return bjdong_cd

        body = data.get("response", {}).get("body", {})
        total_count = body.get("totalCount", 0)
        print(f"구미시 전체 단지 수: {total_count}")

        item_list = extract_item_list(body)

        # 옥계동 필터링
        okgye_items = [
            item for item in item_list
            if "옥계" in str(item.get("as3", ""))
        ]

        print(f"옥계동 단지 수: {len(okgye_items)}\n")

        if not okgye_items:
            print("옥계동 아파트 단지가 없습니다.")
            dong_names = set()
            for item in item_list:
                dong = str(item.get("as3", "")).strip()
                if dong:
                    dong_names.add(dong)
            if dong_names:
                print(f"\n구미시 내 동 목록: {', '.join(sorted(dong_names))}")
            return bjdong_cd

        for i, item in enumerate(okgye_items, 1):
            print(f"--- 단지 {i} ---")
            print(f"  단지코드(kaptCode): {item.get('kaptCode', '')}")
            print(f"  단지명: {item.get('kaptName', '')}")
            print(f"  시도: {item.get('as1', '')}")
            print(f"  시군구: {item.get('as2', '')}")
            print(f"  읍면동: {item.get('as3', '')}")
            print(f"  리: {item.get('as4', '')}")
            print(f"  법정동코드: {item.get('bjdCode', '')}")
            print(f"  도로명주소: {item.get('doroJuso', '')}")
            print()

        # 전체 필드 출력
        if okgye_items:
            print("[ 응답에 포함된 전체 필드 목록 ]")
            for key in sorted(okgye_items[0].keys()):
                print(f"  - {key}: {okgye_items[0][key]}")

        # 법정동코드 추출
        for item in okgye_items:
            bjd = str(item.get("bjdCode", ""))
            if bjd and len(bjd) >= 10:
                bjdong_cd = bjd[5:10]  # 뒤 5자리
                print(f"\n★ 옥계동 법정동코드 확인: {bjd}")
                print(f"  → 시군구코드: {bjd[:5]}")
                print(f"  → 법정동코드(5자리): {bjdong_cd}")
                break

    except requests.exceptions.RequestException as e:
        print(f"API 호출 실패: {e}")
    except (json.JSONDecodeError, KeyError) as e:
        print(f"응답 파싱 실패: {e}")
        print(f"응답 내용: {resp.text[:500]}")

    return bjdong_cd


# ============================================================
# 2. 건축물대장 표제부 조회
# ============================================================
def fetch_building_register(service_key, bjdong_cd):
    """건축물대장 표제부에서 옥계동 건물 정보를 조회합니다."""
    print("\n" + "=" * 60)
    print("  [2] 건축물대장 표제부 조회 (옥계동)")
    print("=" * 60)

    if not bjdong_cd:
        print("\n옥계동 법정동코드를 확인할 수 없어 건축물대장 조회를 건너뜁니다.")
        print("https://www.code.go.kr 에서 '경상북도 구미시 옥계동' 검색 후")
        print("법정동코드 뒤 5자리를 확인하세요.")
        return

    base_url = "https://apis.data.go.kr/1613000/BldRgstHubService/getBrTitleInfo"
    params = {
        "sigunguCd": SIGUNGU_CD,
        "bjdongCd": bjdong_cd,
        "numOfRows": "10",
        "pageNo": "1",
        "_type": "json",
    }

    full_url = build_url(base_url, service_key, params)
    print(f"\n파라미터: sigunguCd={SIGUNGU_CD}, bjdongCd={bjdong_cd}")
    print(f"(옥계동 건물 최대 10건 조회)\n")

    try:
        resp = requests.get(full_url, timeout=10)
        print(f"HTTP 상태코드: {resp.status_code}")

        if resp.status_code != 200:
            print(f"HTTP 오류: {resp.status_code}")
            print(f"응답 내용: {resp.text[:500]}")
            return

        data = resp.json()

        header = data.get("response", {}).get("header", {})
        result_code = header.get("resultCode")
        result_msg = header.get("resultMsg")
        print(f"응답 코드: {result_code} ({result_msg})")

        if not is_success_code(result_code):
            print(f"오류 발생: {result_msg}")
            print(f"전체 응답: {json.dumps(data, ensure_ascii=False, indent=2)}")
            return

        body = data.get("response", {}).get("body", {})
        total_count = body.get("totalCount", 0)
        print(f"총 데이터 건수: {total_count}")

        item_list = extract_item_list(body)

        if not item_list:
            print("조회된 건물이 없습니다.")
            return

        print(f"이번 페이지 조회 건수: {len(item_list)}\n")

        fields_to_show = [
            ("bldNm", "건물명"),
            ("platPlc", "지번주소"),
            ("newPlatPlc", "도로명주소"),
            ("mainPurpsCdNm", "주용도"),
            ("strctCdNm", "구조"),
            ("archArea", "건축면적(㎡)"),
            ("totArea", "연면적(㎡)"),
            ("platArea", "대지면적(㎡)"),
            ("grndFlrCnt", "지상층수"),
            ("ugrndFlrCnt", "지하층수"),
            ("useAprDay", "사용승인일"),
            ("hhldCnt", "세대수"),
            ("vlRat", "용적률(%)"),
            ("bcRat", "건폐율(%)"),
        ]

        for i, item in enumerate(item_list, 1):
            print(f"--- 건물 {i} ---")
            for field_key, field_name in fields_to_show:
                val = item.get(field_key, "")
                if val:
                    print(f"  {field_name}: {val}")
            print()

        # 전체 응답 필드 목록 출력
        if item_list:
            print("[ 응답에 포함된 전체 필드 목록 ]")
            for key in sorted(item_list[0].keys()):
                print(f"  - {key}: {item_list[0][key]}")

    except requests.exceptions.RequestException as e:
        print(f"API 호출 실패: {e}")
    except (json.JSONDecodeError, KeyError) as e:
        print(f"응답 파싱 실패: {e}")
        print(f"응답 내용: {resp.text[:500]}")


# ============================================================
# 3. 아파트 매매 실거래가 조회
# ============================================================
def fetch_apt_trade(service_key):
    """아파트 매매 실거래가에서 옥계동 데이터를 조회합니다."""
    print("\n" + "=" * 60)
    print("  [3] 아파트 매매 실거래가 조회 (구미시 → 옥계동 필터)")
    print("=" * 60)

    base_url = "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade"
    deal_ymd = "202603"
    params = {
        "LAWD_CD": SIGUNGU_CD,
        "DEAL_YMD": deal_ymd,
        "numOfRows": "100",
        "pageNo": "1",
    }

    full_url = build_url(base_url, service_key, params)
    print(f"\n파라미터: LAWD_CD={SIGUNGU_CD}, DEAL_YMD={deal_ymd}")
    print(f"(구미시 전체 조회 후 옥계동만 필터링)\n")

    try:
        resp = requests.get(full_url, timeout=10)
        print(f"HTTP 상태코드: {resp.status_code}")

        if resp.status_code != 200:
            print(f"HTTP 오류: {resp.status_code}")
            print(f"응답 내용: {resp.text[:500]}")
            return

        # XML 파싱
        root = ET.fromstring(resp.text)

        result_code = root.findtext(".//resultCode")
        result_msg = root.findtext(".//resultMsg")
        print(f"응답 코드: {result_code} ({result_msg})")

        if not is_success_code(result_code):
            print(f"오류 발생: {result_msg}")
            print(f"응답: {resp.text[:500]}")
            return

        total_count = root.findtext(".//totalCount")
        print(f"구미시 전체 거래 건수: {total_count}")

        items = root.findall(".//item")
        # 옥계동 필터링
        okgye_items = [
            item for item in items
            if item.findtext("umdNm", "").strip() == "옥계동"
        ]

        print(f"옥계동 거래 건수: {len(okgye_items)}\n")

        if not okgye_items:
            print("옥계동 거래 데이터가 없습니다.")
            print(f"(조회 기간: {deal_ymd})")
            dong_names = set()
            for item in items:
                dong = item.findtext("umdNm", "").strip()
                if dong:
                    dong_names.add(dong)
            if dong_names:
                print(f"\n이 기간에 거래가 있는 동: {', '.join(sorted(dong_names))}")
            return

        fields_to_show = [
            ("aptNm", "아파트명"),
            ("umdNm", "법정동"),
            ("excluUseAr", "전용면적(㎡)"),
            ("dealAmount", "거래금액(만원)"),
            ("dealYear", "계약년도"),
            ("dealMonth", "계약월"),
            ("dealDay", "계약일"),
            ("floor", "층"),
            ("buildYear", "건축년도"),
            ("jibun", "지번"),
            ("dealingGbn", "거래유형"),
        ]

        for i, item in enumerate(okgye_items[:10], 1):
            print(f"--- 거래 {i} ---")
            for field_key, field_name in fields_to_show:
                val = (item.findtext(field_key) or "").strip()
                if val:
                    print(f"  {field_name}: {val}")
            print()

        # 전체 필드 출력
        if okgye_items:
            print("[ 응답에 포함된 전체 필드 목록 ]")
            for elem in sorted(okgye_items[0], key=lambda e: e.tag):
                print(f"  - {elem.tag}: {(elem.text or '').strip()}")

    except requests.exceptions.RequestException as e:
        print(f"API 호출 실패: {e}")
    except ET.ParseError as e:
        print(f"XML 파싱 실패: {e}")
        print(f"응답 내용: {resp.text[:500]}")


# ============================================================
# 메인 실행
# ============================================================
def main():
    service_key = get_api_key()

    print(f"\n사용 코드 정보:")
    print(f"  시군구코드: {SIGUNGU_CD} (구미시)")

    # 1. 단지 목록 조회
    fetch_apt_list(service_key)

    # 2. 건축물대장 표제부
    fetch_building_register(service_key, BJDONG_CD)

    # 3. 아파트 매매 실거래가
    fetch_apt_trade(service_key)

    print("\n" + "=" * 60)
    print("  조회 완료")
    print("=" * 60)
    print("""
[참고]
- 단지 목록 API: 아파트 단지코드(kaptCode) 획득 → 상세정보 API 연동 가능
- 건축물대장 API: 건물의 구조, 면적, 층수, 용도 등 물리적 정보
- 실거래가 API: 실제 거래된 가격, 면적, 층 등 시장 정보
    """)


if __name__ == "__main__":
    main()
