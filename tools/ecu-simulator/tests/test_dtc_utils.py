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
        self.assertTrue(dtc_utils.is_hex_value('0'))
        self.assertTrue(dtc_utils.is_hex_value('9'))
        self.assertTrue(dtc_utils.is_hex_value('A'))
        self.assertTrue(dtc_utils.is_hex_value('f'))
        self.assertFalse(dtc_utils.is_hex_value('G'))
        self.assertFalse(dtc_utils.is_hex_value('z'))
        self.assertFalse(dtc_utils.is_hex_value('-'))

    def test_is_dtc_valid(self):
        self.assertTrue(dtc_utils.is_dtc_valid("P0300"))
        self.assertTrue(dtc_utils.is_dtc_valid("C0035"))
        self.assertTrue(dtc_utils.is_dtc_valid("B1000"))
        self.assertTrue(dtc_utils.is_dtc_valid("U0100"))
        self.assertTrue(dtc_utils.is_dtc_valid("p0171"))

        # Invalid cases
        self.assertFalse(dtc_utils.is_dtc_valid("P030"))    # Too short
        self.assertFalse(dtc_utils.is_dtc_valid("P03000"))   # Too long
        self.assertFalse(dtc_utils.is_dtc_valid("X0300"))   # Invalid group prefix 'X'
        self.assertFalse(dtc_utils.is_dtc_valid("P5300"))   # Invalid type digit '5'
        self.assertFalse(dtc_utils.is_dtc_valid("P030Z"))   # Invalid hex char 'Z'

    def test_get_dtc_bytes(self):
        # P0300 -> 0x03, 0x00
        self.assertEqual(b'\x03', dtc_utils.get_dtc_first_byte("P0300"))
        self.assertEqual(b'\x00', dtc_utils.get_dtc_second_byte("P0300"))

        # C0035 -> 0x40, 0x35
        self.assertEqual(b'\x40', dtc_utils.get_dtc_first_byte("C0035"))
        self.assertEqual(b'\x35', dtc_utils.get_dtc_second_byte("C0035"))

        # B1000 -> 0x90, 0x00
        self.assertEqual(b'\x90', dtc_utils.get_dtc_first_byte("B1000"))
        self.assertEqual(b'\x00', dtc_utils.get_dtc_second_byte("B1000"))

        # U0100 -> 0xC1, 0x00
        self.assertEqual(b'\xC1', dtc_utils.get_dtc_first_byte("U0100"))
        self.assertEqual(b'\x00', dtc_utils.get_dtc_second_byte("U0100"))

    def test_encode_obd_dtcs(self):
        dtcs = ["P0300", "INVALID", "C0035"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # Expected: P0300 (0x03, 0x00) + C0035 (0x40, 0x35)
        self.assertEqual(b'\x03\x00\x40\x35', encoded)
    def test_is_dtc_valid(self):
        self.assertTrue(dtc_utils.is_dtc_valid("P0300"))
        self.assertTrue(dtc_utils.is_dtc_valid("C0123"))
        self.assertTrue(dtc_utils.is_dtc_valid("B0001"))
        self.assertTrue(dtc_utils.is_dtc_valid("U0100"))
        self.assertTrue(dtc_utils.is_dtc_valid("P1171"))
        self.assertFalse(dtc_utils.is_dtc_valid("P030"))
        self.assertFalse(dtc_utils.is_dtc_valid("P03000"))
        self.assertFalse(dtc_utils.is_dtc_valid("X0300"))
        self.assertFalse(dtc_utils.is_dtc_valid("P9300"))
        self.assertFalse(dtc_utils.is_dtc_valid("P030Z"))

    def test_encode_obd_dtcs(self):
        dtcs = ["P0300", "C0123", "B0001", "U0100", "INVALID"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # Expected bytes:
        # P0300 -> 0x03, 0x00
        # C0123 -> 0x41, 0x23
        # B0001 -> 0x80, 0x01
        # U0100 -> 0xC1, 0x00
        expected = bytes([0x03, 0x00, 0x41, 0x23, 0x80, 0x01, 0xC1, 0x00])
        self.assertEqual(encoded, expected)

    def test_encode_uds_dtcs(self):
        dtcs = ["P0300"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # Expected: P0300 (0x03, 0x00) + 0x01 + 0x2F
        self.assertEqual(b'\x03\x00\x01\x2F', encoded)
    def test_is_dtc_valid(self):
        self.assertTrue(dtc_utils.is_dtc_valid("P0133"))
        self.assertTrue(dtc_utils.is_dtc_valid("C0171"))
        self.assertTrue(dtc_utils.is_dtc_valid("B0001"))
        self.assertTrue(dtc_utils.is_dtc_valid("U0100"))
        self.assertTrue(dtc_utils.is_dtc_valid("P0300"))

        self.assertFalse(dtc_utils.is_dtc_valid("P01333"))  # Length > 5
        self.assertFalse(dtc_utils.is_dtc_valid("P013"))   # Length < 5
        self.assertFalse(dtc_utils.is_dtc_valid("X0133"))  # Invalid group
        self.assertFalse(dtc_utils.is_dtc_valid("P4133"))  # Invalid type
        self.assertFalse(dtc_utils.is_dtc_valid("P0G33"))  # Invalid hex digit

    def test_encode_obd_dtcs(self):
        dtcs = ["P0133", "C0171", "INVALID", "U0100"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # P0133 -> [0x01, 0x33], C0171 -> [0x41, 0x71], U0100 -> [0xC1, 0x00]
        expected = bytearray([0x01, 0x33, 0x41, 0x71, 0xC1, 0x00])
        self.assertEqual(encoded, expected)

    def test_encode_uds_dtcs(self):
        dtcs = ["P0133", "INVALID"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # P0133 -> [0x01, 0x33, 0x01, 0x2F]
        expected = bytearray([0x01, 0x33, 0x01, 0x2F])
        self.assertEqual(encoded, expected)

    def test_get_dtc_bytes(self):
        self.assertEqual(dtc_utils.get_dtc_first_byte("P0133"), bytes([0x01]))
        self.assertEqual(dtc_utils.get_dtc_second_byte("P0133"), bytes([0x33]))
        # Expected bytes: P0300 -> 0x03, 0x00, 0x01, 0x2F
        expected = bytes([0x03, 0x00, 0x01, 0x2F])
        self.assertEqual(encoded, expected)

    def test_get_dtc_bytes(self):
        self.assertEqual(dtc_utils.get_dtc_first_byte("P0300"), bytes([0x03]))
        self.assertEqual(dtc_utils.get_dtc_second_byte("P0300"), bytes([0x00]))

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("A"))
        self.assertTrue(dtc_utils.is_hex_value("a"))
        self.assertFalse(dtc_utils.is_hex_value("G"))


if __name__ == "__main__":
    unittest.main()
