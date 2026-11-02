# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-24 - DTC Encoding and Validation Bitwise Optimization in ECU Simulator

**Learning:** Using dynamic string formatting (`int(..., 2)`, `int(..., 16)`), string concatenation, and `try/except ValueError` in `tools/ecu-simulator/dtc_utils.py` causes significant latency during high-frequency ECU simulation loops (~2059ms per 50,000 encode cycles).

**Action:** Replace dynamic binary/hex string parsing and exception-based validation with pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct byte buffer extension to reduce encoding time by ~60% (~817ms).
