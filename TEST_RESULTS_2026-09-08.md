# Test Results - 2026-09-09

## Changes covered

- SAE J1979 TypeScript decoder response framing and malformed-input handling.
- SAE supported-PID mask expectations and boundary coverage.
- Simulated Android telemetry polling.
- Simulated OBD responses for DTC fetch, DTC clear, and RPM polling.
- Android tests aligned with the production initial telemetry state.
- Complete ECU simulator discovery and virtual ELM327 smoke coverage.

## Static validation

- Workspace diagnostics: passed with no errors for all changed TypeScript and Kotlin files.
- `npm run lint`: passed in the accessible checkout.
- `python tools/ecu-simulator/run_tests.py`: 46 passed, 0 failed.
- `python -m unittest discover -s test/hil_emulator -p "test*.py"`: 2 passed, 0 failed.

## Runtime results

The portable suites were rerun from a clean clone of the latest GitHub revision:

```text
Python simulator tests: 46 passed, 0 failed with `python tools/ecu-simulator/run_tests.py`.
Virtual ELM327 smoke tests: 2 passed, 0 failed.
TypeScript full suite: previously 24 passed, 1 failed on the stale published checkout.
The corrected workspace expectations were not rerun from the Windows virtual filesystem.
TypeScript lint: passed.
```

The portable simulator and ELM327 results are current for commit `54337f0`. The TypeScript result above is historical because the terminal checkout did not execute the later workspace test correction.

## Android and physical hardware status

- Java 21: available.
- Gradle wrapper: available.
- Android SDK: installed at `C:\Users\michael\AppData\Local\Android\Sdk`.
- Android API 35 platform: installed.
- ADB: installed, but no device was listed while the phone was charging.
- Focused Gradle task: SDK discovery succeeded and the daemon started, but no task completion result was returned.
- Physical OTG/ELM validation: pending phone reconnection.

## Reproducible commands

```powershell
npm ci
npm test
npm run lint
.\gradlew.bat :app:testDebugUnitTest
python tools/ecu-simulator/run_tests.py
python -m unittest discover -s test/hil_emulator -p "test*.py"
```

This is not a fully passing complete-project report yet. The portable Python/ELM327 coverage passes; TypeScript lint passes; Android unit tests remain pending a completed Gradle run; physical testing remains pending an authorized ADB device.
