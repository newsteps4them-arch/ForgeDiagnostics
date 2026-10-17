# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-09 - Python ECU Simulator DTC Encoding Optimization

**Learning:** String concatenation (`"00" + "01"`), binary string parsing (`int(..., 2)`), and `try/except` exception handlers inside `is_hex_value` during high-frequency ECU DTC encoding (`tools/ecu-simulator/dtc_utils.py`) create significant CPU overhead (~0.905s per 450,000 DTC checks).

**Action:** Use pre-computed dictionary bitwise lookups (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) to directly construct byte outputs with bitwise OR operations without string parsing or try/except overhead, yielding a ~69% speedup (~0.279s execution time).
