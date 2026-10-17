# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - ECU DTC Encoding Bitwise Lookup Optimization

**Learning:** String concatenation, base conversions (`int(s, 2)`, `int(s, 16)`), and exception-driven hex validation (`try: int(v, 16)`) in Python DTC encoding (`tools/ecu-simulator/dtc_utils.py`) create significant overhead during ECU simulation response generation (~9.6s per 20,000 dual OBD/UDS encoding iterations).

**Action:** Replace string parsing and exception handling with pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bit shifts (`<< 6`, `<< 4`) with bytearray appends for ~4.9x faster DTC encoding.
