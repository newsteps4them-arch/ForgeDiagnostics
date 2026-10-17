import unittest
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import dtc_utils


class TestDtcUtils(unittest.TestCase):

    def test_is_dtc_valid_positive(self):
        valid_dtcs = ["P0300", "C0123", "B1234", "U0100", "p0100", "c0171"]
        for dtc in valid_dtcs:
            self.assertTrue(dtc_utils.is_dtc_valid(dtc.upper()), f"Expected {dtc} to be valid")

    def test_is_dtc_valid_negative(self):
        invalid_dtcs = [
            "P030",      # too short
            "P03000",    # too long
            "X0300",     # invalid prefix
            "P9300",     # invalid DTC type digit
            "P0G00",     # non-hex char
            "",          # empty
            12345,       # non-string
            None         # None
        ]
        for dtc in invalid_dtcs:
            self.assertFalse(dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be invalid")

    def test_get_dtc_first_byte(self):
        # P0300: P=00, 0=00 -> 0x00 | 0x03 -> 0x03
        self.assertEqual(b"\x03", dtc_utils.get_dtc_first_byte("P0300"))
        # C0171: C=01 (0x40), 0=00 -> 0x40 | 0x01 -> 0x41
        self.assertEqual(b"\x41", dtc_utils.get_dtc_first_byte("C0171"))
        # B1234: B=10 (0x80), 1=01 (0x10) -> 0x90 | 0x02 -> 0x92
        self.assertEqual(b"\x92", dtc_utils.get_dtc_first_byte("B1234"))
        # U0100: U=11 (0xC0), 0=00 -> 0xC0 | 0x01 -> 0xC1
        self.assertEqual(b"\xC1", dtc_utils.get_dtc_first_byte("U0100"))

    def test_get_dtc_second_byte(self):
        # P0300: 00 -> 0x00
        self.assertEqual(b"\x00", dtc_utils.get_dtc_second_byte("P0300"))
        # C0171: 71 -> 0x71
        self.assertEqual(b"\x71", dtc_utils.get_dtc_second_byte("C0171"))
        # B1234: 34 -> 0x34
        self.assertEqual(b"\x34", dtc_utils.get_dtc_second_byte("B1234"))

    def test_encode_obd_dtcs(self):
        dtcs = ["P0300", "INVALID", "C0171"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # Expected: P0300 (0x03, 0x00) + C0171 (0x41, 0x71)
        self.assertEqual(b"\x03\x00\x41\x71", encoded)

    def test_encode_uds_dtcs(self):
        dtcs = ["P0300"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # Expected: P0300 (0x03, 0x00) + 0x01 + 0x2F
        self.assertEqual(b"\x03\x00\x01\x2f", encoded)

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("F"))
        self.assertTrue(dtc_utils.is_hex_value("a"))
        self.assertFalse(dtc_utils.is_hex_value("G"))
        self.assertFalse(dtc_utils.is_hex_value("z"))


if __name__ == "__main__":
    unittest.main()
