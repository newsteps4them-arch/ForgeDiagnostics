# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-10 - Jetpack Compose Gauge Canvas Trigonometric Recalculation

**Learning:** Performing trigonometric calculations (`cos`/`sin`) and double/float arithmetic inside Jetpack Compose Canvas `DrawScope` loops for telemetry gauges (e.g., `RadialGauge` in `ObdGaugeCluster.kt`) causes unnecessary CPU churn and heap allocations on every 60/120 FPS animation frame.

**Action:** Pre-calculate trigonometric unit vectors (`cos`, `sin`) and tick mark metadata outside the Canvas `DrawScope` block using Compose's `remember(keys...)` so math is executed only when tick configuration bounds change.
