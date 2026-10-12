import unittest
import sys
import os

# Ensure tools/ecu-simulator is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import dtc_utils


class TestDtcUtils(unittest.TestCase):

    def test_is_dtc_valid(self):
        valid_dtcs = ['P0100', 'C0123', 'B0234', 'U0345', 'P1234', 'C3999']
        for dtc in valid_dtcs:
            self.assertTrue(dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be valid")

        invalid_dtcs = ['P010', 'P01000', 'X0100', 'P4100', 'P010G', 'c3999', '', None, 12345]
        for dtc in invalid_dtcs:
            self.assertFalse(dtc_utils.is_dtc_valid(dtc), f"Expected {dtc} to be invalid")

    def test_get_dtc_first_byte(self):
        # P0100 -> P (0x00) | 0 (0x00) | 1 (0x01) -> 0x01
        self.assertEqual(dtc_utils.get_dtc_first_byte('P0100'), b'\x01')
        # C0123 -> C (0x40) | 0 (0x00) | 1 (0x01) -> 0x41
        self.assertEqual(dtc_utils.get_dtc_first_byte('C0123'), b'\x41')
        # B0234 -> B (0x80) | 0 (0x00) | 2 (0x02) -> 0x82
        self.assertEqual(dtc_utils.get_dtc_first_byte('B0234'), b'\x82')
        # U0345 -> U (0xC0) | 0 (0x00) | 3 (0x03) -> 0xC3
        self.assertEqual(dtc_utils.get_dtc_first_byte('U0345'), b'\xc3')

    def test_get_dtc_second_byte(self):
        # P0100 -> '0' (0), '0' (0) -> 0x00
        self.assertEqual(dtc_utils.get_dtc_second_byte('P0100'), b'\x00')
        # C0123 -> '2' (2), '3' (3) -> 0x23
        self.assertEqual(dtc_utils.get_dtc_second_byte('C0123'), b'\x23')
        # P12AB -> 'A' (10), 'B' (11) -> 0xAB
        self.assertEqual(dtc_utils.get_dtc_second_byte('P12AB'), b'\xab')

    def test_encode_obd_dtcs(self):
        dtcs = ['P0100', 'C0123', 'INVALID', 'B0234']
        encoded = dtc_utils.encode_obd_dtcs(dtcs)
        # Expected:
        # P0100 -> b'\x01\x00'
        # C0123 -> b'\x41\x23'
        # INVALID -> skipped
        # B0234 -> b'\x82\x34'
        expected = b'\x01\x00\x41\x23\x82\x34'
        self.assertEqual(encoded, expected)

    def test_encode_uds_dtcs(self):
        dtcs = ['P0100']
        encoded = dtc_utils.encode_uds_dtcs(dtcs)
        # Expected: 4 bytes per DTC (1st byte, 2nd byte, high byte 0x01, status 0x2F)
        expected = b'\x01\x00\x01\x2f'
        self.assertEqual(encoded, expected)

    def test_is_hex_value(self):
        valid_hex = ['0', '1', '9', 'A', 'F', 'a', 'f']
        for h in valid_hex:
            self.assertTrue(dtc_utils.is_hex_value(h))

        invalid_hex = ['G', '10', '', None, 1]
        for h in invalid_hex:
            self.assertFalse(dtc_utils.is_hex_value(h))


if __name__ == '__main__':
    unittest.main()
