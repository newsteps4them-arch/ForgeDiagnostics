# Team Forge: Engineering Suite

[![Download Android APK](https://img.shields.io/badge/Download-Android%20APK-00E676?style=for-the-badge&logo=android&logoColor=black)](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases/latest)
[![Latest Release](https://img.shields.io/github/v/release/newsteps4them-arch/ForgeDiagnostics?style=for-the-badge&color=2196F3)](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases)

Native Android application built with **Kotlin** and **Jetpack Compose**, featuring a high-end hardware diagnostic tool aesthetic.

---

## 📲 Download & Install APK

You can download the compiled Android APK directly to install on your Android phone, tablet, or diagnostic scan tool:

1. 📥 **[Download Latest APK Releases](https://github.com/newsteps4them-arch/ForgeDiagnostics/releases/latest)**
2. Select `Forge-debug-1.0.0.apk` (or the latest release `.apk`) from the **Assets** section at the bottom of the release page.
3. On your Android device, tap the downloaded `.apk` file to install.
   *(Note: If prompted, enable **"Install from Unknown Sources"** or **"Allow from this source"** in your device Settings).*

---

## Features

- **Diagnostics Suite**: Real-time OBD-II telemetry stream (RPM, Speed, Coolant, Voltage, Fuel Trims, Boost), CAN/LIN ECU Network Topology viewer, Dual-Channel Digital Oscilloscope, and ELM327 Raw Command Terminal.
- **Multimodal Gemini AI Assistant**: Context-aware automotive diagnostic assistant for DTC trouble codes (P0300, P0171, etc.), OEM service procedures, and component vision identification.
- **Workshop Management**: Vehicle Garage profiles, Parts Catalog & Inventory with Room persistence, Repair Estimator quote breakdown, Digital Vehicle Inspection (DVI) checklists, Wiring Diagrams, and Technician Time Clock.
- **Data Persistence**: Offline-first Room Database architecture for vehicles, tasks, parts, and diagnostic fault logs.

## Tech Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Language**: Kotlin 2.0
- **Database**: Room Database with KSP
- **Networking & AI**: Retrofit, OkHttp, Kotlinx Serialization, Gemini REST API
- **Build System**: Gradle (Kotlin DSL)

