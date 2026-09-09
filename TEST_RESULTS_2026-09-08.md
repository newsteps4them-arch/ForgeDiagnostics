# Test Results - 2026-09-09

## Changes covered

- SAE J1979 TypeScript decoder response framing and malformed-input handling.
- SAE supported-PID mask expectations and boundary coverage.
- Simulated Android telemetry polling.
- Simulated OBD responses for DTC fetch, DTC clear, and RPM polling.
- Android tests aligned with the production initial telemetry state.

## Static validation

- Workspace diagnostics: passed with no errors for all changed TypeScript and Kotlin files.
- `npm run lint`: passed in the accessible checkout.

## Runtime commands attempted

The following commands were previously blocked by the WSL2 installer and were rerun after Ubuntu became available:

```text
Python simulator tests: 15 passed, 0 failed with `python -m unittest discover -s tools/ecu-simulator/tests -p "test*.py"`.
TypeScript full suite: 24 passed, 1 failed.
TypeScript focused J1979 suite: 9 passed, 1 failed.
TypeScript lint: passed.
```

The TypeScript failure is the published test still expecting the old supported-PID values (`01`-`05` and `06`). The decoder returned the corrected SAE values (`1C`-`20` and `1B`). The workspace copy contains the corrected expectations; the accessible checkout was cloned from the published revision and does not include the later unpushed test edit.

The accessible checkout used for these results was `35d6eed` from the published `main` branch. The VS Code workspace itself is mounted through a virtual filesystem, so its unpushed edits could not be executed directly by the terminal.

Android toolchain status:

- Java 21: available.
- Gradle wrapper: available.
- Android SDK: installed at `C:\Users\michael\AppData\Local\Android\Sdk`.
- Android API 35 platform: installed.
- ADB: installed, but no device was listed at test time.
- Focused Gradle task: SDK discovery succeeded and the daemon started, but no task completion result was returned.
- ADB device list: empty; no physical Android device was connected to the host during validation.

## Required rerun commands

After the terminal is available, run from the repository root:

```powershell
npm ci
npm test
npm run lint
.\gradlew.bat :app:testDebugUnitTest
python -m unittest discover tools/ecu-simulator/tests
```

This is not a fully passing runtime report yet. Python tests and TypeScript lint passed; the workspace TypeScript expectations are corrected, but the published checkout still contains one stale assertion; Android unit tests remain pending a completed Gradle run; physical testing remains pending an authorized ADB device.
