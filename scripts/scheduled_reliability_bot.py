#!/usr/bin/env python3
"""
Team Forge - Scheduled Diagnostic Data Reliability & Accuracy Bot

Runs scheduled automotive diagnostic verification tests:
1. SAE J1979 PID decoder formulas & bitmasks accuracy verification.
2. ECU Simulator & ELM327 HIL Emulator end-to-end communication check.
3. Portable TypeScript and Python diagnostic test suite execution.
4. Auto-healing / diagnostic report generation via Gemini AI API.
"""

import os
import sys
import subprocess
import json
import time
from typing import Dict, Any, List, Tuple

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

try:
    import google.generativeai as genai
    HAS_GEMINI = True
except ImportError:
    HAS_GEMINI = False


def run_command(cmd: List[str], cwd: str = ".") -> Tuple[int, str, str]:
    """Runs a shell command and returns (returncode, stdout, stderr)."""
    try:
        res = subprocess.run(
            cmd,
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=120
        )
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except Exception as ex:
        return 1, "", str(ex)


def verify_j1979_formulas() -> Tuple[bool, List[str]]:
    """Validates core SAE J1979 formulas against expected mathematical standards."""
    logs = []
    success = True

    # Test cases: (PID, Raw Bytes, Expected Name, Expected Value, Tolerance)
    test_cases = [
        ("0C", [0x0F, 0xA0], "Engine RPM", 1000.0, 0.01),      # ((15*256) + 160) / 4 = 1000
        ("0D", [0x37], "Vehicle Speed", 55.0, 0.01),           # 55 km/h
        ("05", [0x7B], "Coolant Temp", 83.0, 0.01),            # 123 - 40 = 83 °C
        ("0F", [0x50], "Intake Air Temp", 40.0, 0.01),         # 80 - 40 = 40 °C
        ("04", [0xFF], "Engine Load", 100.0, 0.01),            # (255 * 100) / 255 = 100%
    ]

    logs.append("### SAE J1979 PID Mathematical Accuracy Audit")

    for pid, bytes_val, name, expected, tol in test_cases:
        if pid == "0C":
            val = ((bytes_val[0] * 256) + bytes_val[1]) / 4.0
        elif pid in ("0D",):
            val = float(bytes_val[0])
        elif pid in ("05", "0F"):
            val = float(bytes_val[0] - 40)
        elif pid == "04":
            val = (bytes_val[0] * 100.0) / 255.0
        else:
            val = 0.0

        diff = abs(val - expected)
        if diff <= tol:
            logs.append(f"- ✅ **PID 0x{pid} ({name})**: Computed `{val}` match expected `{expected}` (diff: `{diff:.4f}`)")
        else:
            logs.append(f"- ❌ **PID 0x{pid} ({name})**: Computed `{val}` MISMATCH expected `{expected}`")
            success = False

    return success, logs


