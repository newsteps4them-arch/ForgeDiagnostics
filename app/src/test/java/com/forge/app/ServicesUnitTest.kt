package com.forge.app

import com.forge.app.data.DtcErrorCodeEntity
import com.forge.app.data.ObdSensorPidData
import com.forge.app.data.ObdTelemetryRecordEntity
import com.forge.app.data.ObdTelemetrySnapshot
import com.forge.app.data.VehicleEntity
import com.forge.app.services.*
import org.junit.Assert.*
import org.junit.Test

class ServicesUnitTest {

    @Test
    fun testObdTelemetryRecordEntityAndSnapshot() {
        val record = ObdTelemetryRecordEntity(
            vin = "1G1YC2D70H5100001",
            engineRpm = 2450,
            vehicleSpeedMph = 68,
            coolantTempCelsius = 92,
            intakeAirTempCelsius = 28,
            throttlePositionPercent = 22.5f,
            batteryVoltage = 14.1f,
            boostPressurePsi = 12.4f,
            dtcCodesFormatted = "P0300,P0171",
            connectionType = "USB_OTG",
            connectionStatusText = "Connected OTG"
        )

        assertEquals("1G1YC2D70H5100001", record.vin)
        assertEquals(2450, record.engineRpm)
        assertEquals(68, record.vehicleSpeedMph)
        assertEquals(92, record.coolantTempCelsius)
        assertEquals("P0300,P0171", record.dtcCodesFormatted)

        val snapshot = ObdTelemetrySnapshot(
            vin = record.vin,
            rpm = record.engineRpm,
            speedMph = record.vehicleSpeedMph,
            coolantTempC = record.coolantTempCelsius,
            activeDtcs = listOf("P0300", "P0171")
        )

        assertEquals(2, snapshot.activeDtcs.size)
        assertEquals("P0300", snapshot.activeDtcs[0])

        val pidData = ObdSensorPidData(
            pidHex = "010C",
            parameterName = "Engine RPM",
            currentValue = "2450",
            unit = "RPM",
            status = "NORMAL"
        )
        assertEquals("010C", pidData.pidHex)
        assertEquals("NORMAL", pidData.status)
    }

    @Test
    fun testDtcErrorCodeEntity() {
        val dtcEntity = DtcErrorCodeEntity(
            vin = "1G1YC2D70H5100001",
            dtcCode = "P0300",
            systemCategory = "Powertrain",
            description = "Random/Multiple Cylinder Misfire Detected",
            status = "Stored",
            severity = "High",
            freezeFrameRpm = 2500,
            freezeFrameCoolantTemp = 95
        )

        assertEquals("P0300", dtcEntity.dtcCode)
        assertEquals("Powertrain", dtcEntity.systemCategory)
        assertEquals("High", dtcEntity.severity)
        assertEquals(2500, dtcEntity.freezeFrameRpm)
    }

    @Test
    fun testAgentTypesAndInitialState() {

        val frontendAgent = ForgeAgentType.FrontendUiAgent
        val hwAgent = ForgeAgentType.ClientHardwareAgent
        val telemetryAgent = ForgeAgentType.MiddlewareTelemetryAgent
        val syncAgent = ForgeAgentType.BackendSyncAgent
        val aiAgent = ForgeAgentType.ServerAiAgent

        assertEquals("agent_frontend_ui", frontendAgent.id)
        assertEquals("agent_client_hw", hwAgent.id)
        assertEquals("agent_middleware_telemetry", telemetryAgent.id)
        assertEquals("agent_backend_sync", syncAgent.id)
        assertEquals("agent_server_ai", aiAgent.id)

        val initialState = AgentOrchestratorState()
        assertEquals(5, initialState.activeAgents.size)
        assertEquals("No Active Vehicle Selected", initialState.globalContextVehicle)
        assertEquals(0, initialState.globalContextDtcCount)
        assertTrue(initialState.systemLog.isNotEmpty())
    }

    @Test
    fun testUsbConnectionStatusEnum() {
        val statuses = UsbConnectionStatus.values()
        assertTrue(statuses.contains(UsbConnectionStatus.DISCONNECTED))
        assertTrue(statuses.contains(UsbConnectionStatus.CONNECTED))
        assertTrue(statuses.contains(UsbConnectionStatus.SCANNING))
        assertTrue(statuses.contains(UsbConnectionStatus.CONNECTING))
        assertTrue(statuses.contains(UsbConnectionStatus.ERROR))
    }

    @Test
    fun testObdPidCalculationLogics() {
        // RPM: ((A * 256) + B) / 4
        val rawA = 0x0D
        val rawB = 0x80
        val rpm = ((rawA * 256) + rawB) / 4
        assertEquals(864, rpm)

        // Speed: A in km/h -> converted to mph
        val speedKmh = 100
        val speedMph = (speedKmh * 0.621371).toInt()
        assertEquals(62, speedMph)

        // Coolant Temp: A - 40 (°C)
        val tempRaw = 130
        val tempCelsius = tempRaw - 40
        assertEquals(90, tempCelsius)
    }

    @Test
    fun testSyncStatusModel() {
        val syncStatus = SyncStatus(
            isConnectedToFirestore = true,
            dbName = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9",
            lastSyncTime = 1700000000000L,
            syncedItemsCount = 42,
            statusText = "Real-time Firestore Sync Active"
        )
        assertTrue(syncStatus.isConnectedToFirestore)
        assertEquals(42, syncStatus.syncedItemsCount)
        assertEquals("ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9", syncStatus.dbName)
    }
}
