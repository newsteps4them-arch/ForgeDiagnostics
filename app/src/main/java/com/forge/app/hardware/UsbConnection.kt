package com.forge.app.hardware

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * A connection that talks directly to USB-OTG ELM327 or similar OBD cables.
 */
class UsbConnection(
    private val usbManager: UsbManager,
    private val device: UsbDevice
) : VehicleConnection {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!usbManager.hasPermission(device)) {
                Log.e("UsbConnection", "No permission to access USB device")
                return@withContext false
            }

            usbInterface = device.getInterface(0)
            connection = usbManager.openDevice(device)

            if (connection == null) {
                Log.e("UsbConnection", "Failed to open USB device")
                return@withContext false
            }

            if (connection!!.claimInterface(usbInterface, true)) {
                // Find endpoints (simplified - normally needs to check direction)
                for (i in 0 until usbInterface!!.endpointCount) {
                    val ep = usbInterface!!.getEndpoint(i)
                    if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) {
                        endpointIn = ep
                    } else if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                        endpointOut = ep
                    }
                }

                if (endpointIn != null && endpointOut != null) {
                    _isConnected.value = true
                    Log.i("UsbConnection", "Successfully connected to USB cable")
                    return@withContext true
                } else {
                    Log.e("UsbConnection", "Could not find IN/OUT endpoints")
                }
            } else {
                Log.e("UsbConnection", "Could not claim USB interface")
            }

            disconnect()
            false
        } catch (e: Exception) {
            Log.e("UsbConnection", "Error connecting to USB: ${e.message}")
            disconnect()
            false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) {
            Log.e("UsbConnection", "Error closing USB connection: ${e.message}")
        } finally {
            connection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
            _isConnected.value = false
        }
    }

    override suspend fun sendCommand(command: String): String? = withContext(Dispatchers.IO) {
        if (!_isConnected.value || connection == null || endpointOut == null || endpointIn == null) {
            return@withContext null
        }

        try {
            val toSend = if (command.endsWith("\r")) command else "$command\r"
            val bytes = toSend.toByteArray()

            // Bulk transfer OUT
            val outResult = connection!!.bulkTransfer(endpointOut, bytes, bytes.size, 1000)
            if (outResult < 0) {
                Log.e("UsbConnection", "Failed to send data via USB")
                return@withContext null
            }

            // Bulk transfer IN (read loop)
            val buffer = ByteArray(1024)
            val responseBuilder = StringBuilder()
            var timeoutCount = 0

            while (timeoutCount < 50) { // Safety timeout
                val bytesRead = connection!!.bulkTransfer(endpointIn, buffer, buffer.size, 100)
                if (bytesRead > 0) {
                    val chunk = String(buffer, 0, bytesRead)
                    responseBuilder.append(chunk)
                    if (chunk.contains(">")) {
                        break
                    }
                } else {
                    timeoutCount++
                    kotlinx.coroutines.delay(50)
                }
            }

            responseBuilder.toString()
        } catch (e: Exception) {
            Log.e("UsbConnection", "Error communicating via USB: ${e.message}")
            null
        }
    }
}
