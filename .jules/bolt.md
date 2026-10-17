# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-05 - SAE J1979 Decoder Zero-Allocation Byte Buffer Optimization

**Learning:** Allocating intermediate JS arrays (`number[]`) and growing them dynamically via `bytes.push()` inside high-frequency telemetry parsers creates significant Garbage Collection (GC) overhead and heap allocation pressure during continuous CAN telemetry streams (~787ms vs ~544ms per 2.5 million decoder calls).

**Action:** Reuse a module-scoped, pre-allocated `Uint8Array(256)` static buffer (`SHARED_BYTE_BUF`) to store parsed bytes directly and return integer byte counts, eliminating array allocation entirely for synchronous parsing functions.
