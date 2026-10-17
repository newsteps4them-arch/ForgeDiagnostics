# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - DTC Encoding String & Exception Handling Bottleneck

**Learning:** String parsing (`int(DTC_GROUP.get(...) + DTC_TYPE.get(...) + "0000", 2)`), string concatenation (`dtc[3] + dtc[4]`), and `try/except ValueError` exception handling during DTC validation in Python ECU simulator tools (`tools/ecu-simulator/dtc_utils.py`) create significant execution overhead (~540ms for 80,000 DTCs).

**Action:** Pre-compute bitwise integer lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) to perform direct character membership checks and bitwise OR/shift calculations, bypassing string parsing and exception handling to achieve a ~3.6x speedup.
