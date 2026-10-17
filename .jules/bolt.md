# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-10 - ECU Simulator DTC Bitwise Encoding Bottleneck

**Learning:** String parsing (`int("0000" + char, 16)`, `int(str, 2)`), string concatenation, and `try/except ValueError` exception handling during DTC encoding/validation in `tools/ecu-simulator/dtc_utils.py` created significant latency overhead (~425ms per 160,000 DTCs).

**Action:** Bypass string conversions and exception handling by utilizing pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and inlined `bytearray.append()` operations for 5.2x faster encoding performance (~81ms per 160,000 DTCs).
