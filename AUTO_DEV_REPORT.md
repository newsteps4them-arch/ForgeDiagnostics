# Team Forge Auto-Dev Diagnostic Report

- **Overall Status:** `PASSED`
- **Passed Suites:** `5/5`

## Test Execution Summary

| Suite Name | Command | Result |
| :--- | :--- | :--- |
| TypeScript Lint & Types | `npx tsc --noEmit` | ✅ PASS |
| Vitest Unit Tests | `npx vitest run` | ✅ PASS |
| Python ECU Simulator Tests | `/home/jules/.pyenv/versions/3.12.13/bin/python3 tools/ecu-simulator/run_tests.py` | ✅ PASS |
| Python HIL Emulator Tests | `/home/jules/.pyenv/versions/3.12.13/bin/python3 -m unittest discover -s test/hil_emulator -p test*.py` | ✅ PASS |
| Bot Collective Unit Tests | `/home/jules/.pyenv/versions/3.12.13/bin/python3 -m unittest discover -s test/unit` | ✅ PASS |
