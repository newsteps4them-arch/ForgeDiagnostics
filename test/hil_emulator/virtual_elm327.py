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
    "0106": "41 06 80\r\n>",          # STFT1: 0%
    "0107": "41 07 90\r\n>",          # LTFT1: +12.5%
    "010C": "41 0C 0F A0\r\n>",       # 1000 RPM
    "010D": "41 0D 37\r\n>",          # 55 km/h
    "0105": "41 05 7B\r\n>",          # 83 deg C
    "0142": "41 42 37 D8\r\n>",       # Control Module Voltage: 14.3V
    "020200": "42 02 04 20\r\n>",     # Freeze frame DTC P0420
    "03":   "43 02 04 20 03 00\r\n>", # Stored DTCs: P0420, P0300
    "04":   "44 00\r\n>",             # Clear DTCs OK
    "07":   "47 01 71 03 00\r\n>",    # Pending DTCs: P0171, P0300
    "0A":   "4A 04 20 01 28\r\n>"     # Permanent DTCs: P0420, P0128
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