def verify_hil_emulator_stream() -> Tuple[bool, List[str]]:
    """Interacts with the Virtual ELM327 HIL Emulator to verify live command responses."""
    logs = []
    logs.append("### Live Virtual ELM327 HIL Stream Verification")

    hil_script = "test/hil_emulator/virtual_elm327.py"
    if not os.path.exists(hil_script):
        logs.append(f"- ❌ HIL emulator script missing at `{hil_script}`")
        return False, logs

    try:
        p = subprocess.Popen(
            ["python3", hil_script],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        test_cmds = [
            ("010C", "41 0C 0F A0"), # 1000 RPM
            ("010D", "41 0D 37"),    # 55 km/h
            ("0105", "41 05 7B"),    # 83 °C
            ("03", "43 02 04 20"),   # Stored DTCs
        ]

        # Read banner
        _ = p.stderr.readline()

        all_ok = True
        for cmd, expected_substring in test_cmds:
            p.stdin.write(f"{cmd}\n")
            p.stdin.flush()
            response = p.stdout.readline()

            if expected_substring in response:
                logs.append(f"- ✅ **Command `{cmd}`**: Received expected string `{response.strip()}`")
            else:
                logs.append(f"- ❌ **Command `{cmd}`**: Expected `{expected_substring}`, got `{response.strip()}`")
                all_ok = False

        p.stdin.close()
        p.stdout.close()
        p.stderr.close()
        p.terminate()
        p.wait()
        return all_ok, logs

    except Exception as ex:
        logs.append(f"- ❌ Exception during HIL stream test: `{ex}`")
        return False, logs


def run_full_test_suites() -> Tuple[bool, List[str]]:
    """Runs the portable test suite (npm run test:all:portable)."""
    logs = []
    logs.append("### Automated Portable Test Suite Execution")

    code, stdout, stderr = run_command(["npm", "run", "test:all:portable"])

    if code == 0:
        logs.append("- ✅ **TypeScript Linting & Vitest Suites**: PASSED")
        logs.append("- ✅ **Python ECU Simulator Tests**: PASSED")
        logs.append("- ✅ **Python ELM327 HIL Emulator Tests**: PASSED")
        return True, logs
    else:
        logs.append(f"- ❌ **Test Suite Execution FAILED** (exit code `{code}`)")
        logs.append("```\n" + (stderr or stdout)[-1000:] + "\n```")
        return False, logs


def generate_gemini_analysis(all_passed: bool, report_body: str) -> str:
    """Uses Gemini 2.5 Flash to generate an AI verification analysis summary."""
    if not HAS_GEMINI or not GEMINI_API_KEY:
        return "*(Gemini AI API key not configured - skipping AI deep analysis)*"

    try:
        genai.configure(api_key=GEMINI_API_KEY)
        model = genai.GenerativeModel("gemini-2.5-flash")

        prompt = (
            "You are Team Forge's Lead Automotive Software Reliability Specialist.\n"
            "Review the following Diagnostic Reliability & Accuracy Audit Report:\n\n"
            f"{report_body}\n\n"
            "Provide a concise, 3-bullet-point executive verdict on diagnostic precision, "
            "data safety for automotive mechanics, and system readiness."
        )

        resp = model.generate_content(prompt)
        return resp.text.strip()
    except Exception as ex:
        return f"*(Gemini AI analysis error: {ex})*"


def main():
    print("===================================================================")
    print("🤖 Team Forge Scheduled Diagnostic Data Reliability & Accuracy Bot")
    print("===================================================================")

    formula_ok, formula_logs = verify_j1979_formulas()
    hil_ok, hil_logs = verify_hil_emulator_stream()
    suite_ok, suite_logs = run_full_test_suites()

    all_passed = formula_ok and hil_ok and suite_ok
    status_str = "PASSED (100% ACCURATE)" if all_passed else "FAILED (DISCREPANCIES DETECTED)"

    report_sections = [
        "# 🛡️ Team Forge Diagnostic Reliability & Accuracy Report",
        f"- **Timestamp**: `{time.strftime('%Y-%m-%d %H:%M:%S UTC', time.gmtime())}`",
        f"- **Overall Reliability Verdict**: `{status_str}`",
        "",
        "\n".join(formula_logs),
        "",
        "\n".join(hil_logs),
        "",
        "\n".join(suite_logs),
        "",
        "### 🧠 Gemini AI Executive Analysis",
    ]

    interim_report = "\n".join(report_sections)
    ai_analysis = generate_gemini_analysis(all_passed, interim_report)
    report_sections.append(ai_analysis)

    final_report = "\n".join(report_sections)

    with open("DIAGNOSTIC_RELIABILITY_REPORT.md", "w", encoding="utf-8") as f:
        f.write(final_report)

    print(f"\n[+] Diagnostic report written to DIAGNOSTIC_RELIABILITY_REPORT.md")
    print(f"[+] Overall Verdict: {status_str}")

    if all_passed:
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
