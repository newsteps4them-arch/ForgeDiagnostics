# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-24 - Python ECU Simulator DTC Encoding & Validation Overhead

**Learning:** In high-throughput simulated ECU streams and automated test loops, Python `try/except` blocks (in `is_hex_value`) combined with dynamic binary string conversions (`int(binary_str, 2)` and `.to_bytes()`) cause significant CPU overhead (~745ms per 50,000 DTC encode operations).

**Action:** Replace dynamic binary string parsing and `try/except` validation with pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, and `HEX_VAL`) and direct byte buffer construction (`bytearray.append/extend`), providing a ~3.5x-4x speedup.
