# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-11-10 - Diagnostic Trouble Code (DTC) Bitwise Encoding Bottleneck

**Learning:** String concatenation (`DTC_GROUP.get(...) + DTC_TYPE.get(...) + "0000"`), binary string parsing `int(..., 2)`, `int(..., 16)`, and `try/except` ValueError checking in Python DTC encoding (`tools/ecu-simulator/dtc_utils.py`) create substantial interpreter overhead (~1.51s per 100,000 OBD cycles and ~1.88s per 100,000 UDS cycles).

**Action:** Use pre-computed integer bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) to compute encoded DTC bytes directly via bitwise OR (`|`) and shift (`<< 4`), reducing execution time to ~0.45s (~3.3x speedup).
