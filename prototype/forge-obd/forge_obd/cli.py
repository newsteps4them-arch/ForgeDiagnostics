"""Command-line entry point: python -m forge_obd.cli diagnose [--simulator] [--port PORT]"""
from __future__ import annotations

import argparse
import json
import sys

from .adapter import read_fault_codes
from .diagnosis import diagnose_codes


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(
        prog="forge-obd",
        description="Read OBD-II fault codes and explain them in plain English.",
    )
    parser.add_argument("command", choices=["diagnose"], help="action to run")
    parser.add_argument(
        "--simulator",
        action="store_true",
        help="Use the labeled simulator instead of a real adapter.",
    )
    parser.add_argument(
        "--port",
        default=None,
        help="Serial port for the ELM327 adapter (e.g. /dev/ttyUSB0).",
    )
    args = parser.parse_args(argv)

    use_simulator = args.simulator or not args.port
    if use_simulator and args.port:
        print("Note: --port ignored because --simulator was given.", file=sys.stderr)
    try:
        readings = read_fault_codes(use_simulator=use_simulator, port=args.port)
    except RuntimeError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 2

    print(json.dumps(diagnose_codes(readings), indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
