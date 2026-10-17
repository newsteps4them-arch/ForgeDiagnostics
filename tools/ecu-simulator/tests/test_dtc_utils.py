import unittest
import sys
import os

# Ensure tools/ecu-simulator is in python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import dtc_utils


class TestDtcUtils(unittest.TestCase):
    def test_is_dtc_valid(self):
        self.assertTrue(dtc_utils.is_dtc_valid("P0300"))
        self.assertTrue(dtc_utils.is_dtc_valid("C0171"))
        self.assertTrue(dtc_utils.is_dtc_valid("B1234"))
        self.assertTrue(dtc_utils.is_dtc_valid("U3FFF"))
        self.assertTrue(dtc_utils.is_dtc_valid("p0300"))

        self.assertFalse(dtc_utils.is_dtc_valid("INVALID"))
        self.assertFalse(dtc_utils.is_dtc_valid("P030"))
        self.assertFalse(dtc_utils.is_dtc_valid("X0300"))
        self.assertFalse(dtc_utils.is_dtc_valid("P9300"))
        self.assertFalse(dtc_utils.is_dtc_valid("P030G"))
        self.assertFalse(dtc_utils.is_dtc_valid(12345))
        self.assertFalse(dtc_utils.is_dtc_valid(None))

    def test_encode_obd_dtcs(self):
        encoded = dtc_utils.encode_obd_dtcs(["P0300", "C0171", "INVALID"])
        # P0300 -> 0x03 0x00
        # C0171 -> 0x41 0x71
        self.assertEqual(encoded, bytes([0x03, 0x00, 0x41, 0x71]))

    def test_encode_uds_dtcs(self):
        encoded = dtc_utils.encode_uds_dtcs(["P0300"])
        # P0300 -> 0x03 0x00 0x01 0x2F
        self.assertEqual(encoded, bytes([0x03, 0x00, 0x01, 0x2F]))

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("A"))
        self.assertTrue(dtc_utils.is_hex_value("f"))
        self.assertFalse(dtc_utils.is_hex_value("G"))
        self.assertFalse(dtc_utils.is_hex_value("-"))


if __name__ == "__main__":
    unittest.main()
