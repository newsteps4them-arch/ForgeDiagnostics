# Bolt's Journal - Performance Insights & Learnings

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck
**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decodes).
**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-10 - Jetpack Compose Gauge Canvas Trigonometric Recalculation
**Learning:** Performing trigonometric calculations (`cos`/`sin`) and double/float arithmetic inside Jetpack Compose Canvas `DrawScope` loops for telemetry gauges (e.g., `RadialGauge` in `ObdGaugeCluster.kt`) causes unnecessary CPU churn and heap allocations on every 60/120 FPS animation frame.
**Action:** Pre-calculate trigonometric unit vectors (`cos`, `sin`) and tick mark metadata outside the Canvas `DrawScope` block using Compose's `remember(keys...)` so math is executed only when tick configuration bounds change.

## 2026-09-09 - TypedArray Subarray vs V8 Fast Elements Array Allocation
**Learning:** Replacing small JS `number[]` array allocations (`[a, b, c, d]`) in hot parsing loops with `Uint8Array.subarray()` on a static buffer actually degraded V8 performance by ~50% (325ms vs 487ms per 1M iterations) due to TypedArray view object creation overhead.
**Action:** Prefer standard V8 Fast Elements packed arrays for small fixed-size byte buffers (< 16 elements) in JavaScript/TypeScript unless working with large binary buffers (> 1KB).

## 2026-09-09 - Jetpack Compose Canvas Path Allocations at 60 FPS
**Learning:** Instantiating `Path()` objects directly inside Jetpack Compose `DrawScope` or `Canvas` loops during continuous 60 FPS animation updates creates thousands of object allocations per second, triggering frequent garbage collection pauses (jank).
**Action:** Pre-allocate `Path` instances outside `Canvas` using `remember { Path() }` and invoke `.reset()` on existing path instances prior to redrawing each frame.
**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-09 - Jetpack Compose Canvas Animation Allocation Bottleneck

**Learning:** Allocating `Path()` instances (`val path = Path()`) or recalculating trigonometric values (`cos`/`sin`) inside Jetpack Compose `Canvas` `DrawScope` during high-frequency 60/120 FPS animations (e.g. gauge needle sweeps and oscilloscope waveforms) introduces unnecessary heap allocation pressure and UI thread jank.

**Action:** Pre-allocate `Path` objects outside `Canvas` using `remember { Path() }` and invoke `.reset()` per frame. Cache static tick mark geometry and trig calculations using `remember(keys...)` so `DrawScope` only executes pure drawing calls.
