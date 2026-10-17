import os
import sys
import unittest

# Add parent directory to path so dtc_utils can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import dtc_utils


class TestDtcUtils(unittest.TestCase):

    def test_is_hex_value(self):
        valid_chars = "0123456789ABCDEFabcdef"
        for char in valid_chars:
            self.assertTrue(
                dtc_utils.is_hex_value(char),
                f"Expected {char} to be recognized as hex",
            )

        invalid_chars = ["G", "z", "!", "-", " ", "10"]
        for char in invalid_chars:
            self.assertFalse(
                dtc_utils.is_hex_value(char),
                f"Expected {char} not to be recognized as single hex character",
            )

    def test_is_dtc_valid(self):
        valid_dtcs = ["P0001", "C0300", "B1477", "U0100", "P0100", "C0000"]
        for dtc in valid_dtcs:
            self.assertTrue(
                dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be valid DTC"
            )

        invalid_dtcs = [
            "P000",      # Too short
            "P00000",    # Too long
            "X0000",      # Invalid prefix
            "p0100",      # Lowercase prefix
            "P4000",      # Invalid type (must be 0-3)
            "P000G",      # Invalid hex char
            "",
        ]
        for dtc in invalid_dtcs:
            self.assertFalse(
                dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be invalid DTC"
            )

    def test_get_dtc_bytes(self):
        # "P0100" -> P=0x00, 0=0x00, 1=0x01 => First byte: 0x01, Second byte: 0x00
        self.assertEqual(dtc_utils.get_dtc_first_byte("P0100"), b"\x01")
        self.assertEqual(dtc_utils.get_dtc_second_byte("P0100"), b"\x00")

        # "C0300" -> C=0x40, 0=0x00, 3=0x03 => First byte: 0x43, Second byte: 0x00
        self.assertEqual(dtc_utils.get_dtc_first_byte("C0300"), b"\x43")
        self.assertEqual(dtc_utils.get_dtc_second_byte("C0300"), b"\x00")

        # "B1477" -> B=0x80, 1=0x10, 4=0x04 => First byte: 0x94, Second byte: 0x77
        self.assertEqual(dtc_utils.get_dtc_first_byte("B1477"), b"\x94")
        self.assertEqual(dtc_utils.get_dtc_second_byte("B1477"), b"\x77")

        # "U0100" -> U=0xC0, 0=0x00, 1=0x01 => First byte: 0xC1, Second byte: 0x00
        self.assertEqual(dtc_utils.get_dtc_first_byte("U0100"), b"\xC1")
        self.assertEqual(dtc_utils.get_dtc_second_byte("U0100"), b"\x00")

    def test_encode_obd_dtcs(self):
        dtcs = ["B1477", "INVALID", "P0001"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)

        # B1477 => 0x94 0x77
        # P0001 => 0x00 0x01
        expected = bytearray([0x94, 0x77, 0x00, 0x01])
        self.assertEqual(encoded, expected)

    def test_encode_uds_dtcs(self):
        dtcs = ["B1477"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)

        # B1477 => 0x94 0x77 0x01 0x2F
        expected = bytearray([0x94, 0x77, 0x01, 0x2F])
        self.assertEqual(encoded, expected)


if __name__ == "__main__":
    unittest.main()
