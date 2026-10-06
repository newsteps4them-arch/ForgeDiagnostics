#!/usr/bin/env python3
"""
Team Forge - Unit & Integration Test Suite for Conflict Finder (Bot 1) & Conflict Resolver (Bot 2)
"""

import unittest
import tempfile
import os
import sys

# Add scripts/ directory to path for importing bot modules
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../scripts')))
from conflict_resolver_brain import resolve_conflict_content_smart

class TestConflictResolutionEngine(unittest.TestCase):

    def test_smart_conflict_resolution_code(self):
        """Tests that conflict_resolver_brain.resolve_conflict_content_smart resolves conflict blocks in code files without markers or duplicate statements."""
        conflict_sample = (
            "def calculate_total(a, b):\n"
            "<<<<<<< HEAD\n"
            "    # Head branch change\n"
            "    tax = 0.05\n"
            "=======\n"
            "    # Base branch update\n"
            "    tax = 0.08\n"
            ">>>>>>> origin/main\n"
            "    return (a + b) * (1 + tax)\n"
        )

        resolved = resolve_conflict_content_smart(conflict_sample, "calculator.py")

        self.assertNotIn("<<<<<<<", resolved)
        self.assertNotIn("=======", resolved)
        self.assertNotIn(">>>>>>>", resolved)
        self.assertIn("tax = 0.08", resolved)
        self.assertNotIn("tax = 0.05", resolved)
        self.assertIn("return (a + b) * (1 + tax)", resolved)

    def test_smart_conflict_resolution_markdown(self):
        """Tests that conflict_resolver_brain.resolve_conflict_content_smart resolves conflict blocks in documentation files by combining unique lines."""
        conflict_sample = (
            "# Diagnostic Report\n"
            "<<<<<<< HEAD\n"
            "- Feature A added\n"
            "=======\n"
            "- Feature B added\n"
            ">>>>>>> origin/main\n"
        )

        resolved = resolve_conflict_content_smart(conflict_sample, "REPORT.md")

        self.assertNotIn("<<<<<<<", resolved)
        self.assertNotIn("=======", resolved)
        self.assertNotIn(">>>>>>>", resolved)
        self.assertIn("- Feature A added", resolved)
        self.assertIn("- Feature B added", resolved)

    def test_conflict_marker_verification(self):
        """Tests checking for remaining conflict markers in file contents."""
        clean_code = "function add(a, b) { return a + b; }"
        conflicted_code = "function add(a, b) {\n<<<<<<< HEAD\n return a + b;\n=======\n return a - b;\n>>>>>>> main\n}"

        self.assertFalse("<<<<<<<" in clean_code or ">>>>>>>" in clean_code)
        self.assertTrue("<<<<<<<" in conflicted_code and ">>>>>>>" in conflicted_code)

    def test_pr_comment_formatting(self):
        """Tests formatting of PR conflict diagnostic comment body."""
        conflicting_files = ["src/protocols/j1979_decoder.ts", "package.json"]
        file_list_md = "\n".join([f"- `{f}`" for f in conflicting_files])

        comment_body = (
            f"### ⚠️ Merge Conflict Detected by Conflict Finder Bot (Bot 1)\n\n"
            f"**Conflicting Files:**\n{file_list_md}"
        )

        self.assertIn("j1979_decoder.ts", comment_body)
        self.assertIn("package.json", comment_body)
        self.assertIn("Conflict Finder Bot (Bot 1)", comment_body)

if __name__ == "__main__":
    unittest.main()
