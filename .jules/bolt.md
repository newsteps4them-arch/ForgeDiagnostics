# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-10 - Zero-Allocation Kotlin OBD-II Telemetry Frame Decoding

**Learning:** Repeated `.replace(" ", "").replace("\r", "").replace("\n", "").substring(...)` calls inside `ObdTelemetryService.kt` `parseRpmResponse` during high-frequency Android telemetry polling generate up to 7 temporary String objects per telemetry tick, incurring an ~86% latency penalty and frequent ART GC pressure.

**Action:** Implement single-pass character iteration in Kotlin directly matching target PID headers (e.g. `410C`) and hex nibbles while skipping framing characters without intermediate String allocations.
