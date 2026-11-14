#!/usr/bin/env python3
"""
Team Forge - Automated Release & Finalization Verifier Script

Executes comprehensive verification across TypeScript linting, Vitest unit tests,
Python ECU simulator tests, Python HIL emulator tests, and verifies essential workflow
and build configurations for release readiness.
"""

import sys
import subprocess
import os

def run_step(description, command):
    print(f"[CHECK] {description}...")
    try:
        res = subprocess.run(command, shell=True, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"[FAIL] {description}")
            print("STDOUT:", res.stdout)
            print("STDERR:", res.stderr)
            return False
        print(f"[OK] {description}")
        return True
    except Exception as e:
        print(f"[ERROR] Executing {description}: {e}")
        return False

def main():
    print("===================================================================")
    print("🚀 Team Forge Release Readiness & Automated Finalization Audit")
    print("===================================================================")

    steps = [
        ("TypeScript Compile / Lint Check", "npm run lint"),
        ("Vitest Diagnostic Suite", "npm test"),
        ("Python ECU Simulator Test Suite", "python3 tools/ecu-simulator/run_tests.py"),
        ("Python ELM327 HIL Emulator Test Suite", "python3 -m unittest discover -s test/hil_emulator -p 'test*.py'"),
        ("Bot Collective Unit Tests", "python3 -m unittest discover -s test/unit -p 'test*.py'"),
    ]

    failed = False
    for desc, cmd in steps:
        if not run_step(desc, cmd):
            failed = True

    # Validate key files existence
    critical_files = [
        "app/build.gradle.kts",
        "ROADMAP_AUTOMOTIVE_DIAGNOSTICS.md",
        "WORKFLOW_AUTOMATION_GUIDE.md",
        ".github/workflows/autonomous-guardian.yml",
        ".github/workflows/conflict_finder_bot.yml",
        ".github/workflows/conflict_resolver_bot.yml",
        "scripts/conflict_finder.py",
        "scripts/conflict_resolver_brain.py"
    ]

    print("[CHECK] Verifying Critical System Files...")
    missing = [f for f in critical_files if not os.path.exists(f)]
    if missing:
        print("[FAIL] Missing critical files:", missing)
        failed = True
    else:
        print("[OK] All critical system files verified.")

    print("===================================================================")
    if failed:
        print("❌ Release Readiness Audit FAILED. Resolve errors before proceeding.")
        sys.exit(1)
    else:
        print("✅ Release Readiness Audit PASSED! All automation & tests verified.")
        sys.exit(0)

if __name__ == "__main__":
    main()
