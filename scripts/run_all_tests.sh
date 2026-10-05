#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_PATH="${1:-${ROOT_DIR}/TEST_RESULTS_$(date -u +%Y-%m-%d).md}"
RUN_ID="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cd "$ROOT_DIR"
mkdir -p "$(dirname "$REPORT_PATH")"
: > "$REPORT_PATH"

cat >> "$REPORT_PATH" <<EOF
# ForgeDiagnostics verification report

## Scope and acceptance criteria

- Run the repository's portable TypeScript, Python ECU simulator, and virtual ELM327 suites.
- Run Android JVM tests and a debug build when an Android SDK is available.
- Preserve evidence boundaries: local automated/simulator results do not prove HIL or real-vehicle behavior.
- Record each command, exit status, and environment blocker without hiding failures.

Run: $RUN_ID
Commit: $(git rev-parse --short HEAD 2>/dev/null || echo unknown)

## Changes made

- No production code changes are made by this runner.
- This report is generated from the commands executed below.

## Evidence matrix

| Area | Test/evidence | Result | Evidence level | Notes/blockers |
|---|---|---|---|---|
EOF

FAILED=0
run_case() {
  local label="$1" command="$2" level="$3" note="$4"
  local output status result
  output=$(mktemp)
  printf '\n=== %s ===\n' "$label"
  printf '$ %s\n' "$command"
  bash -lc "$command" >"$output" 2>&1
  status=$?
  cat "$output"
  if [ "$status" -eq 0 ]; then result="PASS"; else result="FAIL"; FAILED=1; fi
  printf '| %s | `%s` | **%s** | %s | %s |\n' "$label" "$command" "$result" "$level" "$note" >> "$REPORT_PATH"
  rm -f "$output"
}

run_case "TypeScript lint" "npm run lint" "Automated" "Type-check only; no vehicle or adapter involved."
run_case "TypeScript unit tests" "npm test" "Automated" "Decoder and security utility tests."
run_case "Python ECU simulator" "npm run test:simulator" "Simulator" "Virtual ECU/OBD/UDS behavior only."
run_case "Virtual ELM327 harness" "npm run test:elm327" "Simulator" "Dependency-free protocol harness; no physical adapter."
run_case "Probabilistic diagnostic experiment" "npm run test:probabilistic" "Probabilistic / quantum simulator" "Local state-vector model and Monte Carlo only; no real QPU."

if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME}" ]; then
  run_case "Android JVM unit tests" "./gradlew :app:testDebugUnitTest --no-daemon" "Automated" "SDK detected at $ANDROID_HOME."
  run_case "Android debug build" "./gradlew :app:assembleDebug --no-daemon" "Automated" "Build artifact only; install/runtime still unverified."
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT}" ]; then
  run_case "Android JVM unit tests" "./gradlew :app:testDebugUnitTest --no-daemon" "Automated" "SDK detected at $ANDROID_SDK_ROOT."
  run_case "Android debug build" "./gradlew :app:assembleDebug --no-daemon" "Automated" "Build artifact only; install/runtime still unverified."
else
  printf '| Android JVM unit tests | `./gradlew :app:testDebugUnitTest --no-daemon` | **BLOCKED** | Automated | Android SDK not available in this environment. |\n' >> "$REPORT_PATH"
  printf '| Android debug build | `./gradlew :app:assembleDebug --no-daemon` | **BLOCKED** | Automated | Android SDK not available in this environment. |\n' >> "$REPORT_PATH"
  printf '\n=== Android suites ===\nBLOCKED: Android SDK not available (set ANDROID_HOME/ANDROID_SDK_ROOT or sdk.dir in local.properties).\n'
fi

cat >> "$REPORT_PATH" <<'EOF'

## Safety and mechanic usability

- No physical vehicle, actuator, DTC clear, flashing, or bidirectional operation was performed.
- A simulator pass validates modeled software behavior only.
- Real-vehicle testing remains gated on a stationary vehicle, stable power, identified adapter/firmware, redacted raw traces, and independent result comparison.

## Defects and follow-up

- HIL bench, Android device/ADB, USB-OTG adapter, and real-vehicle evidence require their respective environments and are not inferred from this run.
- Cloud-provider and quantum-platform resources were not invoked by this local runner; they require credentials, account authorization, and a defined reproducible scenario.

## Honest conclusion

Portable automated and simulator results are reported above. Any blocked Android result is explicitly marked. This report does not claim physical-vehicle compatibility.
EOF

printf '\nReport written to %s\n' "$REPORT_PATH"
exit "$FAILED"
