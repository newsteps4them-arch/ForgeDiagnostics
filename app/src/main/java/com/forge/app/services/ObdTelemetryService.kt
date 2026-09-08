// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class ObdTelemetryData(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempC: Int = 0,
    val intakeAirTempC: Int = 0,
    val throttlePosPct: Int = 0,
    val batteryVoltage: Float = 0.0f,
    val boostPressurePsi: Float = 0.0f,
    val fuelTrimShortPct: Float = 1.2f,
    val fuelTrimLongPct: Float = -0.8f,
    val oilPressurePsi: Float = 0.0f,
    val isConnected: Boolean = false,
    val connectionType: String = "SIMULATED",
    val connectionStatusText: String = "Disconnected",
    val activeDtcCodes: List<DtcInfo> = emptyList()
)

data class DtcInfo(
    val code: String,
    val description: String,
    val status: String
)

class ObdTelemetryService(
    private val scope: CoroutineScope,
    private val usbHardwareService: UsbHardwareCommunicationService? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _telemetry = MutableStateFlow(ObdTelemetryData())
    val telemetry: StateFlow<ObdTelemetryData> = _telemetry.asStateFlow()

    private var isRunning = false
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var telemetryJob: kotlinx.coroutines.Job? = null

    init { startTelemetryLoop() }

    fun startTelemetryLoop() {
        if (isRunning) return
        isRunning = true
        telemetryJob = scope.launch(ioDispatcher) {
            while (isRunning) {
                val current = _telemetry.value
                if (current.isConnected) {
                    val success = when (current.connectionType) {
                        "BLUETOOTH" -> tryConnectAndReadBluetoothObd()
                        "USB_OTG" -> tryConnectAndReadUsbOtgObd()
                        "OBD_SCANNER_WIFI" -> tryConnectAndReadWifiObdScanner()
                        "TORQUE_PRO" -> tryConnectTorqueProBridge()
                        "ALFA_OBD" -> tryConnectAlfaObdBridge()
                        "REPAIR2SOLUTIONS" -> tryConnectRepairSolutions2Bridge()
                        else -> false
                    }
                    if (!success) {
                        _telemetry.value = current.copy(connectionStatusText = "No valid telemetry response received")
                    }
                }
                delay(300)
            }
        }
    }

    fun stopTelemetryLoop() {
        isRunning = false
        telemetryJob?.cancel()
        telemetryJob = null
    }

    private suspend fun tryConnectAndReadUsbOtgObd(): Boolean = try {
        val usbState = usbHardwareService?.hardwareState?.value
        val statusMsg = usbState?.statusMessage ?: "USB OTG hardware unavailable"
        val response = usbHardwareService?.sendRawCommand("010C", 300)
        val parsedRpm = response?.let { parseRpmResponse(it) }
        if (parsedRpm != null) {
            _telemetry.value = _telemetry.value.copy(rpm = parsedRpm, connectionStatusText = statusMsg)
            true
        } else {
            _telemetry.value = _telemetry.value.copy(connectionStatusText = statusMsg)
            false
        }
    } catch (_: Exception) { false }

    private fun tryConnectAndReadWifiObdScanner(): Boolean {
        _telemetry.value = _telemetry.value.copy(connectionStatusText = "OBD Scanner Wi-Fi bridge unavailable")
        return false
    }

    private fun tryConnectTorqueProBridge(): Boolean {
        _telemetry.value = _telemetry.value.copy(connectionStatusText = "Torque Pro bridge unavailable")
        return false
    }

    private fun tryConnectAlfaOBDBridge(): Boolean = tryConnectAlfaObdBridge()

    private fun tryConnectAlfaObdBridge(): Boolean {
        _telemetry.value = _telemetry.value.copy(connectionStatusText = "AlfaOBD bridge unavailable")
        return false
    }

    private fun tryConnectRepairSolutions2Bridge(): Boolean {
        _telemetry.value = _telemetry.value.copy(connectionStatusText = "RepairSolutions2 does not expose a supported telemetry bridge")
        return false
    }

    private fun tryConnectAndReadBluetoothObd(): Boolean {
        return try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!btAdapter.isEnabled) return false
            val pairedDevices: Set<BluetoothDevice>? = btAdapter.bondedDevices
            val obdDevice = pairedDevices?.firstOrNull { device ->
                val name = device.name ?: ""
                name.contains("OBD", ignoreCase = true) || name.contains("ELM327", ignoreCase = true) || name.contains("vLinker", ignoreCase = true) || name.contains("Viecar", ignoreCase = true)
            } ?: return false
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                bluetoothSocket = obdDevice.createRfcommSocketToServiceRecord(sppUuid)
                bluetoothSocket?.connect()
            }
            val inputStream: InputStream = bluetoothSocket?.inputStream ?: return false
            val outputStream: OutputStream = bluetoothSocket?.outputStream ?: return false
            outputStream.write("010C\r".toByteArray())
            outputStream.flush()
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                val parsedRpm = parseRpmResponse(String(buffer, 0, bytesRead).trim())
                if (parsedRpm != null) {
                    _telemetry.value = _telemetry.value.copy(rpm = parsedRpm)
                    return true
                }
            }
            false
        } catch (_: Exception) {
            try { bluetoothSocket?.close() } catch (_: Exception) {}
            bluetoothSocket = null
            false
        }
    }

    internal fun parseRpmResponse(response: String): Int? = try {
        val clean = response.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        if (clean.contains("410C")) {
            val hexStr = clean.substringAfter("410C").take(4)
            if (hexStr.length == 4) {
                val a = hexStr.substring(0, 2).toInt(16)
                val b = hexStr.substring(2, 4).toInt(16)
                return ((a * 256) + b) / 4
            }
        }
        null
    } catch (_: Exception) { null }

    fun setSpeed(speed: Int) { _telemetry.value = _telemetry.value.copy(speedKmh = speed.coerceIn(0, 240)) }
    fun clearDtcs() { _telemetry.value = _telemetry.value.copy(activeDtcCodes = emptyList()) }
    fun addDtc(code: String, description: String) { _telemetry.value = _telemetry.value.copy(activeDtcCodes = _telemetry.value.activeDtcCodes + DtcInfo(code, description, "Stored")) }
    fun setConnectionType(type: String) { _telemetry.value = _telemetry.value.copy(connectionType = type) }

    fun toggleConnection() {
        val cur = _telemetry.value.isConnected
        _telemetry.value = if (cur) _telemetry.value.copy(isConnected = false, connectionStatusText = "Disconnected")
        else _telemetry.value.copy(connectionStatusText = "Connect a physical OBD-II adapter before polling")
    }
}
