# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-09 - TypedArray Subarray vs V8 Fast Elements Array Allocation

**Learning:** Replacing small JS `number[]` array allocations (`[a, b, c, d]`) in hot parsing loops with `Uint8Array.subarray()` on a static buffer actually degraded V8 performance by ~50% (325ms vs 487ms per 1M iterations) due to TypedArray view object creation overhead.

**Action:** Prefer standard V8 Fast Elements packed arrays for small fixed-size byte buffers (< 16 elements) in JavaScript/TypeScript unless working with large binary buffers (> 1KB).
