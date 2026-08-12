# Team Forge: Engineering Suite

Native Android application built with **Kotlin** and **Jetpack Compose**, featuring a high-end hardware diagnostic tool aesthetic.

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
