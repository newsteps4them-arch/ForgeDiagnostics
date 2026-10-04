import unittest
import dtc_utils


class TestDtcUtils(unittest.TestCase):
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
