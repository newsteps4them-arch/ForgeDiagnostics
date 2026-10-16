# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - Python ECU Simulator Diagnostic Trouble Code (DTC) Encoding Bottleneck

**Learning:** Dynamic string slicing, string-to-int bases conversions (`int(..., 2)`, `int(..., 16)`), binary string formatting, and `try/except ValueError` exception handling during DTC validation/encoding in `tools/ecu-simulator/dtc_utils.py` creates excessive overhead (~2193 ms for 800k DTC encodings).

**Action:** Use pre-computed integer bitmask lookup tables (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bitwise operations (`|`, `<<`) wrapped directly into `bytes([...])` to eliminate string allocations and achieve a ~1.48x speedup (~1481 ms vs ~2193 ms).
