# Automotive Diagnostics Roadmap

## Goal

Turn ForgeDiagnostics into a practical automotive diagnostics toolkit by borrowing only legally safe, open patterns from the automotive diagnostics ecosystem and adapting them into this project.

## What we learned

The most useful free-to-use patterns from the automotive diagnostics repos are not OEM-specific software clones. They are the fundamentals:

- SAE J1979 PID decoding
- Mode 09 VIN and ECU identification
- Supported-PID mask parsing
- DTC parsing and fault classification
- ECU simulation for testing without a real vehicle
- CAN/LIN bus traffic monitoring concepts

These are the building blocks that make a diagnostics app useful without requiring proprietary code.

## Phase 1: Core diagnostics foundations

- Add stable J1979 parsing for common PIDs
- Decode VIN and ECU name responses
- Decode supported-PID masks and show supported feature sets
- Normalize raw OBD payloads into readable values

## Phase 2: Vehicle and fault intelligence

- Map DTC codes to readable descriptions
- Separate stored vs pending vs permanent faults
- Build a simple health summary for the current vehicle
- Show which data points are supported before querying them

## Phase 3: Testing and simulator loop

- Add a local ECU simulation mode for demos and QA
- Mock warm/cold engine conditions and common faults
- Validate the app against realistic hex responses
- Use the simulator to verify UI, parsing, and triage logic without physical hardware

## Phase 4: Workshop UX improvements

- Add a live vehicle summary screen
- Add vehicle identification card and ECU list
- Add DTC explanation and root-cause panel
- Add a guided diagnostics flow for common faults such as misfire, coolant issues, and fuel trim problems

## Phase 5: Real-world hardware integration

- Connect to USB OTG and Bluetooth adapters cleanly
- Add adapter compatibility checks and fallback modes
- Document safe testing flows for real-world devices
- Keep simulator-first development to reduce debug time

## Why this matters

This approach keeps the project legal, practical, and valuable. It turns the app from a generic dashboard into a diagnostic tool that can be tested, shown to users, and expanded without depending on restricted proprietary data.

## Recommended next actions

1. Keep the decoder layer as the universal contract between hardware and UI.
2. Add a simulator mode for testing before real hardware.
3. Expand DTC explanation and health scoring.
4. Add vehicle context and supported PID awareness to the app.
5. Build the app around safe, standards-based OBD patterns rather than OEM-only logic.
