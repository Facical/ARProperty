#!/usr/bin/env sh

set -u

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
WARNINGS=0
ERRORS=0

ok() {
  printf 'OK   %s\n' "$1"
}

warn() {
  WARNINGS=$((WARNINGS + 1))
  printf 'WARN %s\n' "$1"
}

fail() {
  ERRORS=$((ERRORS + 1))
  printf 'FAIL %s\n' "$1"
}

cd "$ROOT_DIR" || exit 1

printf 'ARProperty Codex quick doctor\n'
printf 'Workspace: %s\n\n' "$ROOT_DIR"

if [ -f "AGENTS.md" ] && [ -d "backend" ] && [ -d "android" ]; then
  ok "Repository shape looks like ARProperty."
else
  fail "Run this from the ARProperty repository root."
fi

if command -v java >/dev/null 2>&1; then
  JAVA_SPEC="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.specification.version/ {print $2; exit}')"
  case "$JAVA_SPEC" in
    17|18|19|20|21|22|23|24|25)
      ok "Java $JAVA_SPEC is available."
      ;;
    "")
      warn "Java is present, but its version could not be detected."
      ;;
    *)
      warn "Java $JAVA_SPEC is active. Use JDK 17+ for backend and Android Gradle commands."
      ;;
  esac
else
  fail "Java is not on PATH. Install JDK 17 before running Gradle builds."
fi

if [ -x "backend/gradlew" ]; then
  ok "Backend Gradle wrapper is executable."
else
  warn "backend/gradlew is not executable. Run: chmod +x backend/gradlew"
fi

if [ -x "android/gradlew" ]; then
  ok "Android Gradle wrapper is executable."
else
  warn "android/gradlew is not executable. Run: chmod +x android/gradlew"
fi

if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    ok "Docker daemon is reachable."
  else
    warn "Docker is installed, but the daemon is not reachable."
  fi

  if docker compose version >/dev/null 2>&1; then
    ok "Docker Compose plugin is available."
  elif command -v docker-compose >/dev/null 2>&1; then
    ok "Legacy docker-compose is available."
  else
    warn "Docker Compose is not available."
  fi
else
  warn "Docker is not installed or not on PATH."
fi

if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
  ok "ANDROID_HOME points to an existing SDK."
elif [ -d "$HOME/Library/Android/sdk" ]; then
  ok "Android SDK found at ~/Library/Android/sdk."
else
  warn "Android SDK was not found. Backend-only Codex work can continue."
fi

if command -v lsof >/dev/null 2>&1; then
  for port in 8080 5433 6379; do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      warn "Port $port is already in use."
    else
      ok "Port $port is free."
    fi
  done
else
  warn "lsof is unavailable, so port checks were skipped."
fi

if command -v curl >/dev/null 2>&1; then
  if curl -fsSIL --max-time 5 https://services.gradle.org/distributions/ >/dev/null 2>&1; then
    ok "Gradle distribution host responds within 5 seconds."
  else
    warn "Gradle distribution host did not respond within 5 seconds. First Gradle run may be slow or fail."
  fi
else
  warn "curl is unavailable, so the Gradle network check was skipped."
fi

printf '\nSummary: %s error(s), %s warning(s).\n' "$ERRORS" "$WARNINGS"
if [ "$ERRORS" -gt 0 ]; then
  exit 1
fi
