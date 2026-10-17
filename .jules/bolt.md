# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-06 - Python ECU Simulator DTC Encoding Bottleneck

**Learning:** String concatenation, runtime `int(..., 2)`/`int(..., 16)` base conversions, and `try/except` exception handling inside high-frequency ECU simulator DTC encoders (`tools/ecu-simulator/dtc_utils.py`) introduce ~2.8s overhead per 10,000 encoded DTCs.

**Action:** Use pre-computed integer bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and zero-allocation direct bit-shifts (`(h2 << 4) | h3`) with `bytearray.append()` to reduce DTC encoding time down to ~0.5s (>80% execution time reduction).
