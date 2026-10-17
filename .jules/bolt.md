# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-10 - ECU Simulator DTC Bitwise Encoding & Validation Bottleneck

**Learning:** Parsing DTCs in Python ECU simulators using string slicing (`dtc[0]`, `dtc[1]`), binary string conversions (`int(binary_str, 2)`), and exception handling (`try/except int(value, 16)`) introduces significant CPU overhead (~4.25s per 100 iterations of 8,000 DTC batch calls).

**Action:** Use pre-computed integer bitwise lookup maps (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and bitwise operators (`|`, `<<`) in `tools/ecu-simulator/dtc_utils.py` to bypass string parsing and exception overhead, cutting execution time to ~1.15s (72.8% speedup).
