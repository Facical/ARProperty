# AGENTS.md

This file provides repository-specific guidance for Codex or similar local coding agents.

## Project Summary

- Project: ARProperty
- Goal: an AR-assisted apartment exploration app for Gumi, South Korea
- Active product docs:
  - `README.md`
  - `docs/project-guide.md`
  - `docs/api-spec.md`
  - `역할_분담표.md`

Archived research and draft material lives under `docs/archive/`.

## Primary Product Priority

The first priority is the Okgye-dong field demo: when a user points the phone camera at an apartment building in Okgye-dong, AR building tags should appear.

- Protect `GET /api/v1/buildings/nearby` for Okgye coordinates.
- Keep Okgye demo data, fallback coordinates, and Android AR fallback behavior usable.
- For changes near backend APIs, data loading, or Android AR/map flows, verify that this path is not broken.

## Current Repo Reality

This repository is no longer in the skeleton stage.

- `android/` contains a Compose app with `ar`, `map`, `livability`, and `building` feature areas, Kakao Map integration, ARCore Geospatial wiring, runtime base URL override, and fallback AR tags when Geospatial is unavailable.
- `backend/` contains a Spring Boot application with real source code, Dockerfile, application settings, DB schema scripts, and implemented `/health`, `GET /api/v1/buildings/nearby`, `GET /api/v1/buildings/{id}`, and `GET /api/v1/livability/infra/nearby` endpoints.
- `backend/src/main/java` application source exists. Some controllers such as `ComplexController` and `TradeController` are still stubs, so do not describe every planned API as implemented.
- `data/` contains implemented Python helper scripts, placeholder collector scripts, mapping files, and Okgye sync support paths.

Do not describe the repo as skeleton-stage. Instead, distinguish implemented demo-critical paths from planned or stubbed endpoints.

## Technical Standards

- Frontend target: Android Kotlin
- Backend target: Spring Boot 3.3 / Java 17
- Database: PostgreSQL 16 + PostGIS 3.4
- Cache: Redis 7
- Data ingestion: Python scripts under `data/collectors`
- API base URL convention: `http://localhost:8080`
- API path convention: `/api/v1/...`

## Commands That Match Current State

These commands match the repository as it exists today:

```bash
docker-compose up -d

curl 'http://localhost:8080/health'
curl 'http://localhost:8080/api/v1/buildings/nearby?lat=36.139&lon=128.432&radius=1000'

cd backend
./gradlew test -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew build -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

cd android
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew test -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

cd data/collectors
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Plain Gradle commands can fail on machines where Java 17 is not the default toolchain. Use the explicit Java 17 path above on this Mac, or the equivalent local Java 17 path on another machine.

Do not claim that planned endpoints such as `buildings/{id}/trades` or `complexes/*` are working until their controller mappings and services are implemented.

## Important Paths

- DB schema: `backend/scripts/init_db.sql`
- Backend source: `backend/src/main/java/`
- Backend env example: `backend/src/main/resources/application-example.yml`
- Docker Compose: `docker-compose.yml`
- Data collectors: `data/collectors/`
- Document archive: `docs/archive/`

## Constants And Local Context

- Gumi `sigungu_cd`: `47190`
- Okgye legal dong code: `4719012800`
- Gumi coordinate range used in docs: latitude `36.05~36.25`, longitude `128.20~128.50`

## Git Conventions

- Current base branch: `main`
- Working branches: `feature/{domain}/{name}`
- If the team later changes the branching model, all related docs must be updated together in the same change set

## Documentation Rule

When repository facts, API contracts, or branch rules change:

1. Update `README.md` first if current status changed.
2. Update `docs/api-spec.md` if request/response contracts changed.
3. Update `docs/project-guide.md` if scope, phases, or technical direction changed.
4. Leave old research in `docs/archive/` unless it is being intentionally promoted back into the active standards.

## Behavioral Guidelines

Adapted from [andrej-karpathy-skills / AGENTS.md](https://github.com/forrestchang/andrej-karpathy-skills/blob/main/AGENTS.md) (MIT). These principles bias toward caution over speed; for trivial edits, use judgment.

### 1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First
Minimum code that solves the problem. Nothing speculative.
- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

### 3. Surgical Changes
Touch only what you must. Clean up only your own mess.
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- Remove imports/variables/functions that **your** changes made unused; don't delete pre-existing dead code unless asked.
- Test: every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution
Define success criteria. Loop until verified.
- "Add validation" → write tests for invalid inputs, then make them pass.
- "Fix the bug" → write a test that reproduces it, then make it pass.
- "Refactor X" → ensure tests pass before and after.
- For multi-step tasks, state a brief plan with a verify step per item.

### Project-Specific Application
- Backend (Spring Boot): when adding controllers/services, do not pre-build interfaces, DTO hierarchies, or generic abstractions until a second concrete use case appears.
- Android (Compose): match the scaffolding style introduced in commit `dfccd56`; do not introduce alternative DI/navigation libraries without discussion.
- Data collectors (Python): keep scripts single-purpose; do not generalize across regions until Gumi (`sigungu_cd 47190`) flow is verified end-to-end.
- All layers: when a change requires updates to API contracts or repo facts, follow the existing **Documentation Rule** above in the same change set.
