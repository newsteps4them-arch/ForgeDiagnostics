# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-31 - Zero-Allocation Byte Buffer & Numeric PID Dispatch

**Learning:** Allocating a new `number[]` array per decoded OBD-II frame in `parseHexBytes` and converting PID numbers into string hex keys for string-based switch dispatch causes substantial garbage collection pressure and CPU overhead (~519ms per 4,000,000 decodings).

**Action:** Use a module-scoped reusable `Uint8Array` buffer (`parseHexBytesBuffer`) with a decoded byte count returned by `parseHexBytesToBuffer`, and switch directly on numeric byte PID values (e.g. `0x0C` instead of `'0C'`). This yields a ~2.2x speedup (54.5% reduction in execution time) with zero heap allocations during high-frequency telemetry loops.
