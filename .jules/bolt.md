# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-10 - Jetpack Compose Canvas Path Allocations During Real-Time Telemetry

**Learning:** Allocating new `Path()` instances inside `@Composable` `Canvas` `DrawScope` blocks (such as in `OscilloscopeScreen`, `DynoTelematicsScreen`, and `OpenManusTelemetryDashboard`) creates continuous heap allocation pressure (up to 180 Path objects allocated per second at 60 FPS in animated oscilloscope sweeps), causing premature garbage collection passes and micro-stutters during high-speed sensor feeds.

**Action:** Pre-allocate `Path` objects in the composable body using `remember { Path() }` and reset them via `path.reset()` prior to populating points inside the `Canvas` `DrawScope`.
