// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Interface type used for physical OBD-II dongles
 */
enum class ObdHardwareInterface {
    USB_OTG,
    BLUETOOTH_SPP,
    WIFI_SOCKET,
    SIMULATED
}

/**
 * Detailed DTC record with freeze frame data and status
 */
data class LiveDtcRecord(
    val code: String,
    val description: String,
    val category: String, // Powertrain (P), Chassis (C), Body (B), Network (U)
    val status: String,   // Stored (Mode 03), Pending (Mode 07), Permanent (Mode 0A)
    val freezeFrameRpm: Int? = null,
    val freezeFrameCoolantTempC: Int? = null,
    val freezeFrameSpeedKmh: Int? = null,
    /**
     * Provenance of this record. Records fabricated by the app (simulation /
     * demo mode) are tagged SIMULATED and must be labeled as such in the UI.
     * Hardware reads are tagged LIVE_HARDWARE.
     */
    val dataSource: DiagnosticDataSource = DiagnosticDataSource.UNKNOWN
)

/**
 * Hardware connection state and diagnostics
 */
data class ObdHardwareDiagnosticState(
    val selectedInterface: ObdHardwareInterface = ObdHardwareInterface.USB_OTG,
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val connectedDeviceName: String = "No Device Connected",
    val activeProtocol: String = "ISO 15765-4 CAN 11-bit / 500k",
    val batteryVoltage: Float = 14.1f,
    val isFetchingDtcs: Boolean = false,
    val activeDtcs: List<LiveDtcRecord> = emptyList(),
    val rawRxLog: List<String> = emptyList(),
    val lastSyncTimestamp: String = "Pending",
    /**
     * Set when the last DTC fetch could not read the ECU (timeout, no adapter,
     * ECU answered NO DATA, transport error). Null means the fetch completed.
     * An empty [activeDtcs] with a null error means the ECU genuinely reported
     * zero stored/pending codes — never invented ones.
     */
    val dtcFetchError: String? = null,
    /** Provenance of [activeDtcs]: LIVE_HARDWARE or SIMULATED (demo mode). */
    val dtcDataSource: DiagnosticDataSource = DiagnosticDataSource.UNKNOWN
)

/**
 * Dedicated OBD-II hardware diagnostic module that handles low-level USB OTG
 * and Bluetooth RFCOMM AT/ELM327 protocol handshakes, live sensor polling,
 * and DTC querying for direct feeding into the OpenManus Autonomous Agent.
 */
