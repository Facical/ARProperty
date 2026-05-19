# ARProperty Upgrade Review

Date: 2026-05-18
Branch: `upgrade`

This document saves the repository-wide review and turns it into an upgrade discussion checklist. The goal is not to rewrite everything at once, but to agree on the risky points first and then improve the repo in small, verifiable steps.

## North Star Priority

The first priority is the Okgye-dong field demo: when the user points the phone camera at an apartment building in Okgye-dong, the AR building tag should appear. Every upgrade should protect this flow first.

Practical implication:

- Do not make backend, API contract, data, or Android changes that risk breaking `GET /api/v1/buildings/nearby` for Okgye coordinates.
- Keep Okgye demo data and coordinates usable while improving broader architecture.
- Treat AR fallback behavior as a top demo-critical fix if Geospatial is unavailable or unstable.
- Verify important changes against the Okgye AR tag path, not only isolated build/test success.

## Current Read

The repository is no longer just a skeleton. It has a working Android Compose/AR/Kakao Map direction, a Spring Boot backend with real source code, PostGIS schema, Docker Compose, and several data collection/normalization scripts.

The biggest gap is not basic structure. The bigger issue is that implementation, docs, API contracts, and operational assumptions have drifted from each other.

## Decisions To Discuss First

### 1. API Key Policy

The API keys are intentionally committed so teammates can build and run the project on other PCs without repeatedly requesting separate keys. Some keys are also constrained by package name, SHA-1, or console configuration.

This should be treated as a deliberate team/demo policy, not an accidental leak.

Decision on 2026-05-18:
Use option B. Keep the committed shared development/demo keys during the private school/team demo phase, and document the boundary, restrictions, and rotation guidance clearly.

Still, the repo should document the boundary clearly:

- Keep the repository private if real shared keys remain committed.
- Mark the keys as team development/demo credentials, not production credentials.
- Confirm which keys are actually device/package restricted and which are plain server-side API keys.
- Rotate or revoke shared keys after presentation/demo periods if the repo may be shared outside the team.
- Keep `.env.example` / `application-example.yml` consistent with the chosen policy.

Documentation target:
`README.md`, `backend/src/main/resources/application-example.yml`, and any future env example files should say the same thing: committed keys are a private demo exception, not a general secret-management rule.

### 2. API Contract Standard

There were two response conventions in the repo:

- `docs/api-spec.md` used `"status": "success"`.
- Backend `ApiResponse.success(...)` returns `"status": "ok"`.
- `docs/api-편의시설.md` also uses `"ok"` for the convenience facility API.

Decision on 2026-05-18:
Use the implemented `"ok"` convention for `/api/v1/...` `ApiResponse` wrappers. `/health` is outside the common wrapper and currently keeps its implemented `"success"` health status.

Documentation target:
Update the main API spec examples to `"ok"` and keep `docs/api-편의시설.md` aligned. Done in `docs/api-spec.md`.

### 3. Infra Endpoint Path

The actual backend and Android use:

```text
GET /api/v1/livability/infra/nearby
```

The main API spec previously listed:

```text
GET /api/v1/infra/nearby
```

Decision on 2026-05-18:
Keep the current implemented path.

Documentation target:
Use `/api/v1/livability/infra/nearby` in `docs/api-spec.md`, because it is already wired in Android and backend. Done in `docs/api-spec.md`.

## Priority Findings

### P1. Error Handling And Validation

Status on 2026-05-18:
Done for the implemented nearby endpoints.

Required query parameter errors, type mismatch errors, invalid coordinates, and invalid categories should consistently return 400 with the standard error wrapper.

Implemented:

- Missing or non-numeric `lat/lon` returns 400 `INVALID_PARAMETER`.
- Coordinates outside the Gumi range return 400 `INVALID_COORDINATES`.
- Generic 500 responses return `INTERNAL_ERROR` without exposing raw exception messages.
- MockMvc coverage was added for `buildings/nearby` and `livability/infra/nearby`.

### P1. Convenience Facility Query Limits

