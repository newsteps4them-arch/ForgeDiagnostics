# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - ECU Simulator DTC Encoding & Validation Overhead

**Learning:** Repeated string conversions (`int(..., 2)`, `int(..., 16)`, `"0000" + char`), byte array instantiations, and `try/except` exception handling during high-frequency diagnostic DTC validation and encoding in `tools/ecu-simulator/dtc_utils.py` consume significant CPU time (~5.26s per 100,000 iterations).

**Action:** Replace string parsing and exception handling with pre-computed lookup tables (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bitwise operations (`|`, `<<`) to improve execution speed by ~1.8x (~2.88s per 100,000 iterations).
