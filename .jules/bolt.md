# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - ECU Simulator DTC Bitwise Encoding Bottleneck

**Learning:** String parsing (`int(..., 2)` and `int(..., 16)`), string concatenation (`DTC_GROUP.get(dtc[0]) + DTC_TYPE.get(dtc[1]) + "0000"`), and `try/except ValueError` exception handling during diagnostic trouble code (DTC) validation and byte encoding in `tools/ecu-simulator/dtc_utils.py` created significant computational overhead during high-frequency ECU responses (~1.335s for 500k DTC encodings).

**Action:** Replace dynamic string parsing and exception handling with pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, and `HEX_VAL`) to perform direct bitwise OR/shift byte operations (`b1 = g | t | h2`, `b2 = (h3 << 4) | h4`), improving DTC encoding performance by ~3.4x (~0.393s).
