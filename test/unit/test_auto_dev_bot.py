#!/usr/bin/env python3
import os
import sys
import unittest
from unittest.mock import patch, MagicMock

# Ensure repo root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from scripts.forge_auto_dev import run_step, generate_report, attempt_auto_fix

class TestAutoDevBot(unittest.TestCase):

    def test_run_step_success(self):
        result = run_step("Echo Test", [sys.executable, "-c", "print('hello')"])
        self.assertTrue(result["passed"])
        self.assertEqual(result["returncode"], 0)
        self.assertIn("hello", result["stdout"])

    def test_run_step_failure(self):
        result = run_step("Fail Test", [sys.executable, "-c", "import sys; sys.exit(2)"])
        self.assertFalse(result["passed"])
        self.assertEqual(result["returncode"], 2)

    def test_generate_report_passed(self):
        mock_results = [
            {"name": "Suite 1", "cmd": "cmd 1", "passed": True, "returncode": 0, "stdout": "ok", "stderr": ""},
            {"name": "Suite 2", "cmd": "cmd 2", "passed": True, "returncode": 0, "stdout": "ok", "stderr": ""}
        ]
        test_report_path = "test_auto_dev_report.md"
        try:
            report_content = generate_report(mock_results, report_path=test_report_path)
            self.assertIn("Overall Status:** `PASSED`", report_content)
            self.assertIn("Passed Suites:** `2/2`", report_content)
            self.assertTrue(os.path.exists(test_report_path))
        finally:
            if os.path.exists(test_report_path):
                os.remove(test_report_path)

    def test_generate_report_failed(self):
        mock_results = [
            {"name": "Suite 1", "cmd": "cmd 1", "passed": True, "returncode": 0, "stdout": "ok", "stderr": ""},
            {"name": "Suite 2", "cmd": "cmd 2", "passed": False, "returncode": 1, "stdout": "", "stderr": "Error happened"}
        ]
        test_report_path = "test_auto_dev_report_fail.md"
        try:
            report_content = generate_report(mock_results, report_path=test_report_path)
            self.assertIn("Overall Status:** `FAILED`", report_content)
            self.assertIn("Passed Suites:** `1/2`", report_content)
            self.assertIn("Error happened", report_content)
        finally:
            if os.path.exists(test_report_path):
                os.remove(test_report_path)

    def test_attempt_auto_fix_no_failures(self):
        mock_results = [
            {"name": "Suite 1", "cmd": "cmd 1", "passed": True, "returncode": 0, "stdout": "ok", "stderr": ""}
        ]
        result = attempt_auto_fix(mock_results)
        self.assertTrue(result)

if __name__ == "__main__":
    unittest.main()
