#!/usr/bin/env python3
"""
ForgeDiagnostic Virtual ELM327 & Vehicle ECU Test Harness
"""
import sys

RESPONSES = {
    "ATZ": "\r\rELM327 v1.5\r\n>",
    "ATE0": "OK\r\n>",
    "ATL0": "OK\r\n>",
    "ATS0": "OK\r\n>",
    "ATSP0": "OK\r\n>",
    "ATDP": "ISO 15765-4 (CAN 11/500)\r\n>",
    "ATRV": "12.6V\r\n>",
    "0100": "41 00 BE 3F B8 13\r\n>",
    "010C": "41 0C 0F A0\r\n>",       # 1000 RPM
    "010D": "41 0D 37\r\n>",          # 55 km/h
    "0105": "41 05 7B\r\n>",          # 83 deg C
    "03":   "43 02 04 20 03 00\r\n>", # P0420, P0300
    "04":   "44 00\r\n>"              # Clear DTCs OK
}

def main():
    sys.stderr.write("[HIL-Emulator] Virtual ECU online.\n")
    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                break
            cmd = line.strip().upper().replace(" ", "")
            if cmd in RESPONSES:
                sys.stdout.write(RESPONSES[cmd])
            else:
                sys.stdout.write("7F 01 12\r\n>")
            sys.stdout.flush()
        except KeyboardInterrupt:
            break

if __name__ == "__main__":
    main()
