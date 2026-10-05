import unittest
import os
import sys

# Add parent directory to path so dtc_utils can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import dtc_utils


class TestDtcUtils(unittest.TestCase):

    def test_is_hex_value(self):
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

    def test_encode_uds_dtcs(self):
        dtcs = ["P0300"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # Expected: P0300 (0x03, 0x00) + 0x01 + 0x2F
        self.assertEqual(b'\x03\x00\x01\x2F', encoded)


if __name__ == "__main__":
    unittest.main()
