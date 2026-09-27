import unittest
import os
import sys

# Add parent directory to path so dtc_utils can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import dtc_utils


class TestDtcUtils(unittest.TestCase):

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("f"))
        self.assertTrue(dtc_utils.is_hex_value("A"))
        self.assertFalse(dtc_utils.is_hex_value("g"))
        self.assertFalse(dtc_utils.is_hex_value("Z"))
        self.assertFalse(dtc_utils.is_hex_value(""))

    def test_is_dtc_valid(self):
        # Valid DTCs for all groups (P, C, B, U) and types (0, 1, 2, 3)
        self.assertTrue(dtc_utils.is_dtc_valid("P0001"))
        self.assertTrue(dtc_utils.is_dtc_valid("B1477"))
        self.assertTrue(dtc_utils.is_dtc_valid("C2123"))
        self.assertTrue(dtc_utils.is_dtc_valid("U3FFF"))

        # Invalid lengths
        self.assertFalse(dtc_utils.is_dtc_valid("P001"))
        self.assertFalse(dtc_utils.is_dtc_valid("P00001"))
        self.assertFalse(dtc_utils.is_dtc_valid(""))

        # Invalid group prefix
        self.assertFalse(dtc_utils.is_dtc_valid("X0001"))
        self.assertFalse(dtc_utils.is_dtc_valid("10001"))

        # Invalid type digit
        self.assertFalse(dtc_utils.is_dtc_valid("P4001"))
        self.assertFalse(dtc_utils.is_dtc_valid("PA001"))

        # Non-hex characters in last 3 characters
        self.assertFalse(dtc_utils.is_dtc_valid("P0G01"))
        self.assertFalse(dtc_utils.is_dtc_valid("P00X1"))
        self.assertFalse(dtc_utils.is_dtc_valid("P000Z"))

    def test_get_dtc_first_byte(self):
        # P0001 -> P=00, 0=00 -> 0x00 | 0 = 0x00
        self.assertEqual(dtc_utils.get_dtc_first_byte("P0001"), bytes([0x00]))
        # B1477 -> B=10, 1=01 -> 0x90 | 0x04 = 0x94
        self.assertEqual(dtc_utils.get_dtc_first_byte("B1477"), bytes([0x94]))
        # C2123 -> C=01, 2=10 -> 0x60 | 0x01 = 0x61
        self.assertEqual(dtc_utils.get_dtc_first_byte("C2123"), bytes([0x61]))
        # U3FFF -> U=11, 3=11 -> 0xF0 | 0x0F = 0xFF
        self.assertEqual(dtc_utils.get_dtc_first_byte("U3FFF"), bytes([0xFF]))

    def test_get_dtc_second_byte(self):
        self.assertEqual(dtc_utils.get_dtc_second_byte("P0001"), bytes([0x01]))
        self.assertEqual(dtc_utils.get_dtc_second_byte("B1477"), bytes([0x77]))
        self.assertEqual(dtc_utils.get_dtc_second_byte("C2123"), bytes([0x23]))
        self.assertEqual(dtc_utils.get_dtc_second_byte("U3FFF"), bytes([0xFF]))

    def test_encode_obd_dtcs(self):
        # Empty list
        self.assertEqual(dtc_utils.encode_obd_dtcs([]), bytearray())

        # Single valid DTC
        expected_p0001 = bytearray([0x00, 0x01])
        self.assertEqual(dtc_utils.encode_obd_dtcs(["P0001"]), expected_p0001)

        # Multiple valid DTCs
        expected_multi = bytearray([0x94, 0x77, 0x00, 0x01])
        self.assertEqual(dtc_utils.encode_obd_dtcs(["B1477", "P0001"]), expected_multi)

        # Mixed valid and invalid DTCs
        self.assertEqual(
            dtc_utils.encode_obd_dtcs(["INVALID", "B1477", "P001", "P0001", "X9999"]),
            expected_multi
        )

    def test_encode_uds_dtcs(self):
        # Empty list
        self.assertEqual(dtc_utils.encode_uds_dtcs([]), bytearray())

        # Single valid DTC (P0001 -> 0x00, 0x01, 0x01, 0x2F)
        expected_p0001 = bytearray([0x00, 0x01, 0x01, 0x2F])
        self.assertEqual(dtc_utils.encode_uds_dtcs(["P0001"]), expected_p0001)

        # Multiple valid DTCs (B1477 and P0001)
        expected_multi = bytearray([
            0x94, 0x77, 0x01, 0x2F,  # B1477
            0x00, 0x01, 0x01, 0x2F   # P0001
        ])
        self.assertEqual(dtc_utils.encode_uds_dtcs(["B1477", "P0001"]), expected_multi)

        # Mixed valid and invalid DTCs (filters out invalid DTCs)
        self.assertEqual(
            dtc_utils.encode_uds_dtcs(["BAD_DTC", "B1477", "P000", "P0001", "Z1234"]),
            expected_multi
        )


if __name__ == "__main__":
    unittest.main()
