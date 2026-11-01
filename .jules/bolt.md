# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-11-04 - ECU Simulator DTC Encoding and Bitwise Lookup Optimization

**Learning:** In Python ECU diagnostic simulation (`tools/ecu-simulator/dtc_utils.py`), dynamic string concatenations (`+`), base conversions (`int(..., 2)` / `int(..., 16)`), and exception handling blocks (`try/except ValueError`) inside tight encoding loops create significant CPU bottlenecks (~2.61s per 400,000 DTC conversions).

**Action:** Replace dynamic string manipulations and exception catching with pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and bound method caching (`append = dtcs_bytes.append`), resulting in ~5.2x faster encoding throughput (~0.50s per 400,000 DTC conversions).
