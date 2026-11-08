# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-10-16 - Jetpack Compose Canvas Path Allocations

**Learning:** Instantiating `Path()` instances inside `Canvas` / `DrawScope` blocks animated at 60-120 fps causes 180-360+ heap allocations per second, producing frequent GC churn, memory pressure, and frame drops in real-time telemetry rendering (such as `OscilloscopeScreen`).

**Action:** Pre-allocate `Path` objects in composables using `remember { Path() }` outside the `Canvas` draw scope and call `.reset()` on each frame before drawing.
