# ForgeDiagnostics verification report

## Scope and acceptance criteria

- Run the repository's portable TypeScript, Python ECU simulator, and virtual ELM327 suites.
- Run bounded randomized accuracy and probabilistic diagnostic experiments.
- Run Android JVM tests and a debug build when an Android SDK is available.
- Preserve evidence boundaries: local automated/simulator results do not prove HIL or real-vehicle behavior.
- Record each command, exit status, and environment blocker without hiding failures.

Run: 2026-10-05T08:24:49Z
Commit: d7ac953

## Changes made

- No production code changes are made by this runner.
- This report is generated from the commands executed below.

## Evidence matrix

| Area | Test/evidence | Result | Evidence level | Notes/blockers |
|---|---|---|---|---|
| TypeScript lint | `npm run lint` | **PASS** | Automated | Type-check only; no vehicle or adapter involved. |
| TypeScript unit tests | `npm test` | **PASS** | Automated | 46 tests: decoder, security, randomized accuracy, and corruption coverage. |
| Python ECU simulator | `npm run test:simulator` | **PASS** | Simulator | Virtual ECU/OBD/UDS behavior only. |
| Virtual ELM327 harness | `npm run test:elm327` | **PASS** | Simulator | Dependency-free protocol harness; no physical adapter. |
| Randomized protocol accuracy | `npm test -- --run test/unit/j1979_property.test.ts` | **PASS** | Automated | 2,266 generated/negative cases across speed, RPM, and corruption classes. |
| Probabilistic diagnostic experiment | `npm run test:probabilistic` | **PASS** | Probabilistic / quantum simulator | 20,000 trials, 256 shots/trial; classical accuracy 0.999950, local state-vector accuracy 1.000000, mean measurement confidence 0.749299. No QPU. |
| Android JVM unit tests | `./gradlew :app:testDebugUnitTest --no-daemon` | **BLOCKED** | Automated | Android SDK not available in this environment. |
| Android debug build | `./gradlew :app:assembleDebug --no-daemon` | **BLOCKED** | Automated | Android SDK not available in this environment. |

## Safety and mechanic usability

- No physical vehicle, actuator, DTC clear, flashing, or bidirectional operation was performed.
- A simulator pass validates modeled software behavior only.
- Real-vehicle testing remains gated on a stationary vehicle, stable power, identified adapter/firmware, redacted raw traces, and independent result comparison.

## Defects and follow-up

- HIL bench, Android device/ADB, USB-OTG adapter, and real-vehicle evidence require their respective environments and are not inferred from this run.
- Cloud-provider and quantum-platform resources were not invoked by this local runner; they require credentials, account authorization, and a defined reproducible scenario.
- The quantum result is from a standard-library local state-vector model; it is not evidence from IBM Quantum, Amazon Braket, a real QPU, or a physical ECU.

## Honest conclusion

Portable automated and simulator results are reported above. Any blocked Android result is explicitly marked. This report does not claim physical-vehicle compatibility.
