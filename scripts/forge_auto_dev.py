#!/usr/bin/env python3
"""
Team Forge - Automated Development & Self-Healing Bot (Forge Auto-Dev Bot)

Performs continuous automated verification across TypeScript, Python ECU Simulators,
ELM327 HIL Emulators, and Unit Test suites. Generates diagnostic reports and triggers
self-healing / pre-commit routines.
"""

import os
import sys
import json
import subprocess
import argparse
from typing import List, Dict, Any, Tuple

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def run_step(name: str, cmd: List[str], cwd: str = REPO_ROOT) -> Dict[str, Any]:
    """Runs a shell command and captures stdout, stderr, exit status, and duration."""
    print(f"[*] Running step: {name} ({' '.join(cmd)})...")
    try:
        res = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        passed = (res.returncode == 0)
        return {
            "name": name,
            "cmd": " ".join(cmd),
            "passed": passed,
            "returncode": res.returncode,
            "stdout": res.stdout.strip(),
            "stderr": res.stderr.strip()
        }
    except Exception as ex:
        return {
            "name": name,
            "cmd": " ".join(cmd),
            "passed": False,
            "returncode": 1,
            "stdout": "",
            "stderr": str(ex)
        }

def run_all_checks() -> List[Dict[str, Any]]:
    """Runs the full portable test matrix and auto-dev checks."""
    steps = [
        ("TypeScript Lint & Types", ["npx", "tsc", "--noEmit"]),
        ("Vitest Unit Tests", ["npx", "vitest", "run"]),
        ("Python ECU Simulator Tests", [sys.executable, "tools/ecu-simulator/run_tests.py"]),
        ("Python HIL Emulator Tests", [sys.executable, "-m", "unittest", "discover", "-s", "test/hil_emulator", "-p", "test*.py"]),
        ("Bot Collective Unit Tests", [sys.executable, "-m", "unittest", "discover", "-s", "test/unit"])
    ]

    results = []
    for name, cmd in steps:
        res = run_step(name, cmd)
        results.append(res)
    return results

def generate_report(results: List[Dict[str, Any]], report_path: str = "AUTO_DEV_REPORT.md") -> str:
    """Generates a detailed Markdown report based on test suite execution results."""
    total_steps = len(results)
    passed_steps = sum(1 for r in results if r["passed"])
    failed_steps = total_steps - passed_steps
    status_summary = "PASSED" if failed_steps == 0 else "FAILED"

    report = f"# Team Forge Auto-Dev Diagnostic Report\n\n"
    report += f"- **Overall Status:** `{status_summary}`\n"
    report += f"- **Passed Suites:** `{passed_steps}/{total_steps}`\n\n"
    report += f"## Test Execution Summary\n\n"
    report += f"| Suite Name | Command | Result |\n"
    report += f"| :--- | :--- | :--- |\n"

    for r in results:
        res_str = "✅ PASS" if r["passed"] else "❌ FAIL"
        report += f"| {r['name']} | `{r['cmd']}` | {res_str} |\n"

    if failed_steps > 0:
        report += f"\n## Failure Diagnostics & Self-Healing Insights\n\n"
        for r in results:
            if not r["passed"]:
                report += f"### ❌ Failure in `{r['name']}`\n"
                report += f"**Exit Code:** `{r['returncode']}`\n\n"
                report += f"**Output:**\n```text\n{r['stderr'] or r['stdout']}\n```\n\n"

    full_path = os.path.join(REPO_ROOT, report_path)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(report)

    return report

def attempt_auto_fix(results: List[Dict[str, Any]]) -> bool:
    """Attempts self-healing routines if failures are detected."""
    failures = [r for r in results if not r["passed"]]
    if not failures:
        print("[+] No failures detected. System is healthy.")
        return True

    print(f"[!] Detected {len(failures)} failure(s). Executing auto-heal routines...")
    fixed_any = False

    for f in failures:
        if "TypeScript Lint" in f["name"]:
            print("[*] Attempting auto-fix for TypeScript / package environment...")
            # Run npm install or verify
            res = subprocess.run(["pnpm", "install"], cwd=REPO_ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if res.returncode == 0:
                fixed_any = True

    return fixed_any

def main():
    parser = argparse.ArgumentParser(description="Forge Automated Development & Self-Healing Assistant Bot")
    parser.add_argument("--check", action="store_true", help="Execute all portable tests and checks")
    parser.add_argument("--report", action="store_true", help="Generate AUTO_DEV_REPORT.md report")
    parser.add_argument("--auto-fix", action="store_true", help="Attempt self-healing fixes for identified failures")
    args = parser.parse_args()

    print("===================================================================")
    print("🤖 Team Forge Automated Development & Self-Healing Assistant Bot")
    print("===================================================================")

    results = run_all_checks()
    all_passed = all(r["passed"] for r in results)

    if args.report or not all_passed or args.check:
        generate_report(results)
        print(f"[+] Diagnostic report written to AUTO_DEV_REPORT.md")

    if args.auto_fix and not all_passed:
        attempt_auto_fix(results)

    if all_passed:
        print("\n✅ All portable test suites passed successfully!")
        sys.exit(0)
    else:
        print("\n❌ One or more test suites failed. Check AUTO_DEV_REPORT.md for diagnostics.")
        sys.exit(1)

if __name__ == "__main__":
    main()
