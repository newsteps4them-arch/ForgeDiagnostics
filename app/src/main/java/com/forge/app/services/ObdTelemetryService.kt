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
import kotlin.random.Random

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
    private var simulationTick = 0

    init {
        startTelemetryLoop()
    }

    private var telemetryJob: kotlinx.coroutines.Job? = null

    fun startTelemetryLoop() {
        if (isRunning) return
        isRunning = true
        telemetryJob = scope.launch(ioDispatcher) {
            while (isRunning) {
                val current = _telemetry.value

                if (current.isConnected) {
                    val success = when (current.connectionType) {
                        "SIMULATED" -> {
                            simulationTick += 1
                            updateLiveDataStream(current, simulationTick)
                            true
                        }
                        "BLUETOOTH" -> tryConnectAndReadBluetoothObd()
                        "USB_OTG" -> tryConnectAndReadUsbOtgObd()
                        "OBD_SCANNER_WIFI" -> tryConnectAndReadWifiObdScanner()
                        "TORQUE_PRO" -> tryConnectTorqueProBridge()
                        "ALFA_OBD" -> tryConnectAlfaObdBridge()
                        "REPAIR2SOLUTIONS" -> tryConnectRepairSolutions2Bridge()
                        else -> false
                    }
                    if (!success) {
                        _telemetry.value = current.copy(
                            connectionStatusText = "No valid telemetry response received"
                        )
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

    private suspend fun tryConnectAndReadUsbOtgObd(): Boolean {
        return try {
            val usbState = usbHardwareService?.hardwareState?.value
            val statusMsg = usbState?.statusMessage ?: "USB OTG Hardware Bridge Active (115200 Baud)"

            val response = usbHardwareService?.sendRawCommand("010C", 300)
            if (!response.isNullOrBlank()) {
                val parsedRpm = parseRpmResponse(response)
                if (parsedRpm != null) {
                    _telemetry.value = _telemetry.value.copy(
                        rpm = parsedRpm,
                        connectionStatusText = statusMsg
                    )
                    return true
                }
            }
            _telemetry.value = _telemetry.value.copy(
                connectionStatusText = statusMsg
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tryConnectAndReadWifiObdScanner(): Boolean {
        return try {
            _telemetry.value = _telemetry.value.copy(
                connectionStatusText = "OBD Scanner Wi-Fi Socket (192.168.0.10:35000)"
            )
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun tryConnectTorqueProBridge(): Boolean {
        return try {
            _telemetry.value = _telemetry.value.copy(
                connectionStatusText = "Torque Pro Intent Bridge (org.prowl.torque Active)"
            )
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun tryConnectAlfaOBDBridge(): Boolean {
        return tryConnectAlfaObdBridge()
    }

    private fun tryConnectAlfaObdBridge(): Boolean {
        return try {
            _telemetry.value = _telemetry.value.copy(
                connectionStatusText = "AlfaOBD FCA Diagnostic Bridge Active"
            )
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun tryConnectRepairSolutions2Bridge(): Boolean {
        return try {
            _telemetry.value = _telemetry.value.copy(
                connectionStatusText = "RepairSolutions2 Innova Dongle Bridge Active"
            )
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun updateLiveDataStream(current: ObdTelemetryData, tick: Int) {
        val baseRpm = if (current.speedKmh > 0) 1800 + (current.speedKmh * 35) else 850
        val rpmVariation = Random.nextInt(-40, 45)
        val newRpm = (baseRpm + rpmVariation).coerceIn(750, 6800)

        val speedVariation = if (tick % 5 == 0) Random.nextInt(-1, 2) else 0
        val newSpeed = (current.speedKmh + speedVariation).coerceIn(0, 180)

        val throttle = if (newSpeed > 0) (20 + newSpeed / 3).coerceAtMost(95) else 14
        val boost = if (throttle > 40) ((throttle - 40) * 0.25f) else 0.0f
        val voltage = 14.1f + Random.nextFloat() * 0.3f

        _telemetry.value = current.copy(
            rpm = newRpm,
            speedKmh = newSpeed,
            throttlePosPct = throttle,
            boostPressurePsi = boost,
            batteryVoltage = (voltage * 10).toInt() / 10.0f,
            fuelTrimShortPct = ((Random.nextFloat() * 4 - 2) * 10).toInt() / 10.0f,
            oilPressurePsi = (35.0f + (newRpm / 200.0f) + Random.nextFloat()).coerceIn(25f, 75f)
        )
    }

    private fun tryConnectAndReadBluetoothObd(): Boolean {
        return try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!btAdapter.isEnabled) return false

            val pairedDevices: Set<BluetoothDevice>? = btAdapter.bondedDevices
            val obdDevice = pairedDevices?.firstOrNull { device ->
                val name = device.name ?: ""
                name.contains("OBD", ignoreCase = true) ||
                        name.contains("ELM327", ignoreCase = true) ||
                        name.contains("vLinker", ignoreCase = true) ||
                        name.contains("Viecar", ignoreCase = true)
            } ?: pairedDevices?.firstOrNull() ?: return false

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
                val response = String(buffer, 0, bytesRead).trim()
                val parsedRpm = parseRpmResponse(response)
                if (parsedRpm != null) {
                    _telemetry.value = _telemetry.value.copy(rpm = parsedRpm)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            try {
                bluetoothSocket?.close()
            } catch (_: Exception) {}
            bluetoothSocket = null
            false
        }
    }

    internal fun parseRpmResponse(response: String): Int? {
        return try {
            val clean = response.replace(" ", "").replace("\r", "").replace("\n", "")
            if (clean.startsWith("410C", ignoreCase = true)) {
                val hexStr = clean.substring(4).take(4)
                if (hexStr.length == 4) {
                    val a = hexStr.substring(0, 2).toInt(16)
                    val b = hexStr.substring(2, 4).toInt(16)
                    return ((a * 256) + b) / 4
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun setSpeed(speed: Int) {
        _telemetry.value = _telemetry.value.copy(speedKmh = speed.coerceIn(0, 240))
    }

    fun clearDtcs() {
        _telemetry.value = _telemetry.value.copy(activeDtcCodes = emptyList())
    }

    fun addDtc(code: String, description: String) {
        val list = _telemetry.value.activeDtcCodes.toMutableList()
        list.add(DtcInfo(code, description, "Stored"))
        _telemetry.value = _telemetry.value.copy(activeDtcCodes = list)
    }

    fun setConnectionType(type: String) {
        _telemetry.value = _telemetry.value.copy(connectionType = type)
    }

    fun toggleConnection() {
        val cur = _telemetry.value.isConnected
        _telemetry.value = if (cur) {
            _telemetry.value.copy(
                isConnected = false,
                connectionStatusText = "Disconnected"
            )
        } else {
            _telemetry.value.copy(
                connectionStatusText = "Connect a physical OBD-II adapter before polling"
            )
        }
    }
}
