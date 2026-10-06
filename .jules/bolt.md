# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - ECU Simulator DTC Encoding Bottleneck

**Learning:** String concatenation, binary base conversion (`int(..., 2)` and `int(..., 16)`), and `try/except` exception handling inside DTC encoders (`tools/ecu-simulator/dtc_utils.py`) create high CPU overhead (~6.84s per 100,000 encode operations).

**Action:** Use pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bitwise operations (`|`, `<<`) to perform single-pass DTC encoding and validation without string formatting or exception handling overhead (~1.85s, 73% execution time reduction).