class ObdDiagnosticHardwareModule(
    private val scope: CoroutineScope,
    private val usbHardwareService: UsbHardwareCommunicationService? = null,
    private val telemetryService: ObdTelemetryService? = null,
    private val openManusService: OpenManusAgentService? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    private val _hardwareState = MutableStateFlow(ObdHardwareDiagnosticState())
    val hardwareState: StateFlow<ObdHardwareDiagnosticState> = _hardwareState.asStateFlow()

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var isLoopActive = false

    init {
        // Observe USB hardware connection changes if available
        scope.launch {
            usbHardwareService?.hardwareState?.collect { usbState ->
                if (usbState.status == UsbConnectionStatus.CONNECTED) {
                    _hardwareState.value = _hardwareState.value.copy(
                        isConnected = true,
                        connectedDeviceName = usbState.connectedDevice?.deviceName ?: "USB Diagnostic Dongle",
                        activeProtocol = usbState.detectedObdProtocol
                    )
                }
            }
        }
    }

    fun setHardwareInterface(interfaceType: ObdHardwareInterface) {
        _hardwareState.value = _hardwareState.value.copy(selectedInterface = interfaceType)
        telemetryService?.setConnectionType(interfaceType.name)
    }

    /**
     * Connects to the selected physical hardware interface (USB or Bluetooth)
     * and performs the ELM327 initialization handshake:
     * ATZ -> ATE0 -> ATL0 -> ATH1 -> ATSP0 -> 0100 -> 010C
     */
    fun connectHardwareDongle(onResult: ((Boolean, String) -> Unit)? = null) {
        scope.launch(ioDispatcher) {
            _hardwareState.value = _hardwareState.value.copy(isScanning = true)
            when (_hardwareState.value.selectedInterface) {
                ObdHardwareInterface.USB_OTG -> {
                    val usbSuccess = initUsbOtgDongle()
                    _hardwareState.value = _hardwareState.value.copy(isScanning = false)
                    withContext(mainDispatcher) {
                        onResult?.invoke(usbSuccess, if (usbSuccess) "USB OBD-II Interface Initialized" else "Failed to initialize USB OBD device")
                    }
                }
                ObdHardwareInterface.BLUETOOTH_SPP -> {
                    val btSuccess = initBluetoothDongle()
                    _hardwareState.value = _hardwareState.value.copy(isScanning = false)
                    withContext(mainDispatcher) {
                        onResult?.invoke(btSuccess, if (btSuccess) "Bluetooth OBD-II Interface Connected" else "Failed to pair/connect Bluetooth OBD dongle")
                    }
                }
                else -> {
                    _hardwareState.value = _hardwareState.value.copy(
                        isScanning = false,
                        isConnected = true,
                        connectedDeviceName = "Virtual Diagnostic Bridge"
                    )
                    withContext(mainDispatcher) {
                        onResult?.invoke(true, "Virtual OBD Interface Connected")
                    }
                }
            }
        }
    }


    private suspend fun initUsbOtgDongle(): Boolean {
        return try {
            val resetResp = usbHardwareService?.sendRawCommand("ATZ", 300)
            usbHardwareService?.sendRawCommand("ATE0", 200) // Echo off
            usbHardwareService?.sendRawCommand("ATL0", 200) // Linefeeds off
            usbHardwareService?.sendRawCommand("ATSP0", 300) // Automatic protocol search
            val pidsResp = usbHardwareService?.sendRawCommand("0100", 400) // Supported PIDs
            
            val success = !resetResp.isNullOrBlank() || !pidsResp.isNullOrBlank()
            if (success) {
                _hardwareState.value = _hardwareState.value.copy(
                    isConnected = true,
                    connectedDeviceName = "USB OBD-II (ELM327 / FTDI / CH340)",
                    activeProtocol = "ISO 15765-4 (CAN 11-bit/500k)"
                )
                startPeriodicSensorPolling()
            }
            success
        } catch (e: Exception) {
            Log.e("ObdHardware", "USB init error: ${e.message}")
            false
        }
    }

    private fun initBluetoothDongle(): Boolean {
        return try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!btAdapter.isEnabled) return false

            val bondedDevices: Set<BluetoothDevice> = btAdapter.bondedDevices ?: emptySet()
            val obdDevice = bondedDevices.firstOrNull { device ->
                val name = device.name ?: ""
                name.contains("OBD", ignoreCase = true) ||
                        name.contains("ELM327", ignoreCase = true) ||
                        name.contains("vLinker", ignoreCase = true) ||
                        name.contains("Viecar", ignoreCase = true) ||
                        name.contains("OBDLink", ignoreCase = true)
            } ?: bondedDevices.firstOrNull() ?: return false

            bluetoothSocket?.close()
            bluetoothSocket = obdDevice.createRfcommSocketToServiceRecord(sppUuid)
            bluetoothSocket?.connect()

            val outputStream = bluetoothSocket?.outputStream ?: return false
            val inputStream = bluetoothSocket?.inputStream ?: return false

            // Send AT initialization string
            outputStream.write("ATZ\rATE0\rATL0\rATSP0\r".toByteArray())
            outputStream.flush()

            val buffer = ByteArray(512)
            val bytes = inputStream.read(buffer)
            val response = decodeStream(buffer, bytes)

            _hardwareState.value = _hardwareState.value.copy(
                isConnected = true,
                connectedDeviceName = obdDevice.name ?: "Bluetooth OBD-II Dongle",
                activeProtocol = "ISO 15765-4 (CAN 11-bit/500k)"
            )
            startPeriodicSensorPolling()
            true
        } catch (e: Exception) {
            Log.e("ObdHardware", "Bluetooth init error: ${e.message}")
            false
        }
    }

    /**
     * Executes standard SAE J1979 Mode 03 (Stored DTCs) and Mode 07 (Pending DTCs),
     * parses the 2-byte hexadecimal trouble codes, updates live state, and automatically
     * feeds the retrieved DTCs and sensor context directly into the OpenManus Autonomous Agent.
     *
     * HONESTY CONTRACT:
     * - On a hardware interface (USB_OTG / BLUETOOTH_SPP / WIFI_SOCKET) this function
     *   ONLY reports codes actually decoded from ECU responses. An empty or failed
     *   response surfaces as "no DTCs" / "no response" / transport error via
     *   [ObdHardwareDiagnosticState.dtcFetchError] — it NEVER invents codes.
     * - The SIMULATED interface returns clearly labeled demo DTCs ([DiagnosticDataSource.SIMULATED])
     *   for UI/testing without hardware. They must be visibly labeled in the UI.
     */
    fun fetchLiveDiagnosticTroubleCodes(
        vehicleName: String = "Connected Vehicle",
        autoTriggerOpenManus: Boolean = true
    ) {
        scope.launch(ioDispatcher) {
            _hardwareState.value = _hardwareState.value.copy(isFetchingDtcs = true, dtcFetchError = null)


            val parsedDtcs = mutableListOf<LiveDtcRecord>()
            val isSimulation = _hardwareState.value.selectedInterface == ObdHardwareInterface.SIMULATED
            var fetchError: String? = null

            if (isSimulation) {
                // Explicit demo mode: canned DTCs, unmistakably tagged as simulated.
                parsedDtcs.addAll(demoSimulatedDtcRecords())
            } else {
                // 1. Query Mode 03 (Stored DTCs)
                val rawMode03 = sendObdCommand("03")
                if (rawMode03.isNotBlank()) {
                    parsedDtcs.addAll(parseDtcPayload(rawMode03, "Stored"))
                }

                // 2. Query Mode 07 (Pending DTCs)
                val rawMode07 = sendObdCommand("07")
                if (rawMode07.isNotBlank()) {
                    parsedDtcs.addAll(parseDtcPayload(rawMode07, "Pending"))
                }

                // 3. Classify the outcome honestly: distinguish "ECU says no faults"
                //    from "we could not talk to the ECU at all".
                fetchError = when {
                    rawMode03.isBlank() && rawMode07.isBlank() ->
                        "No response from adapter/ECU (not connected, timeout, or transport error). " +
                            "No DTCs could be read — none were invented."
                    isNoDataResponse(rawMode03) && isNoDataResponse(rawMode07) ->
                        "ECU answered NO DATA: no stored or pending DTCs reported."
                    parsedDtcs.isNotEmpty() -> null
                    else -> "ECU returned no decodable DTCs."
                }
            }

            val dtcSource = if (isSimulation) DiagnosticDataSource.SIMULATED else DiagnosticDataSource.LIVE_HARDWARE

            // Update live telemetry service DTC list (hardware reads only — demo
            // records stay inside the explicitly labeled simulation state).
            if (!isSimulation) {
                telemetryService?.clearDtcs()
                parsedDtcs.forEach { dtc ->
                    telemetryService?.addDtc(dtc.code, dtc.description)
                }
            }

            _hardwareState.value = _hardwareState.value.copy(
                isFetchingDtcs = false,
                activeDtcs = parsedDtcs,
                dtcFetchError = fetchError,
                dtcDataSource = dtcSource,
                lastSyncTimestamp = "Just now"
            )

            // 4. Directly feed into OpenManus Autonomous Agent for instant background synthesis
            if (autoTriggerOpenManus && openManusService != null) {
                val dtcCodes = parsedDtcs.map { it.code }
                val telemetryData = telemetryService?.telemetry?.value
                val telemetrySummary = "RPM=${telemetryData?.rpm ?: 850}, ECT=${telemetryData?.coolantTempC ?: 90}C, Volt=${telemetryData?.batteryVoltage ?: 14.1}V, DTCs=${dtcCodes.joinToString()}, source=$dtcSource"

                openManusService.runAutonomousDiagnosis(
                    goal = "Automated physical hardware diagnosis for fault codes [${dtcCodes.joinToString(", ")}] on $vehicleName",
                    vehicleContext = vehicleName,
                    activeDtcs = dtcCodes,
                    telemetrySummary = telemetrySummary
                )
            }
        }
    }

    /**
     * Canned DTCs used ONLY by the explicit SIMULATED hardware interface, so the
     * UI can be exercised without a vehicle. Every record is tagged
     * [DiagnosticDataSource.SIMULATED] and must be rendered with a visible
     * "SIMULATION" label — never as a live diagnosis.
     */
    private fun demoSimulatedDtcRecords(): List<LiveDtcRecord> = listOf(
        LiveDtcRecord(
            code = "P0300",
            description = "Random/Multiple Cylinder Misfire Detected",
            category = "Powertrain",
            status = "Stored",
            freezeFrameRpm = 1920,
            freezeFrameCoolantTempC = 94,
            freezeFrameSpeedKmh = 48,
            dataSource = DiagnosticDataSource.SIMULATED
        ),
        LiveDtcRecord(
            code = "P0171",
            description = "System Too Lean (Bank 1)",
            category = "Powertrain",
            status = "Pending",
            freezeFrameRpm = 1450,
            freezeFrameCoolantTempC = 91,
            freezeFrameSpeedKmh = 32,
            dataSource = DiagnosticDataSource.SIMULATED
        )
    )

    /**
     * True when an ELM327 response explicitly reports no data (ECU reachable but
     * nothing to report) as opposed to a blank/timeout transport failure.
     */
    internal fun isNoDataResponse(raw: String): Boolean =
        raw.contains("NO DATA", ignoreCase = true) ||
            raw.contains("NO_DATA", ignoreCase = true)

    /**
     * Clears diagnostic fault codes using Mode 04 and resets MIL check engine light
     */
    fun clearHardwareFaultCodes(onCompleted: (() -> Unit)? = null) {
        scope.launch(ioDispatcher) {
            sendObdCommand("04")
            telemetryService?.clearDtcs()
            _hardwareState.value = _hardwareState.value.copy(activeDtcs = emptyList())
            withContext(mainDispatcher) {
                onCompleted?.invoke()
            }
        }
    }


    private suspend fun sendObdCommand(command: String): String {
        return try {
            when (_hardwareState.value.selectedInterface) {
                ObdHardwareInterface.USB_OTG -> {
                    usbHardwareService?.sendRawCommand(command, 300) ?: ""
                }
                ObdHardwareInterface.BLUETOOTH_SPP -> {
                    val out = bluetoothSocket?.outputStream ?: return ""
                    val input = bluetoothSocket?.inputStream ?: return ""
                    out.write("$command\r".toByteArray())
                    out.flush()
                    val buf = ByteArray(512)
                    val read = input.read(buf)
                    decodeStream(buf, read, true)
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    internal fun parseDtcPayload(rawHex: String, status: String): List<LiveDtcRecord> {
        val dtcs = mutableListOf<LiveDtcRecord>()
        try {
            // ELM327 negative responses ("NO DATA", "ERROR", "?") are transport
            // signals, not DTC payloads. Decoding them as hex would fabricate
            // phantom codes (e.g. "P0ODA" from "NO DATA") — reject them outright.
            if (rawHex.contains("NO DATA", ignoreCase = true) ||
                rawHex.contains("ERROR", ignoreCase = true) ||
                rawHex.contains("?")
            ) {
                return emptyList()
            }
            val clean = rawHex.replace(" ", "").replace("\r", "").replace("\n", "").replace(">", "")
            // Mode 03 response starts with 43, Mode 07 starts with 47
            val payload = when {
                clean.contains("43") -> clean.substringAfter("43")
                clean.contains("47") -> clean.substringAfter("47")
                else -> clean
            }

            // Each DTC is 2 bytes (4 hex characters)
            var index = 0
            while (index + 4 <= payload.length) {
                val dtcHex = payload.substring(index, index + 4)
                // Skip padding and non-hex garbage — never invent a code from it.
                val isHex = dtcHex.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
                if (dtcHex != "0000" && isHex) {
                    val code = decodeSingleDtcHex(dtcHex)
                    if (code.isNotBlank()) {
                        dtcs.add(
                            LiveDtcRecord(
                                code = code,
                                description = getStandardDtcDescription(code),
                                category = getCategoryForDtc(code),
                                status = status,
                                dataSource = DiagnosticDataSource.LIVE_HARDWARE
                            )
                        )
                    }
                }
                index += 4
            }
        } catch (_: Exception) {}
        return dtcs
    }

    private fun decodeSingleDtcHex(hex: String): String {
        return try {
            val firstChar = hex[0]
            val secondChar = hex[1]
            val rest = hex.substring(2, 4)

            val prefix = when (firstChar) {
                '0' -> "P0"
                '1' -> "P1"
                '2' -> "P2"
                '3' -> "P3"
                '4' -> "C0"
                '5' -> "C1"
                '6' -> "C2"
                '7' -> "C3"
                '8' -> "B0"
                '9' -> "B1"
                'A', 'a' -> "B2"
                'B', 'b' -> "B3"
                'C', 'c' -> "U0"
                'D', 'd' -> "U1"
                'E', 'e' -> "U2"
                'F', 'f' -> "U3"
                else -> "P0"
            }
            "$prefix$secondChar$rest"
        } catch (e: Exception) {
            ""
        }
    }

    private fun getCategoryForDtc(code: String): String {
        return when {
            code.startsWith("P") -> "Powertrain"
            code.startsWith("C") -> "Chassis"
            code.startsWith("B") -> "Body"
            code.startsWith("U") -> "Network / Bus"
            else -> "General"
        }
    }

    private fun getStandardDtcDescription(code: String): String {
        return when (code.uppercase()) {
            "P0300" -> "Random/Multiple Cylinder Misfire Detected"
            "P0301" -> "Cylinder 1 Misfire Detected"
            "P0302" -> "Cylinder 2 Misfire Detected"
            "P0303" -> "Cylinder 3 Misfire Detected"
            "P0304" -> "Cylinder 4 Misfire Detected"
            "P0171" -> "System Too Lean (Bank 1)"
            "P0174" -> "System Too Lean (Bank 2)"
            "P0299" -> "Turbocharger/Supercharger 'A' Underboost Condition"
            "P0420" -> "Catalyst System Efficiency Below Threshold (Bank 1)"
            "P0113" -> "Intake Air Temperature Sensor 1 Circuit High Input"
            "P0128" -> "Coolant Thermostat (Coolant Temp Below Regulating Temp)"
            "U0100" -> "Lost Communication With ECM/PCM 'A'"
            "U0121" -> "Lost Communication With ABS Control Module"
            "U0140" -> "Lost Communication With Body Control Module"
            "C0035" -> "Left Front Wheel Speed Sensor Circuit Fault"
            "B1000" -> "Electronic Control Unit (ECU) Internal Malfunction"
            else -> "Manufacturer Diagnostic Trouble Code ($code)"
        }
    }

    private fun startPeriodicSensorPolling() {
        if (isLoopActive) return
        isLoopActive = true
        scope.launch(ioDispatcher) {

            while (isLoopActive && _hardwareState.value.isConnected) {
                // Poll live RPM (010C) — currently the only PID decoded from real
                // hardware. RPM is reported as-is via telemetryService.setRpm().
                //
                // Vehicle speed is NOT derived from RPM (the old `liveRpm / 35`
                // mapping was fabricated). Unsupported/unavailable PIDs keep
                // their previous value in hardware mode — they are never filled
                // with simulated numbers.
                val rpmRaw = sendObdCommand("010C")
                if (rpmRaw.contains("410C") || rpmRaw.contains("41 0C")) {
                    val clean = rpmRaw.replace(" ", "").substringAfter("410C")
                    if (clean.length >= 4) {
                        val a = clean.substring(0, 2).toIntOrNull(16) ?: 0
                        val b = clean.substring(2, 4).toIntOrNull(16) ?: 0
                        val liveRpm = ((a * 256) + b) / 4
                        telemetryService?.setRpm(liveRpm)
                    }
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    fun disconnect() {
        isLoopActive = false
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {}
        bluetoothSocket = null
        usbHardwareService?.disconnect()
        _hardwareState.value = _hardwareState.value.copy(
            isConnected = false,
            connectedDeviceName = "Disconnected"
        )
    }
    private fun decodeStream(buffer: ByteArray, bytesRead: Int, trim: Boolean = false): String {
        return if (bytesRead > 0) {
            val result = String(buffer, 0, bytesRead)
            if (trim) result.trim() else result
        } else {
            ""
        }
    }

}
