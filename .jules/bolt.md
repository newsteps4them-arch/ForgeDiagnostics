# Bolt's Journal - Performance Insights & Learnings

This journal contains critical codebase-specific performance learnings.

## 2026-09-09 - SAE J1979 OBD-II Response Parsing Bottleneck

**Learning:** Regex-based string cleaning (`replace(/[\s\r\n>]/g, '')`), regex format checking (`/^[0-9A-F]+$/`), and repeated string slicing (`slice(i, i+2)`) inside high-frequency diagnostic telemetry decoders (e.g., `src/protocols/j1979_decoder.ts`) introduce severe overhead (~978ms per 50,000 decoding cycles).

**Action:** Parse raw string inputs directly using single-pass `charCodeAt` character code character loops to convert hex nibbles directly to byte buffers and skip framing characters (`\s`, `\r`, `\n`, `\t`, `>`) without intermediate string allocations.

## 2026-09-09 - Jetpack Compose Canvas Path Allocations at 60 FPS

**Learning:** Instantiating `Path()` objects directly inside Jetpack Compose `DrawScope` or `Canvas` loops during continuous 60 FPS animation updates creates thousands of object allocations per second, triggering frequent garbage collection pauses (jank).

**Action:** Pre-allocate `Path` instances outside `Canvas` using `remember { Path() }` and invoke `.reset()` on existing path instances prior to redrawing each frame.
