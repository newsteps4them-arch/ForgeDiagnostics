# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-08 - Zero-Allocation Static Uint8Array Buffer for High-Frequency Frame Parsing

**Learning:** Creating temporary `number[]` array instances during OBD-II hex parsing in `j1979_decoder.ts` creates GC pressure in high-frequency telemetry streaming loops (~1008ms per 500,000 decoding cycles).

**Action:** Use a pre-allocated static `Uint8Array` buffer (`PARSE_BUFFER`) and write decoded bytes directly into it while returning the byte length, dropping execution time to ~724ms (~28% speedup with 0 memory allocations).
