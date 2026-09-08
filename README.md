# Team Forge: Engineering Suite

[![Direct APK Download](https://img.shields.io/badge/Direct_Download-Android_.APK-00E676?style=for-the-badge&logo=android&logoColor=black)](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases/latest/download/Forge-debug-1.0.0.141.apk)
[![Releases Page](https://img.shields.io/github/v/release/newsteps4them-arch/ForgeDiagnostics?style=for-the-badge&color=2196F3)](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases)

Native Android application built with Kotlin and Jetpack Compose.

## Latest APK

[Download Forge-debug-1.0.0.141.apk](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases/latest/download/Forge-debug-1.0.0.141.apk)

Open the downloaded APK on Android and approve installation when prompted. For all releases and Google Play AAB bundles, visit the [GitHub Releases Page](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases).

## Features

- Diagnostics Suite: Real-time OBD-II telemetry stream, CAN/LIN ECU Network Topology viewer, Digital Oscilloscope, and ELM327 Raw Command Terminal.
- Multimodal Gemini AI Assistant: Context-aware automotive diagnostic assistant for DTC trouble codes, OEM service procedures, and component vision identification.
- Workshop Management: Vehicle Garage profiles, Parts Catalog and Inventory, Repair Estimator, Digital Vehicle Inspection checklists, Wiring Diagrams, and Technician Time Clock.
- Data Persistence: Offline-first Room Database architecture for vehicles, tasks, parts, and diagnostic fault logs.

## Tech Stack

- UI Framework: Jetpack Compose (Material 3)
- Language: Kotlin 2.0
- Database: Room Database with KSP
- Networking and AI: Retrofit, OkHttp, Kotlinx Serialization, Gemini REST API
- Build System: Gradle (Kotlin DSL)

## Integration Verification

Run the non-interactive verifier from PowerShell:

```powershell
npm run verify:integrations
npm run verify:gemini       # requires GEMINI_API_KEY in the environment
npm run verify:serial       # only when a serial adapter is attached to Windows
```

The verifier checks NHTSA, VPIC, GitHub, the latest APK asset, bot prompt files, Gemini configuration, ADB access, and optional serial hardware. It never prints credentials. Physical Android USB access and the Innova RepairSolutions2 connection must be tested on the device; the app does not claim third-party scanner data is available unless it receives a real response.
