"""Command-line entry point: python -m forge_obd.cli diagnose [--simulator] [--port PORT]"""
from __future__ import annotations

import argparse
import json
import os
import sys

from . import consent
from .adapter import read_fault_codes
from .diagnosis import diagnose_codes


def _ensure_llm_consent() -> None:
    """Ask for Claude-explanation consent before the first Claude call.

    The "screen" is consent.DISCLOSURE printed to stderr, followed by a
    y/N prompt. Rules:
    - No API key, or a choice already on record: never ask.
    - Not an interactive terminal: never ask and never opt in silently —
      the diagnosis layer reports "consent_required" and uses built-ins.
    - The answer is saved (grant or decline) so the user is asked once.
    """
    if not os.environ.get("FORGE_OBD_LLM_KEY"):
        return
    if consent.get_consent() != "unknown":
        return
    if not sys.stdin.isatty():
        return
    print(consent.DISCLOSURE, file=sys.stderr)
    # Prompt on stderr (not via input(), which prompts on stdout) so the
    # JSON report on stdout stays machine-readable.
    print("Allow Claude explanations? [y/N] ", end="", file=sys.stderr,
          flush=True)
    try:
        answer = sys.stdin.readline()
    except (EOFError, OSError):
        return
    if not answer:  # EOF — treat as "not now", record nothing
        return
    granted = answer.strip().lower() in ("y", "yes")
    consent.record_consent(granted)
    if not granted:
        print(
            "Okay — built-in explanations only. Nothing will be sent to Claude.",
            file=sys.stderr,
        )


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
    parser.add_argument(
        "--no-llm",
        action="store_true",
        help="Built-in explanations only for this run, even if Claude "
             "explanations were allowed before. Nothing is sent to Claude.",
    )
    args = parser.parse_args(argv)

    if args.no_llm:
        consent.set_run_override("declined")

    use_simulator = args.simulator or not args.port
    if use_simulator and args.port:
        print("Note: --port ignored because --simulator was given.", file=sys.stderr)
    try:
        readings = read_fault_codes(use_simulator=use_simulator, port=args.port)
    except RuntimeError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 2

    _ensure_llm_consent()
    print(json.dumps(diagnose_codes(readings), indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
