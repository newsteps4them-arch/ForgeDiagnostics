# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-09 - Jetpack Compose Canvas Animation Allocation Bottleneck

**Learning:** Allocating `Path()` instances (`val path = Path()`) or recalculating trigonometric values (`cos`/`sin`) inside Jetpack Compose `Canvas` `DrawScope` during high-frequency 60/120 FPS animations (e.g. gauge needle sweeps and oscilloscope waveforms) introduces unnecessary heap allocation pressure and UI thread jank.

**Action:** Pre-allocate `Path` objects outside `Canvas` using `remember { Path() }` and invoke `.reset()` per frame. Cache static tick mark geometry and trig calculations using `remember(keys...)` so `DrawScope` only executes pure drawing calls.
