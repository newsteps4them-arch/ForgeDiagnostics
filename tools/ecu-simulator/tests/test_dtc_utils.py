import unittest
import dtc_utils


class TestDtcUtils(unittest.TestCase):

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


if __name__ == "__main__":
    unittest.main()
