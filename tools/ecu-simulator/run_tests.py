#!/usr/bin/env python3
"""Run every ECU simulator unittest suite with stable import paths."""

from pathlib import Path
import sys
import unittest


SIMULATOR_ROOT = Path(__file__).resolve().parent
TEST_ROOT = SIMULATOR_ROOT / "tests"


def main() -> int:
    sys.path.insert(0, str(SIMULATOR_ROOT))

    loader = unittest.TestLoader()
    suite = loader.discover(str(TEST_ROOT), pattern="test*.py", top_level_dir=str(SIMULATOR_ROOT))

    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
