import unittest
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import dtc_utils


class TestDtcUtils(unittest.TestCase):
    def test_is_dtc_valid_positive(self):
        self.assertTrue(dtc_utils.is_dtc_valid("P0100"))
        self.assertTrue(dtc_utils.is_dtc_valid("P0300"))
        self.assertTrue(dtc_utils.is_dtc_valid("C0123"))
        self.assertTrue(dtc_utils.is_dtc_valid("B1000"))
        self.assertTrue(dtc_utils.is_dtc_valid("U0100"))
        self.assertTrue(dtc_utils.is_dtc_valid("P3FFF"))

    def test_is_dtc_valid_negative(self):
        self.assertFalse(dtc_utils.is_dtc_valid("INVALID"))
        self.assertFalse(dtc_utils.is_dtc_valid("P010"))
        self.assertFalse(dtc_utils.is_dtc_valid("P01000"))
        self.assertFalse(dtc_utils.is_dtc_valid("X0100"))
        self.assertFalse(dtc_utils.is_dtc_valid("P9100"))
        self.assertFalse(dtc_utils.is_dtc_valid("P0G00"))

    def test_encode_obd_dtcs(self):
        dtcs = ["P0100", "P0300", "INVALID"]
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # P0100 -> 0x01, 0x00
        # P0300 -> 0x03, 0x00
        self.assertEqual(encoded, bytes([0x01, 0x00, 0x03, 0x00]))

    def test_encode_uds_dtcs(self):
        dtcs = ["P0100"]
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # P0100 -> 0x01, 0x00, 0x01 (high byte), 0x2F (default status)
        self.assertEqual(encoded, bytes([0x01, 0x00, 0x01, 0x2F]))

    def test_get_dtc_bytes(self):
        b1 = dtc_utils.get_dtc_first_byte("P0100")
        b2 = dtc_utils.get_dtc_second_byte("P0100")
        self.assertEqual(b1, bytes([0x01]))
        self.assertEqual(b2, bytes([0x00]))

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("A"))
        self.assertTrue(dtc_utils.is_hex_value("f"))
        self.assertFalse(dtc_utils.is_hex_value("G"))


if __name__ == "__main__":
    unittest.main()