`/api/v1/livability/infra/nearby` currently accepts radius directly and returns all matching rows.

Status on 2026-05-19:
Done for the implemented nearby infra endpoint.

Upgrade target:

- Clamp radius. Implemented as server-side `1~3000`.
- Add `page` and `page_size`. Implemented with defaults `page=1`, `page_size=100`, max page size `100`.
- Return `total_count` or explicitly document count-only metadata. Implemented with `count` for current page and `total_count` for the full query.
- Keep Android map calls simple. Existing Android calls can omit `page/page_size` and receive the default first page.

### P1. AR Fallback When Geospatial Key Is Missing

Status on 2026-05-18:
Done in `ArRoute.kt`.

Implemented:

- Nearby building loading now starts from Fused Location, or Okgye default coordinates when location is missing or outside the Gumi range.
- Geospatial anchors are still created only when Earth tracking is available.
- While Geospatial is missing or Earth is not tracking, fallback AR tags are shown from the loaded nearby buildings so the Okgye demo does not go blank.

### P2. Documentation Drift

`README.md` reflects recent implementation, but `docs/project-guide.md` and `AGENTS.md` still describe the repo as skeleton-stage.

Status on 2026-05-19:
Done for the skeleton-stage drift in `docs/project-guide.md` and `AGENTS.md`.

Upgrade target:

- Update `docs/project-guide.md` to match current reality. Done.
- Track `AGENTS.md` if it is intended to be the active agent guide. Content updated; file is still untracked until committed.
- Remove or soften statements that say backend source is not committed. Done.

### P2. Cache And API Call Limit Claims

Docs describe Redis caching and API call logging as active architecture, but `CacheService` and `AppConfig` are placeholders and `api_call_log` is not currently used by services.

Status on 2026-05-19:
Documented as planned/placeholder rather than active runtime behavior.

Upgrade target:

- Either implement minimal cache/call-log behavior, or mark it as planned. Done via documentation correction.
- Prefer documentation correction first unless caching is needed for the demo. Done; no runtime behavior was changed.

### P2. Placeholder Scripts

Several collector/backend scripts are one-line placeholders.

Status on 2026-05-19:
Done. Placeholder scripts are explicitly marked, and README no longer describes every Python script as ready.

Examples:

- `data/collectors/collect_apt_list.py`
- `data/collectors/collect_building_register.py`
- `data/collectors/collect_gumi_infra.py`
- `data/collectors/collect_trade_history.py`
- `backend/scripts/load_infra.py`
- `backend/scripts/load_complexes.py`

Upgrade target:

- Mark placeholders clearly, or remove them from "ready scripts" counts. Done.
- Keep only verified commands in README. Done; placeholder scripts are not listed as runnable demo paths.

### P2. CI And Test Coverage

Status on 2026-05-19:
Done for the implemented backend endpoints and backend CI trigger.

Backend tests now include controller error handling, Okgye `buildings/nearby` success metadata, livability pagination/clamping service behavior, DTO JSON response contracts, and an application entrypoint smoke test. Android unit tests still cover navigation route constants only.

Upgrade target:

- Add backend controller/service tests for the real endpoints. Done for implemented nearby endpoints.
- Add DTO serialization contract tests around API responses. Done for `ApiResponse`.
- Update CI branch trigger from `develop` to `main` if main remains the base branch. Done in `.github/workflows/backend-ci.yml`.

## Verification Snapshot

Commands checked during review:

```bash
docker compose config --quiet
```

Passed.

