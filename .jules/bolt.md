# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-05 - ECU Simulator DTC Encoding & Validation Overhead

**Learning:** String concatenation (`dtc[3] + dtc[4]`), `int(..., 2)`/`int(..., 16)` string parsing, `.to_bytes()` object allocations, and `try/except ValueError` exception handling during DTC encoding in `tools/ecu-simulator/dtc_utils.py` created significant CPU overhead (~4.03s per 1M DTC encodings).

**Action:** Use pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and `bytearray.append`/`extend` to eliminate string parsing and object allocations, yielding a ~4.41x speedup (down to ~0.91s per 1M DTCs).
