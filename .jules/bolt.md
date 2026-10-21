# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - Python ECU Simulator DTC Encoding Optimization

**Learning:** Slicing DTC strings, calling `int(x, 16)` inside `try/except` blocks, and repeated `bytearray += ...` allocations during ECU simulation batch DTC encoding introduce significant execution overhead (~0.298s per 100k DTCs).

**Action:** Pre-compute bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and encode bytes directly via bitwise shift/OR operations and `bytearray.append()` to eliminate string conversions and exception handling overhead (~3.5x speedup, ~0.085s per 100k DTCs).
