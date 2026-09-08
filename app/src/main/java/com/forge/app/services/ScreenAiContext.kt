// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

/**
 * ScreenAiContext provides screen-aware contextual intelligence and specialization
 * for every route across Team Forge.
 */
data class ScreenContextInfo(
    val route: String,
    val title: String,
    val specialistName: String,
    val defaultSkill: AssistantSkill,
    val screenDescription: String,
    val suggestedPrompts: List<String>,
    val contextTag: String
)

object ScreenAiContextRegistry {
    private val contexts = mapOf(
        "dashboard" to ScreenContextInfo(
            route = "dashboard",
            title = "Workshop Dashboard",
            specialistName = "Master Diagnostic & Operations Lead",
            defaultSkill = AssistantSkill.REPORTING,
            screenDescription = "Overview of active projects, fleet connection status, and urgent DTC diagnostic alerts.",
            suggestedPrompts = listOf(
                "Analyze active P0300 misfire diagnostic status",
                "Summarize workshop work order pipeline",
                "Generate executive shop diagnostic report",
                "Check fleet vehicle connection health"
            ),
            contextTag = "DASHBOARD"
        ),
        "live_data" to ScreenContextInfo(
            route = "live_data",
            title = "Live OBD-II Telemetry",
            specialistName = "High-Frequency PID & Fuel Trim Math Specialist",
            defaultSkill = AssistantSkill.COMPUTE,
            screenDescription = "Real-time Mode 01 PID stream monitoring (RPM, Coolant Temp, Fuel Trims, Boost Pressure).",
            suggestedPrompts = listOf(
                "Calculate STFT (+14.2%) + LTFT (+6.8%) total fuel trim",
                "Decode Mode 01 PID 0C RPM raw hex stream",
                "Analyze lean condition threshold under 2,450 RPM",
                "Evaluate coolant temp 92°C vs thermostat setpoint"
            ),
            contextTag = "LIVE_TELEMETRY"
        ),
        "dyno" to ScreenContextInfo(
            route = "dyno",
            title = "Dyno Telematics",
            specialistName = "Horsepower, Torque & Power Curve Math Specialist",
            defaultSkill = AssistantSkill.COMPUTE,
            screenDescription = "Calculated wheel horsepower, torque curves, and boost pressure telemetry.",
            suggestedPrompts = listOf(
                "Calculate peak wheel HP and torque from 0-60 pull",
                "Analyze power dropoff above 5,200 RPM",
                "Compare boost pressure (1.2 bar) vs torque delivery",
                "Estimate parasitic drivetrain loss (AWD Quattro)"
            ),
            contextTag = "DYNO"
        ),
        "topology" to ScreenContextInfo(
            route = "topology",
            title = "CAN Bus Topology",
            specialistName = "CAN Network & Gateway Diagnostics Specialist",
            defaultSkill = AssistantSkill.DEEP_THINKING,
            screenDescription = "Inter-module CAN bus network topology, gateway latency, and U-code communications.",
            suggestedPrompts = listOf(
                "Analyze CAN-H / CAN-L 120Ω termination resistance",
                "Isolate intermittent U0100 ECM lost communication",
                "Trace gateway signal drop on Infotainment bus segment",
                "Verify 500kbps baud rate integrity across modules"
            ),
            contextTag = "CAN_TOPOLOGY"
        ),
        "guided_diag" to ScreenContextInfo(
            route = "guided_diag",
            title = "Guided Diagnostics",
            specialistName = "Deep Fault Tree & Root-Cause Reasoning Specialist",
            defaultSkill = AssistantSkill.DEEP_THINKING,
            screenDescription = "Multi-step root-cause isolation and diagnostic fault tree workflows.",
            suggestedPrompts = listOf(
                "Execute P0300 random misfire fault tree",
                "Isolate High-Pressure Fuel Pump vs Injector failure",
                "Step-by-step P0171 System Too Lean troubleshooting",
                "Verify spark plug gap (0.7mm) and coil dwell time"
            ),
            contextTag = "GUIDED_DIAGNOSTICS"
        ),
        "actuators" to ScreenContextInfo(
            route = "actuators",
            title = "Actuator Bi-Directional Tests",
            specialistName = "Bi-Directional ECU Control & Solenoid Testing Specialist",
            defaultSkill = AssistantSkill.GENERAL,
            screenDescription = "Bi-directional component actuation (EVAP purge, fuel pump relay, radiator fan).",
            suggestedPrompts = listOf(
                "Safety checklist for HPFP fuel rail purge test",
                "Execute EVAP purge solenoid 50% PWM duty cycle",
                "Test high-speed cooling fan relay trigger",
                "Verify variable intake flap actuator position"
            ),
            contextTag = "ACTUATORS"
        ),
        "oscilloscope" to ScreenContextInfo(
            route = "oscilloscope",
            title = "Digital Lab Oscilloscope",
            specialistName = "Waveform Analysis & Signal Math Specialist",
            defaultSkill = AssistantSkill.COMPUTE,
            screenDescription = "Multi-channel signal waveforms, frequency/period calculations, and injector PWM.",
            suggestedPrompts = listOf(
                "Measure Cylinder 1 Injector PWM pulse width (ms)",
                "Verify Crankshaft 60-2 sensor missing tooth sync",
                "Analyze secondary ignition coil spark burn duration",
                "Calculate peak-to-peak Vpp ripple on 12V alternator"
            ),
            contextTag = "OSCILLOSCOPE"
        ),
        "terminal" to ScreenContextInfo(
            route = "terminal",
            title = "Raw OBD-II Terminal",
            specialistName = "ELM327 AT Command & Hex Protocol Specialist",
            defaultSkill = AssistantSkill.COMPUTE,
            screenDescription = "Low-level USB OTG / Bluetooth serial terminal for direct AT & OBD hexadecimal queries.",
            suggestedPrompts = listOf(
                "Explain ELM327 initialization (ATZ, ATSP0, ATH1)",
                "Decode response: 41 0C 1F 40 (Engine RPM)",
                "Send Mode 09 PID 02 VIN query protocol command",
                "Send Mode 03 read stored diagnostic trouble codes"
            ),
            contextTag = "TERMINAL"
        ),
        "garage" to ScreenContextInfo(
            route = "garage",
            title = "Vehicle Garage & Fleet",
            specialistName = "Fleet Fleet Profile & OEM Protocol Specialist",
            defaultSkill = AssistantSkill.WEB_DOCS,
            screenDescription = "Vehicle garage management, VIN specifications, and ECU communication protocols.",
            suggestedPrompts = listOf(
                "Decode VIN WAUZZZF58MA019284 factory specifications",
                "Search OEM technical service bulletins for 3.0T V6",
                "Verify ISO 15765-4 CAN 11-bit/500k compatibility",
                "Compare fuel trim baselines across fleet vehicles"
            ),
            contextTag = "GARAGE"
        ),
        "inventory" to ScreenContextInfo(
            route = "inventory",
            title = "Parts Catalog & Inventory",
            specialistName = "Supply Chain & OEM Parts Sourcing Specialist",
            defaultSkill = AssistantSkill.MAPS_SEARCH,
            screenDescription = "Workshop parts stock, automated reorder triggers, and supplier search.",
            suggestedPrompts = listOf(
                "Find nearby OEM Euro parts suppliers for Bosch Injectors",
                "Calculate total workshop inventory valuation",
                "Cross-reference Bosch #0261500059 part specs",
                "Generate automated parts reorder list for low stock"
            ),
            contextTag = "INVENTORY"
        ),
        "estimator" to ScreenContextInfo(
            route = "estimator",
            title = "Repair Cost Estimator",
            specialistName = "Labor Guide & Itemized Repair Quote Specialist",
            defaultSkill = AssistantSkill.REPORTING,
            screenDescription = "Labor time guides, parts cost calculation, and itemized customer quotes.",
            suggestedPrompts = listOf(
                "Estimate labor hours for Audi EA839 spark plug swap",
                "Generate itemized repair estimate for P0300 repair",
                "Calculate parts + labor quote with 8.5% shop tax",
                "Draft customer layman explanation of estimated repairs"
            ),
            contextTag = "ESTIMATOR"
        ),
        "dvi" to ScreenContextInfo(
            route = "dvi",
            title = "Digital Vehicle Inspection (DVI)",
            specialistName = "DVI Inspection & Multimodal Photo Specialist",
            defaultSkill = AssistantSkill.REPORTING,
            screenDescription = "Digital inspection checklists, component photo assessments, and customer DVI reports.",
            suggestedPrompts = listOf(
                "Generate formal DVI customer inspection summary",
                "Classify brake pad wear severity (3mm remaining)",
                "Draft layman explanation for high-pressure fuel leak",
                "Annotate spark plug photo with electrode gap spec"
            ),
            contextTag = "DVI"
        ),
        "wiring" to ScreenContextInfo(
            route = "wiring",
            title = "Wiring Diagrams & Schematics",
            specialistName = "Circuit Blueprint & Harness Pinout Specialist",
            defaultSkill = AssistantSkill.DIAGRAM_GEN,
            screenDescription = "Interactive automotive circuit schematics, wire color codes, and ECU pinouts.",
            suggestedPrompts = listOf(
                "Fetch ECM J220 Pin 14 Injector 1 wiring schematic",
                "Generate 4K circuit diagram for high-pressure fuel harness",
                "Trace 12V switched power rail for ignition coils",
                "Verify ground splice wire gauge and terminal torque"
            ),
            contextTag = "WIRING"
        ),
        "time_clock" to ScreenContextInfo(
            route = "time_clock",
            title = "Technician Time Clock",
            specialistName = "Labor Efficiency & Work Order Time Specialist",
            defaultSkill = AssistantSkill.WORKFLOW,
            screenDescription = "Technician shift punch-in, billable labor tracking, and work order efficiency.",
            suggestedPrompts = listOf(
                "Summarize billable technician hours for this shift",
                "Calculate labor efficiency multiplier for WO-2026-0811",
                "Audit work order punch-in timestamps",
                "Delegate pending repair tasks to Master Tech"
            ),
            contextTag = "TIME_CLOCK"
        ),
        "crm" to ScreenContextInfo(
            route = "crm",
            title = "CRM & Customer Relations",
            specialistName = "Customer Communication & Service History Specialist",
            defaultSkill = AssistantSkill.REPORTING,
            screenDescription = "Customer records, repair service history, and automated status updates.",
            suggestedPrompts = listOf(
                "Draft customer update SMS for Audi S5 misfire status",
                "Review repair history and lifetime value for Apex Motors",
                "Schedule 60k mile maintenance reminder",
                "Create follow-up survey for completed work order"
            ),
            contextTag = "CRM"
        ),
        "orchestrator" to ScreenContextInfo(
            route = "orchestrator",
            title = "Agent Orchestrator",
            specialistName = "Autonomous Multi-Agent Orchestration Specialist",
            defaultSkill = AssistantSkill.WORKFLOW,
            screenDescription = "Autonomous multi-agent execution pipeline (Diagnostics, Parts Sourcing, Work Orders).",
            suggestedPrompts = listOf(
                "Run full autonomous diagnostic pipeline for P0300",
                "Coordinate parts reorder with auto DVI report generation",
                "Audit multi-agent execution log telemetry",
                "Assign automated tasks across virtual technician agents"
            ),
            contextTag = "ORCHESTRATOR"
        ),
        "settings" to ScreenContextInfo(
            route = "settings",
            title = "Settings & Cloud Sync",
            specialistName = "Security, Hardware & Firestore Sync Specialist",
            defaultSkill = AssistantSkill.GENERAL,
            screenDescription = "Bluetooth/USB serial settings, Firestore cloud database sync, and Gemini API keys.",
            suggestedPrompts = listOf(
                "Verify Firestore database real-time sync connectivity",
                "Check Gemini API key configuration and quotas",
                "Configure USB OTG serial baud rate (115200)",
                "Audit local Room database persistence records"
            ),
            contextTag = "SETTINGS"
        )
    )

    fun getContextForRoute(route: String): ScreenContextInfo {
        return contexts[route] ?: contexts["dashboard"]!!
    }
}
