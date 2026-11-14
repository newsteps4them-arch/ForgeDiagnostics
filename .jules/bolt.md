# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - ECU Simulator DTC Encoding Exception & String Parsing Bottleneck

**Learning:** In `tools/ecu-simulator/dtc_utils.py`, `try/except ValueError` in `is_hex_value` combined with runtime binary/hex string parsing (`int(..., 2)`, `int(..., 16)`, `.to_bytes(1, 'big')`) inside `encode_obd_dtcs` and `encode_uds_dtcs` created severe CPU overhead (~4087ms for 800,000 DTC conversions).

**Action:** Replace runtime string parsing and exception handling with pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bitwise OR/shift operations appended directly to bytearrays (~805ms, 5x speedup).
