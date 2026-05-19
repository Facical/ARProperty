# Okgye AR Field Demo QA

Date: 2026-05-19
Scope: Okgye-dong AR tag, popup, building detail, and trade drilldown demo path.

## 1. First Priority

The demo passes only if this works first:

> In Okgye-dong, when the user points the phone camera at an apartment building, AR building tags appear.

All checks below protect this path. Rich detail data, recent trades, and charts are secondary; they must not block the AR tag from appearing.

## 2. Required Preconditions

### Backend / Data

- `docker compose up -d db redis backend` starts without container restart loops.
- `apt_complex_master` has Okgye rows:
  ```sql
  SELECT count(*)
  FROM apt_complex_master
  WHERE legal_dong_code = '4719012800';
  ```
- `apt_building_master` has Okgye building rows with centroids:
  ```sql
  SELECT count(*)
  FROM apt_building_master b
  JOIN apt_complex_master c ON c.complex_id = b.complex_id
  WHERE c.legal_dong_code = '4719012800'
    AND b.centroid IS NOT NULL;
  ```
- `apt_trade_history` has rows for the demo complex/building path if trade list and price chart are part of the demo.

### Android Device

- Camera and location permissions are granted.
- Runtime BaseUrl points to the reachable backend host and ends with `/`.
- If Geospatial is expected, `GEOSPATIAL_API_KEY` is present and the device debug SHA-1 is registered.
- If Geospatial is unavailable, fallback tags must still appear from Okgye/default or device coordinates.

## 3. Automated Checks

Run before manual field QA:

```bash
cd backend
./gradlew build -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

Expected:

- Build succeeds.
- Controller/service tests for nearby/detail/trades/infra still pass.

```bash
cd android
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew test -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

Expected:

- Kotlin compilation succeeds.
- Unit tests pass.

```bash
python3 -m py_compile data/collectors/*.py backend/scripts/*.py
```

Expected:

- No syntax errors in collector/helper scripts.

## 4. API Smoke Checks

Use Okgye coordinates. The exact row count may change with local data, but `status` must be `ok`.

```bash
curl 'http://localhost:8080/api/v1/buildings/nearby?lat=36.13918&lon=128.42137&radius=1000&page=1&page_size=20'
```

Pass criteria:

- HTTP 200.
- `status: "ok"`.
- `data` is not empty for seeded Okgye demo DB.
- Each AR candidate has `building_id`, `complex_name`, `dong_name`, `lat`, and `lon`.

```bash
curl 'http://localhost:8080/api/v1/buildings/{building_id}'
```

Pass criteria:

- HTTP 200 for a nearby building ID.
- `status: "ok"`.
- Detail fields load without breaking the app if optional fields are null.

```bash
curl 'http://localhost:8080/api/v1/buildings/{building_id}/trades?page=1&page_size=10'
```

Pass criteria:

- HTTP 200 for an existing building ID.
- `status: "ok"`.
- Empty `data` is allowed, but if rows exist they include `deal_date`, `trade_type`, and at least one price-like field (`deal_amount`, `deposit`, or `monthly_rent`).

## 5. Manual AR Field Checklist

### A. First-Screen AR Tag

- Open the app on the AR tab.
- Allow camera/location permissions.
- Confirm the AR screen does not stay blank after initial load.
- Confirm the top debug panel shows one of:
  - Earth tracking coordinates.
  - Geospatial unavailable/missing-key fallback status.
  - Okgye fallback status.
- Confirm at least one AR tag/cube/label appears for Okgye demo data.

Pass criteria:

- AR tag appears within a practical demo window.
- If Geospatial fails, fallback tags still appear.
- The app does not crash when location is unavailable.

### B. Building Selection

- Select a building through the visible tag or the bottom building chip row.
- Confirm the selected marker/label is visually emphasized.
- Confirm the bottom sheet shows building name, distance/grade, latest price, area, and floor when available.

Pass criteria:

- Selection changes only the selected building state.
- Nearby tags remain visible after selection.

### C. AR Detail Popup

- Tap `상세정보`.
- Confirm the popup opens.
- Confirm the popup shows building summary fields:
  - floors
  - height
  - structure
  - households
  - livability grade/score if available
- Confirm recent trade rows show up to 3 rows when trades exist.
- Confirm the price trend chart appears when there are at least one or more chartable trade rows.
- Confirm fallback behavior:
  - If trade API fails, popup still shows the nearby latest trade summary.
  - If detail API fails, popup still shows nearby summary fields.

Pass criteria:

- Popup enrichments never prevent the popup from opening.
- Popup enrichments never remove the AR tags behind the modal.

### D. Building Detail Screen

- From AR or map flow, open the building detail screen.
- Confirm building detail fields load.
- Confirm recent trade history list loads, shows empty state, or shows an error message without hiding building detail.
- Confirm `단지 상세` and `생활 점수` buttons do not crash even if their deeper routes are still incomplete.

Pass criteria:

- Building detail screen is usable with partial data.
- Trade failure is isolated from detail failure.

## 6. Known Non-Blocking Gaps

These do not block the Okgye AR tag demo:

- Direct marker tapping may be less reliable than chip-row selection.
- Complex APIs are still stub/planned.
- Livability detail API and richer livability screen are still planned.
- Redis cache and API call-log enforcement are infrastructure/planned, not active runtime behavior.
- Placeholder collector scripts are explicitly marked and should not be presented as ready collection jobs.

## 7. QA Result Log

Use this table during each demo rehearsal.

| Date | Environment | Backend Build | Android Test | API Smoke | Device AR Tag | Popup Trades/Chart | Notes |
|---|---|---|---|---|---|---|---|
| 2026-05-19 | Local dev | Pass (`./gradlew build`) | Pass (`./gradlew test`) | Not run | Not run | Not run | Python script compile also passed. Device/manual AR checks still required |
