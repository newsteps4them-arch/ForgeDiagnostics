package com.forge.app

import com.forge.app.services.DiagnosticReportData
import com.forge.app.services.DiagnosticReportService
import com.forge.app.services.DtcInfo
import com.forge.app.services.ObdTelemetryData
import org.junit.Assert.*
import org.junit.Test

class DiagnosticReportServiceTest {

    @Test
    fun testGenerateFormattedTextReportStructure() {
        val dtcList = listOf(
            DtcInfo("P0300", "Random/Multiple Cylinder Misfire Detected", "Active"),
            DtcInfo("P0171", "System Too Lean (Bank 1)", "Pending"),
            DtcInfo("U0100", "Lost Communication With ECM/PCM 'A'", "Historic")
        )

        val telemetry = ObdTelemetryData(
            rpm = 2250,
            speedKmh = 72,
            coolantTempC = 96,
            batteryVoltage = 14.2f,
            boostPressurePsi = 14.5f,
            fuelTrimShortPct = 8.5f,
            fuelTrimLongPct = 4.2f,
            oilPressurePsi = 48.0f,
            intakeAirTempC = 34,
            activeDtcCodes = dtcList
        )

        val reportData = DiagnosticReportData(
            reportId = "FRG-TEST-9988",
            timestamp = "2026-08-15 10:00:00",
            vehicleName = "2023 Porsche 911 GT3 RS",
            vehicleVin = "WP0AC2A95PS294819",
            protocol = "ISO 15765-4 (CAN 11/500)",
            telemetry = telemetry,
            dtcList = dtcList,
            technicianNotes = "Spark plugs inspected on Bank 1. Coil pack resistance tested within spec.",
            technicianName = "Senior Master Diagnostic Specialist",
            aiGuidanceSummary = "Recommend checking fuel injector pulse width and fuel rail pressure sensor."
        )

        val textReport = DiagnosticReportService.generateFormattedTextReport(reportData)

        // Assert report headers
        assertTrue(textReport.contains("TEAM FORGE MOTORSPORTS & ADVANCED DIAGNOSTICS"))
        assertTrue(textReport.contains("Report ID       : FRG-TEST-9988"))
        assertTrue(textReport.contains("Scan Date & Time: 2026-08-15 10:00:00"))

        // Assert Vehicle Details
        assertTrue(textReport.contains("Vehicle Model   : 2023 Porsche 911 GT3 RS"))
        assertTrue(textReport.contains("VIN Identifier  : WP0AC2A95PS294819"))
        assertTrue(textReport.contains("OBD-II Protocol : ISO 15765-4 (CAN 11/500)"))

        // Assert DTCs
        assertTrue(textReport.contains("2. DIAGNOSTIC TROUBLE CODES (DTCs)"))
        assertTrue(textReport.contains("P0300"))
        assertTrue(textReport.contains("ACTIVE"))
        assertTrue(textReport.contains("P0171"))
        assertTrue(textReport.contains("PENDING"))
        assertTrue(textReport.contains("U0100"))

        // Assert Telemetry Metrics
        assertTrue(textReport.contains("2250 RPM"))
        assertTrue(textReport.contains("72 km/h"))
        assertTrue(textReport.contains("96 °C"))
        assertTrue(textReport.contains("14.2 V"))
        assertTrue(textReport.contains("14.5 PSI"))
        assertTrue(textReport.contains("8.5 %"))
        assertTrue(textReport.contains("4.2 %"))

        // Assert AI & Tech Notes
        assertTrue(textReport.contains("5. AI DIAGNOSTIC INSIGHTS & OEM PROCEDURES"))
        assertTrue(textReport.contains("Recommend checking fuel injector pulse width"))
        assertTrue(textReport.contains("6. TECHNICIAN & WORKSHOP NOTES"))
        assertTrue(textReport.contains("Spark plugs inspected on Bank 1"))
        assertTrue(textReport.contains("Technician      : Senior Master Diagnostic Specialist"))
    }

    @Test
    fun testEmptyDtcReportFormatting() {
        val telemetry = ObdTelemetryData(
            rpm = 800,
            speedKmh = 0,
            coolantTempC = 88,
            batteryVoltage = 13.8f,
            activeDtcCodes = emptyList()
        )

        val reportData = DiagnosticReportData(
            reportId = "FRG-CLEAN-001",
            vehicleName = "Tesla Model 3 Performance",
            vehicleVin = "5YJ3E1EB8NF123456",
            telemetry = telemetry,
            dtcList = emptyList()
        )

        val textReport = DiagnosticReportService.generateFormattedTextReport(reportData)
        assertTrue(textReport.contains("NO DIAGNOSTIC TROUBLE CODES FOUND (SYSTEM OK)"))
    }

    @Test
    fun testReportIdGenerationFormat() {
        val defaultData = DiagnosticReportData()
        assertTrue(defaultData.reportId.startsWith("FORGE-RPT-"))
        assertTrue(defaultData.timestamp.isNotBlank())
        assertEquals("2021 Audi S5 Sportback", defaultData.vehicleName)
        assertEquals("ISO 15765-4 (CAN 11-bit / 500 kbps)", defaultData.protocol)
    }
}
