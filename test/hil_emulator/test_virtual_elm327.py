import subprocess
import sys
import unittest
from pathlib import Path


HARNESS = Path(__file__).with_name("virtual_elm327.py")


class VirtualElm327SmokeTest(unittest.TestCase):
    def run_commands(self, *commands):
        process = subprocess.Popen(
            [sys.executable, str(HARNESS)],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        output, error = process.communicate("".join(f"{command}\n" for command in commands), timeout=5)
        self.assertIn("Virtual ECU online", error)
        self.assertEqual(0, process.returncode)
        return output

    def test_read_only_obd_responses(self):
        output = self.run_commands("010C", "010D", "0105", "03")
        self.assertIn("41 0C 0F A0", output)
        self.assertIn("41 0D 37", output)
        self.assertIn("41 05 7B", output)
        self.assertIn("43 02 04 20 03 00", output)

    def test_extended_obd_modes_and_pids(self):
        output = self.run_commands("0106", "0107", "0142", "020200", "07", "0A")
        self.assertIn("41 06 80", output)
        self.assertIn("41 07 90", output)
        self.assertIn("41 42 37 D8", output)
        self.assertIn("42 02 04 20", output)
        self.assertIn("47 01 71 03 00", output)
        self.assertIn("4A 04 20 01 28", output)

    def test_clear_and_unsupported_commands(self):
        output = self.run_commands("04", "0122")
        self.assertIn("44 00", output)
        self.assertIn("7F 01 12", output)


if __name__ == "__main__":
    unittest.main()
