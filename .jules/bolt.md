# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-09 - ECU Simulator DTC Encoding & Validation Bottleneck

**Learning:** String concatenation (`int(..., 2)`, `int(..., 16)`), string `len` & slicing, and `try/except ValueError` exception handling during diagnostic trouble code (DTC) serialization in `tools/ecu-simulator/dtc_utils.py` created substantial overhead (~296ms per 100k DTCs).

**Action:** Utilize pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) with direct `bytearray` appending (`.append()`) to achieve 3x faster encoding (~87ms per 100k DTCs) while preserving full backwards compatibility.
