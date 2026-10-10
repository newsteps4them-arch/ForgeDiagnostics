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

## 2026-09-09 - Python ECU Simulator DTC Encoding Optimization

**Learning:** String concatenation (`"00" + "01"`), binary string parsing (`int(..., 2)`), and `try/except` exception handlers inside `is_hex_value` during high-frequency ECU DTC encoding (`tools/ecu-simulator/dtc_utils.py`) create significant CPU overhead (~0.905s per 450,000 DTC checks).

**Action:** Use pre-computed dictionary bitwise lookups (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) to directly construct byte outputs with bitwise OR operations without string parsing or try/except overhead, yielding a ~69% speedup (~0.279s execution time).
## 2026-09-10 - Jetpack Compose Gauge Canvas Trigonometric Recalculation

**Learning:** Performing trigonometric calculations (`cos`/`sin`) and double/float arithmetic inside Jetpack Compose Canvas `DrawScope` loops for telemetry gauges (e.g., `RadialGauge` in `ObdGaugeCluster.kt`) causes unnecessary CPU churn and heap allocations on every 60/120 FPS animation frame.

**Action:** Pre-calculate trigonometric unit vectors (`cos`, `sin`) and tick mark metadata outside the Canvas `DrawScope` block using Compose's `remember(keys...)` so math is executed only when tick configuration bounds change.
## 2026-09-10 - Python ECU Simulator DTC Encoding Bottleneck

**Learning:** String concatenations (`+`), `int(..., 2)` / `int(..., 16)` base conversions, and `try/except ValueError` exception handling during high-frequency DTC encoding in `tools/ecu-simulator/dtc_utils.py` introduce ~4.4x CPU overhead.

**Action:** Use pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and `bytearray.extend()` / `append()` to perform O(1) character validation and encoding without temporary string or byte object allocations.
## 2026-09-10 - ECU Simulator DTC Encoding & Validation Bottleneck

**Learning:** String concatenation, `int(..., 2)` / `int(..., 16)` conversions, and `try/except ValueError` exception handling during high-frequency DTC encoding in `tools/ecu-simulator/dtc_utils.py` introduce significant latency (~427.5ms per 70,000 DTC operations).

**Action:** Replace string parsing and exception handling with precomputed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and bitwise operations (`|`, `<<`), achieving a 3.6x performance boost (~116.9ms).
## 2026-10-08 - Zero-Allocation Static Uint8Array Buffer for High-Frequency Frame Parsing

**Learning:** Creating temporary `number[]` array instances during OBD-II hex parsing in `j1979_decoder.ts` creates GC pressure in high-frequency telemetry streaming loops (~1008ms per 500,000 decoding cycles).

**Action:** Use a pre-allocated static `Uint8Array` buffer (`PARSE_BUFFER`) and write decoded bytes directly into it while returning the byte length, dropping execution time to ~724ms (~28% speedup with 0 memory allocations).
## 2026-09-09 - TypedArray Subarray vs V8 Fast Elements Array Allocation

**Learning:** Replacing small JS `number[]` array allocations (`[a, b, c, d]`) in hot parsing loops with `Uint8Array.subarray()` on a static buffer actually degraded V8 performance by ~50% (325ms vs 487ms per 1M iterations) due to TypedArray view object creation overhead.

**Action:** Prefer standard V8 Fast Elements packed arrays for small fixed-size byte buffers (< 16 elements) in JavaScript/TypeScript unless working with large binary buffers (> 1KB).
## 2026-09-10 - ECU Simulator DTC Bitwise Encoding Bottleneck

**Learning:** String parsing (`int("0000" + char, 16)`, `int(str, 2)`), string concatenation, and `try/except ValueError` exception handling during DTC encoding/validation in `tools/ecu-simulator/dtc_utils.py` created significant latency overhead (~425ms per 160,000 DTCs).

**Action:** Bypass string conversions and exception handling by utilizing pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and inlined `bytearray.append()` operations for 5.2x faster encoding performance (~81ms per 160,000 DTCs).
## 2026-09-09 - Jetpack Compose Canvas Path Allocations at 60 FPS

**Learning:** Instantiating `Path()` objects directly inside Jetpack Compose `DrawScope` or `Canvas` loops during continuous 60 FPS animation updates creates thousands of object allocations per second, triggering frequent garbage collection pauses (jank).

**Action:** Pre-allocate `Path` instances outside `Canvas` using `remember { Path() }` and invoke `.reset()` on existing path instances prior to redrawing each frame.
## 2026-09-10 - Zero-Allocation Kotlin OBD-II Telemetry Frame Decoding

**Learning:** Repeated `.replace(" ", "").replace("\r", "").replace("\n", "").substring(...)` calls inside `ObdTelemetryService.kt` `parseRpmResponse` during high-frequency Android telemetry polling generate up to 7 temporary String objects per telemetry tick, incurring an ~86% latency penalty and frequent ART GC pressure.

