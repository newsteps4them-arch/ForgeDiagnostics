#!/usr/bin/env python3
"""
Team Forge - Unit Tests for Scheduled Diagnostic Data Reliability Bot
"""

import unittest
import sys
import os

# Add scripts directory to module search path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../scripts')))

import scheduled_reliability_bot as srb


class TestScheduledReliabilityBot(unittest.TestCase):

    def test_verify_j1979_formulas(self):
        """Tests SAE J1979 formula verification function."""
        success, logs = srb.verify_j1979_formulas()
        self.assertTrue(success)
        self.assertTrue(len(logs) > 0)
        self.assertIn("Engine RPM", logs[1])

    def test_verify_hil_emulator_stream(self):
        """Tests live stream communication with Virtual ELM327 HIL emulator."""
        success, logs = srb.verify_hil_emulator_stream()
        self.assertTrue(success)
        self.assertTrue(len(logs) > 0)
        self.assertIn("Command `010C`", logs[1])

    def test_gemini_analysis_fallback(self):
        """Tests Gemini analysis fallback string when API key is unconfigured."""
        analysis = srb.generate_gemini_analysis(True, "Sample Report")
        self.assertIsInstance(analysis, str)
        self.assertTrue(len(analysis) > 0)


if __name__ == "__main__":
    unittest.main()
