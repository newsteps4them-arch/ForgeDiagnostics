import unittest
import sys
import os

# Ensure tools/ecu-simulator is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import dtc_utils


class TestDtcUtils(unittest.TestCase):

    def test_is_hex_value(self):
        valid_hex = ["0", "5", "9", "A", "F", "a", "f"]
        invalid_hex = ["G", "z", "!", " ", "10", ""]

        for char in valid_hex:
            self.assertTrue(dtc_utils.is_hex_value(char), f"Expected {char} to be hex")

        for char in invalid_hex:
            self.assertFalse(dtc_utils.is_hex_value(char), f"Expected {char} not to be hex")

    def test_is_dtc_valid(self):
        valid_dtcs = ["P0100", "C0171", "B0001", "U0100", "P0300", "C1234", "B0F1A", "P0f1a"]
        invalid_dtcs = [
            "P01000",   # Length 6
            "P010",    # Length 4
            "X0100",   # Invalid prefix
            "p0100",   # Lowercase prefix
            "P4100",   # Invalid type digit (must be 0-3)
            "P0G00",   # Non-hex digit
            12345,     # Non-string
            None,      # None
            "",        # Empty string
        ]

        for dtc in valid_dtcs:
            self.assertTrue(dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be valid")

        for dtc in invalid_dtcs:
            self.assertFalse(dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be invalid")

    def test_get_dtc_bytes(self):
        # P0100 -> P(00), 0(00), 1 -> byte 0x01; 00 -> byte 0x00
        self.assertEqual(dtc_utils.get_dtc_first_byte("P0100"), b"\x01")
        self.assertEqual(dtc_utils.get_dtc_second_byte("P0100"), b"\x00")

        # C0171 -> C(01), 0(00), 1 -> byte 0x41; 71 -> byte 0x71
        self.assertEqual(dtc_utils.get_dtc_first_byte("C0171"), b"\x41")
        self.assertEqual(dtc_utils.get_dtc_second_byte("C0171"), b"\x71")

        # B0001 -> B(10), 0(00), 0 -> byte 0x80; 01 -> byte 0x01
        self.assertEqual(dtc_utils.get_dtc_first_byte("B0001"), b"\x80")
        self.assertEqual(dtc_utils.get_dtc_second_byte("B0001"), b"\x01")

        # U0100 -> U(11), 0(00), 1 -> byte 0xC1; 00 -> byte 0x00
        self.assertEqual(dtc_utils.get_dtc_first_byte("U0100"), b"\xC1")
        self.assertEqual(dtc_utils.get_dtc_second_byte("U0100"), b"\x00")

    def test_encode_obd_dtcs(self):
        dtcs = ["P0100", "C0171", "INVALID"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # P0100 -> 0x01, 0x00; C0171 -> 0x41, 0x71; INVALID skipped
        expected = bytearray([0x01, 0x00, 0x41, 0x71])
        self.assertEqual(encoded, expected)

    def test_encode_uds_dtcs(self):
        dtcs = ["P0100", "INVALID"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # P0100 -> 0x01, 0x00, status high byte 0x01, default status 0x2F
        expected = bytearray([0x01, 0x00, 0x01, 0x2F])
        self.assertEqual(encoded, expected)


if __name__ == "__main__":
    unittest.main()