```bash
./gradlew build -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

Passed in `backend/`.

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew test -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

Passed in `android/`.

```bash
python3 -m py_compile data/collectors/*.py okgye_building_data.py backend/scripts/*.py
```

Passed.

Environment note:
Plain Gradle commands failed locally until Java 17 and Android SDK paths were provided explicitly.

## Suggested Upgrade Order

1. Agree on API key policy and document it.
2. Align API response status and infra endpoint path.
3. Fix AR fallback when Geospatial is disabled, if it blocks Okgye AR tags. (done)
4. Fix backend validation/error handling. (done)
5. Add radius/page limits to infra nearby. (done)
6. Update project docs to current implementation reality. (done)
7. Clarify Redis cache/API call-log claims as planned runtime work. (done)
8. Mark placeholder scripts and keep README commands verified. (done)
9. Add thin but real tests around implemented endpoints. (done)

## Follow-up Implementation

### Backend Building Detail API

Status on 2026-05-19:
Done for `GET /api/v1/buildings/{building_id}`.

Implemented:

- Added a backend detail response for one apartment building/dong.
- Joined building, complex, and default livability score fields.
- Added 404 `BUILDING_NOT_FOUND` handling for missing IDs.
- Added controller/service tests around the success and not-found paths.

### Android Building Detail API Connection

Status on 2026-05-19:
Done for `BuildingDetailRoute`.

Implemented:

- Connected Android `BuildingDetailRoute` to `GET /api/v1/buildings/{building_id}` through `BuildingRepository`.
- Added loading, error/retry, and success states.
- Expanded the Android `BuildingDetail` model to include complex info, structure/area/approval fields, and livability summary.

### Android AR DetailPopup Detail Binding

Status on 2026-05-19:
Done for partial detail enrichment.

Implemented:

- Kept AR tag rendering and `nearby` loading as the first-priority path.
- Added selected building detail state to `ArViewModel`.
- Triggered `GET /api/v1/buildings/{building_id}` only when the AR detail popup opens.
- Enriched `DetailPopupContent` with building height, structure, approval date, total area, households, parking count, and detail livability fields when available.
- Preserved the existing `nearby` summary fallback if detail loading fails.

Next expansion:

- Decide later whether the popup should call a livability detail API or keep only the default livability summary.

### Backend Building Trades API

Status on 2026-05-19:
Done.

Implemented:

- Added `GET /api/v1/buildings/{building_id}/trades`.
- Returns same-complex and same-dong trade rows for the selected building.
- Supports optional `type`, `year`, `page`, and `page_size` parameters.
- Clamps paging to `page >= 1` and `page_size` between `1` and `100`.
- Returns 404 `BUILDING_NOT_FOUND` when the building ID does not exist.
- Added controller and service tests for success, not found, invalid type, row mapping, and paging clamp.

### Android Building Trades API Connection

Status on 2026-05-19:
Done for `BuildingDetailRoute`.

Implemented:

- Connected Android `BuildingDetailRoute` to `GET /api/v1/buildings/{building_id}/trades` through `BuildingRepository`.
- Added separate loading, empty, and error states for trade history so the building detail screen still renders when only trades fail.
- Displayed the latest trade rows with date, type, price/deposit, area, floor, and dealing type.
- Added `dealing_type` to the Android `TradeItem` model.

### Android AR DetailPopup Trade Binding

Status on 2026-05-19:
Done for compact recent trades and price trend.

Implemented:

- Kept AR tag rendering and `nearby` loading as the first-priority path.
- Triggered `GET /api/v1/buildings/{building_id}/trades` only when the AR detail popup opens.
- Added selected building trade state to `ArViewModel`.
- Displayed up to three recent trades in `DetailPopupContent` with date, type, price/deposit, area, floor, and dealing type.
- Added a compact Compose Canvas price trend chart based on the latest available trade type.
- Preserved the existing `nearby.latest_trade` fallback while trades are loading or when the trades request fails.

Next expansion:

- Add richer filtering or area-specific charts later if the trade dataset grows enough to justify it.

### Okgye AR Integrated QA

Status on 2026-05-19:
Checklist created.

Implemented:

- Added `docs/okgye-ar-qa.md` as the field demo QA source of truth.
- Centered the checklist on the first priority: Okgye camera view must show AR building tags.
- Covered backend/data preconditions, API smoke checks, AR fallback behavior, popup detail/trade/chart checks, and building detail trade checks.
- Linked the QA checklist from README's demo checklist section.

Next expansion:

- Run the manual device field checklist and record the result in the QA log table.
