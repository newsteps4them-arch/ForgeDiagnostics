import unittest
import os
import sys

# Add parent directory to path so dtc_utils can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import sys
import os

# Ensure tools/ecu-simulator is in python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

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
        self.assertEqual(b'\x03\x00\x01\x2F', encoded)
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
        # Expected bytes: P0300 -> 0x03, 0x00, 0x01, 0x2F
        expected = bytes([0x03, 0x00, 0x01, 0x2F])
        self.assertEqual(encoded, expected)

    def test_get_dtc_bytes(self):
        self.assertEqual(dtc_utils.get_dtc_first_byte("P0300"), bytes([0x03]))
        self.assertEqual(dtc_utils.get_dtc_second_byte("P0300"), bytes([0x00]))
    def test_is_dtc_valid(self):
        self.assertTrue(dtc_utils.is_dtc_valid("P0300"))
        self.assertTrue(dtc_utils.is_dtc_valid("C0171"))
        self.assertTrue(dtc_utils.is_dtc_valid("B1234"))
        self.assertTrue(dtc_utils.is_dtc_valid("U3FFF"))
        self.assertTrue(dtc_utils.is_dtc_valid("p0300"))

        self.assertFalse(dtc_utils.is_dtc_valid("INVALID"))
        self.assertFalse(dtc_utils.is_dtc_valid("P030"))
        self.assertFalse(dtc_utils.is_dtc_valid("X0300"))
        self.assertFalse(dtc_utils.is_dtc_valid("P9300"))
        self.assertFalse(dtc_utils.is_dtc_valid("P030G"))
        self.assertFalse(dtc_utils.is_dtc_valid(12345))
        self.assertFalse(dtc_utils.is_dtc_valid(None))

    def test_encode_obd_dtcs(self):
        encoded = dtc_utils.encode_obd_dtcs(["P0300", "C0171", "INVALID"])
        # P0300 -> 0x03 0x00
        # C0171 -> 0x41 0x71
        self.assertEqual(encoded, bytes([0x03, 0x00, 0x41, 0x71]))

    def test_encode_uds_dtcs(self):
        encoded = dtc_utils.encode_uds_dtcs(["P0300"])
        # P0300 -> 0x03 0x00 0x01 0x2F
        self.assertEqual(encoded, bytes([0x03, 0x00, 0x01, 0x2F]))

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("A"))
        self.assertTrue(dtc_utils.is_hex_value("f"))
        self.assertTrue(dtc_utils.is_hex_value("a"))
        self.assertFalse(dtc_utils.is_hex_value("G"))
        self.assertEqual(b"\x03\x00\x01\x2f", encoded)

    def test_is_hex_value(self):
        self.assertTrue(dtc_utils.is_hex_value("0"))
        self.assertTrue(dtc_utils.is_hex_value("F"))
        self.assertTrue(dtc_utils.is_hex_value("a"))
        self.assertFalse(dtc_utils.is_hex_value("G"))
        self.assertFalse(dtc_utils.is_hex_value("z"))
        self.assertFalse(dtc_utils.is_hex_value("G"))
        self.assertFalse(dtc_utils.is_hex_value("-"))
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
        self.assertTrue(dtc_utils.is_dtc_valid("p0171"))

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
