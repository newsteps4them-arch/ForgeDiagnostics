# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - Python ECU Simulator DTC Encoding Overhead

**Learning:** String concatenation, binary string parsing (`int(..., 2)`), hex string building, and `try/except` exception handling inside high-frequency ECU simulator DTC encoders (`tools/ecu-simulator/dtc_utils.py`) introduce ~65% latency overhead (~418ms per 70,000 DTC encodings).

**Action:** Use pre-computed bitwise lookup tables (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bit-shifting (`|` and `<<`) with `bytearray.extend()` to bypass string parsing and exception handling overhead (~2.9x speedup).
