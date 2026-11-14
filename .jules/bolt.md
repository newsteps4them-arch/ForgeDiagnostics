# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-10-24 - Pre-Allocating Compose Canvas Paths in High-Frequency Animations

**Learning:** Allocating fresh `Path()` instances inside `Canvas` `DrawScope` on every frame of a continuous 60fps animation (such as `OscilloscopeScreen`, which redraws on `phaseOffset` ticks) allocates 3+ complex path objects per frame (~180 allocations/sec), increasing heap churn and triggering frequent Garbage Collection frame drops.

**Action:** Declare Path instances outside `DrawScope` using `remember { Path() }` and call `path.reset()` at the start of each frame before drawing points.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.
