import unittest
import os
import sys
from unittest.mock import patch

# Add parent directory to path so ecu_config can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import ecu_config


class TestEcuConfig(unittest.TestCase):

    def test_get_vin(self):
        self.assertEqual("TESTVIN0123456789", ecu_config.get_vin())

    def test_get_ecu_name(self):
        self.assertEqual("ECU_SIMULATOR", ecu_config.get_ecu_name())

    def test_get_fuel_level(self):
        self.assertEqual(50, ecu_config.get_fuel_level())

    def test_get_fuel_type(self):
        self.assertEqual(1, ecu_config.get_fuel_type())

    def test_get_dtcs(self):
        self.assertEqual(["B1477", "P0001"], ecu_config.get_dtcs())

    def test_get_obd_broadcast_address(self):
        self.assertEqual(0x7DF, ecu_config.get_obd_broadcast_address())

    def test_get_obd_ecu_address(self):
        self.assertEqual(0x7E0, ecu_config.get_obd_ecu_address())

    def test_get_uds_ecu_address(self):
        self.assertEqual(0x7E1, ecu_config.get_uds_ecu_address())

    def test_create_address_valid(self):
        self.assertEqual(2015, ecu_config.create_address("0x7DF"))
        self.assertEqual(2016, ecu_config.create_address("0x7E0"))
        self.assertEqual(0, ecu_config.create_address("0x0"))

    @patch("builtins.print")
    def test_create_address_invalid_raises_system_exit(self, mock_print):
        with self.assertRaises(SystemExit) as cm:
            ecu_config.create_address("invalid_hex")
        self.assertEqual(cm.exception.code, 1)
        mock_print.assert_called_once()

    def test_get_can_interface(self):
        self.assertEqual("vcan0", ecu_config.get_can_interface())

    def test_get_can_interface_type(self):
        self.assertEqual("virtual", ecu_config.get_can_interface_type())

    def test_get_can_bitrate(self):
        self.assertEqual("500000", ecu_config.get_can_bitrate())

    def test_get_isotp_ko_file_path(self):
        self.assertEqual(
            "/usr/lib/modules/5.3.0-kali2-amd64/kernel/net/can/can-isotp.ko",
            ecu_config.get_isotp_ko_file_path()
        )

    @patch("ecu_config.CONFIG", {
        "vin": {"value": "CUSTOM_VIN_12345"},
        "ecu_name": {"value": "CUSTOM_ECU"},
        "fuel_level": {"value": 75},
        "fuel_type": {"value": 2},
        "dtcs": {"value": ["P0300"]},
        "obd_broadcast_address": {"value": "0x100"},
        "obd_ecu_address": {"value": "0x200"},
        "uds_ecu_address": {"value": "0x300"},
        "can_interface": {"value": "can0"},
        "can_interface_type": {"value": "hardware"},
        "can_bitrate": {"value": "250000"},
        "isotp_ko_file_path": {"value": "/path/to/custom.ko"}
    })
    def test_mocked_config(self):
        self.assertEqual("CUSTOM_VIN_12345", ecu_config.get_vin())
        self.assertEqual("CUSTOM_ECU", ecu_config.get_ecu_name())
        self.assertEqual(75, ecu_config.get_fuel_level())
        self.assertEqual(2, ecu_config.get_fuel_type())
        self.assertEqual(["P0300"], ecu_config.get_dtcs())
        self.assertEqual(0x100, ecu_config.get_obd_broadcast_address())
        self.assertEqual(0x200, ecu_config.get_obd_ecu_address())
        self.assertEqual(0x300, ecu_config.get_uds_ecu_address())
        self.assertEqual("can0", ecu_config.get_can_interface())
        self.assertEqual("hardware", ecu_config.get_can_interface_type())
        self.assertEqual("250000", ecu_config.get_can_bitrate())
        self.assertEqual("/path/to/custom.ko", ecu_config.get_isotp_ko_file_path())


if __name__ == "__main__":
    unittest.main()