**Action:** Implement single-pass character iteration in Kotlin directly matching target PID headers (e.g. `410C`) and hex nibbles while skipping framing characters without intermediate String allocations.
## 2026-09-10 - Jetpack Compose Gauge Canvas Trigonometric Recalculation
**Learning:** Performing trigonometric calculations (`cos`/`sin`) and double/float arithmetic inside Jetpack Compose Canvas `DrawScope` loops for telemetry gauges (e.g., `RadialGauge` in `ObdGaugeCluster.kt`) causes unnecessary CPU churn and heap allocations on every 60/120 FPS animation frame.
**Action:** Pre-calculate trigonometric unit vectors (`cos`, `sin`) and tick mark metadata outside the Canvas `DrawScope` block using Compose's `remember(keys...)` so math is executed only when tick configuration bounds change.
## 2026-09-09 - TypedArray Subarray vs V8 Fast Elements Array Allocation
**Learning:** Replacing small JS `number[]` array allocations (`[a, b, c, d]`) in hot parsing loops with `Uint8Array.subarray()` on a static buffer actually degraded V8 performance by ~50% (325ms vs 487ms per 1M iterations) due to TypedArray view object creation overhead.
**Action:** Prefer standard V8 Fast Elements packed arrays for small fixed-size byte buffers (< 16 elements) in JavaScript/TypeScript unless working with large binary buffers (> 1KB).
## 2026-09-09 - Jetpack Compose Canvas Animation Allocation Bottleneck
**Learning:** Allocating `Path()` instances (`val path = Path()`) or recalculating trigonometric values (`cos`/`sin`) inside Jetpack Compose `Canvas` `DrawScope` during high-frequency 60/120 FPS animations (e.g. gauge needle sweeps and oscilloscope waveforms) introduces unnecessary heap allocation pressure and UI thread jank.
**Action:** Pre-allocate `Path` objects outside `Canvas` using `remember { Path() }` and invoke `.reset()` per frame. Cache static tick mark geometry and trig calculations using `remember(keys...)` so `DrawScope` only executes pure drawing calls.
## 2026-10-06 - ECU Simulator DTC Encoding Bottleneck

**Learning:** String concatenation, binary base conversion (`int(..., 2)` and `int(..., 16)`), and `try/except` exception handling inside DTC encoders (`tools/ecu-simulator/dtc_utils.py`) create high CPU overhead (~6.84s per 100,000 encode operations).

**Action:** Use pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and direct bitwise operations (`|`, `<<`) to perform single-pass DTC encoding and validation without string formatting or exception handling overhead (~1.85s, 73% execution time reduction).
## 2026-09-10 - ECU Simulator DTC Bitwise Encoding & Validation Bottleneck

**Learning:** Parsing DTCs in Python ECU simulators using string slicing (`dtc[0]`, `dtc[1]`), binary string conversions (`int(binary_str, 2)`), and exception handling (`try/except int(value, 16)`) introduces significant CPU overhead (~4.25s per 100 iterations of 8,000 DTC batch calls).

**Action:** Use pre-computed integer bitwise lookup maps (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) and bitwise operators (`|`, `<<`) in `tools/ecu-simulator/dtc_utils.py` to bypass string parsing and exception overhead, cutting execution time to ~1.15s (72.8% speedup).
## 2026-09-09 - Jetpack Compose Canvas Animation Allocation Bottleneck

**Learning:** Allocating `Path()` instances (`val path = Path()`) or recalculating trigonometric values (`cos`/`sin`) inside Jetpack Compose `Canvas` `DrawScope` during high-frequency 60/120 FPS animations (e.g. gauge needle sweeps and oscilloscope waveforms) introduces unnecessary heap allocation pressure and UI thread jank.

**Action:** Pre-allocate `Path` objects outside `Canvas` using `remember { Path() }` and invoke `.reset()` per frame. Cache static tick mark geometry and trig calculations using `remember(keys...)` so `DrawScope` only executes pure drawing calls.
## 2026-09-10 - Zero-Allocation Kotlin OBD-II Telemetry Frame Decoding
**Learning:** Repeated `.replace(" ", "").replace("\r", "").replace("\n", "").substring(...)` calls inside `ObdTelemetryService.kt` `parseRpmResponse` during high-frequency Android telemetry polling generate up to 7 temporary String objects per telemetry tick, incurring an ~86% latency penalty and frequent ART GC pressure.
**Action:** Implement single-pass character iteration in Kotlin directly matching target PID headers (e.g. `410C`) and hex nibbles while skipping framing characters without intermediate String allocations.

## 2026-09-09 - Jetpack Compose Canvas Animation Allocation Bottleneck
**Learning:** Allocating `Path()` instances (`val path = Path()`) or recalculating trigonometric values (`cos`/`sin`) inside Jetpack Compose `Canvas` `DrawScope` during high-frequency 60/120 FPS animations (e.g. gauge needle sweeps and oscilloscope waveforms) introduces unnecessary heap allocation pressure and UI thread jank.
**Action:** Pre-allocate `Path` instances outside `Canvas` using `remember { Path() }` and invoke `.reset()` on existing path instances prior to redrawing each frame.
## 2026-09-09 - ECU Simulator DTC Encoding & Validation Bottleneck

**Learning:** String concatenation (`int(..., 2)`, `int(..., 16)`), string `len` & slicing, and `try/except ValueError` exception handling during diagnostic trouble code (DTC) serialization in `tools/ecu-simulator/dtc_utils.py` created substantial overhead (~296ms per 100k DTCs).

**Action:** Utilize pre-computed bitwise lookup dictionaries (`DTC_GROUP_BITS`, `DTC_TYPE_BITS`, `HEX_VAL`) with direct `bytearray` appending (`.append()`) to achieve 3x faster encoding (~87ms per 100k DTCs) while preserving full backwards compatibility.
