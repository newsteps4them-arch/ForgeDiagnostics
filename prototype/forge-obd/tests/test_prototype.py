"""Smoke tests for the ForgeDiagnostics OBD-II prototype. Run: python -m unittest discover -s tests -v"""
import unittest

from forge_obd.adapter import SimulatorAdapter, read_fault_codes
from forge_obd.diagnosis import diagnose_codes, explain_code, DISCLAIMER


class TestSimulator(unittest.TestCase):
    def test_simulator_labels_provenance(self):
        readings = SimulatorAdapter().get_fault_codes()
        self.assertTrue(readings)
        for r in readings:
            self.assertEqual(r.source, "simulator")

    def test_simulator_scenario_codes(self):
        codes = [r.code for r in SimulatorAdapter().get_fault_codes()]
        self.assertEqual(codes, ["P0300", "P0171"])


class TestDiagnosis(unittest.TestCase):
    def test_known_code_plain_english(self):
        text = explain_code("P0300")["explanation"].lower()
        self.assertIn("misfir", text)

    def test_unknown_code_does_not_invent(self):
        result = explain_code("P9999")
        self.assertIn("no built-in write-up", result["explanation"])

    def test_report_carries_simulator_label(self):
        report = diagnose_codes(read_fault_codes(use_simulator=True))
        self.assertEqual(report["source"], "simulator")
        self.assertIn("SIMULATOR", report["source_detail"])

    def test_empty_vehicle_response_is_not_zero_faults_claim(self):
        report = diagnose_codes([])
        self.assertEqual(report["fault_count"], 0)
        self.assertIn("no stored fault codes", report["verdict"])
        self.assertIn("not that the vehicle is fault-free", report["verdict"])

    def test_disclaimer_present(self):
        report = diagnose_codes(read_fault_codes(use_simulator=True))
        self.assertEqual(report["disclaimer"], DISCLAIMER)
        self.assertIn("not a repair instruction", report["disclaimer"].lower())


if __name__ == "__main__":
    unittest.main()
