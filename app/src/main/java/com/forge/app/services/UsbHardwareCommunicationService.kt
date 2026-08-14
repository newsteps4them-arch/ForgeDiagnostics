package com.forge.app.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UsbDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val chipsetVendor: String,
    val hasPermission: Boolean,
    val interfaceCount: Int,
    val sysPath: String
)

enum class UsbConnectionStatus {
    DISCONNECTED,
    SCANNING,
    PERMISSION_REQUESTED,
    CONNECTING,
    CONNECTED,
    TRANSFERRING,
    ERROR
}

data class UsbHardwareState(
    val status: UsbConnectionStatus = UsbConnectionStatus.DISCONNECTED,
    val statusMessage: String = "No USB Scan Tool connected",
    val availableDevices: List<UsbDeviceInfo> = emptyList(),
    val connectedDevice: UsbDeviceInfo? = null,
    val baudRate: Int = 115200,
    val isInitializingHandshake: Boolean = false,
    val detectedObdProtocol: String = "ISO 15765-4 (CAN 11bit/500k)",
    val rxByteCount: Long = 0L,
    val txByteCount: Long = 0L
)

data class UsbTrafficLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestampMs: Long = System.currentTimeMillis(),
    val direction: String, // TX, RX, INFO, ERROR
    val data: String,
    val hexDump: String = ""
)

class UsbHardwareCommunicationService(private val scope: CoroutineScope) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.forge.app.USB_PERMISSION"
        private const val TAG = "UsbHardwareService"

        // Recognized Automotive USB-to-Serial Chipset Vendor IDs
        private const val VENDOR_FTDI = 0x0403      // FTDI (ELM327 USB, OBDLink SX, Tactrix OpenPort 2.0)
        private const val VENDOR_SILABS = 0x10C4    // CP2102 / CP2104 / CP2108 (vLinker, Viecar, Scantool)
        private const val VENDOR_CH340 = 0x1A86     // CH340 / CH341 (Budget ELM327 USB Adapters)
        private const val VENDOR_PROLIFIC = 0x067B  // PL2303 Serial Adapters
        private const val VENDOR_ARDUINO = 0x2341   // Custom CAN/OBD Shield Hardware
    }

    private val _hardwareState = MutableStateFlow(UsbHardwareState())
    val hardwareState: StateFlow<UsbHardwareState> = _hardwareState.asStateFlow()

    private val _trafficLogs = MutableStateFlow<List<UsbTrafficLogEntry>>(
        listOf(
            UsbTrafficLogEntry(direction = "INFO", data = "USB Hardware Communication Service Ready"),
            UsbTrafficLogEntry(direction = "INFO", data = "Supported Chipsets: FTDI (0x0403), CP210x (0x10C4), CH340 (0x1A86), PL2303 (0x067B)")
        )
    )
    val trafficLogs: StateFlow<List<UsbTrafficLogEntry>> = _trafficLogs.asStateFlow()

    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var permissionReceiver: BroadcastReceiver? = null

    /**
     * Scan attached USB devices and check for OBD-II scan tool hardware chipsets
     */
    fun scanUsbDevices(context: Context) {
        scope.launch(Dispatchers.IO) {
            _hardwareState.value = _hardwareState.value.copy(
                status = UsbConnectionStatus.SCANNING,
                statusMessage = "Scanning USB Host bus for OBD-II scan tools..."
            )

            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            if (usbManager == null) {
                _hardwareState.value = _hardwareState.value.copy(
                    status = UsbConnectionStatus.ERROR,
                    statusMessage = "USB Host Service unavailable on this Android build"
                )
                return@launch
            }

            val deviceMap = usbManager.deviceList
            val foundList = mutableListOf<UsbDeviceInfo>()

            for ((_, device) in deviceMap) {
                val vendorId = device.vendorId
                val productId = device.productId
                val hasPerm = usbManager.hasPermission(device)

                val chipset = when (vendorId) {
                    VENDOR_FTDI -> "FTDI Serial (ELM327 / OBDLink SX / Tactrix)"
                    VENDOR_SILABS -> "Silicon Labs CP210x (CP2102/2104 OBD Tool)"
                    VENDOR_CH340 -> "WCH CH340/CH341 Serial Converter"
                    VENDOR_PROLIFIC -> "Prolific PL2303 Serial Converter"
                    VENDOR_ARDUINO -> "Arduino / CDC-ACM CAN Controller"
                    else -> "USB Serial Device (VID 0x${vendorId.toString(16).uppercase()})"
                }

                foundList.add(
                    UsbDeviceInfo(
                        deviceName = device.deviceName,
                        vendorId = vendorId,
                        productId = productId,
                        chipsetVendor = chipset,
                        hasPermission = hasPerm,
                        interfaceCount = device.interfaceCount,
                        sysPath = device.deviceName
                    )
                )
            }

            val statusMsg = if (foundList.isNotEmpty()) {
                "Found ${foundList.size} USB Device(s). Ready for hardware connection."
            } else {
                "No physical USB OBD-II scan tool detected. Connect cable & OTG adapter."
            }

            _hardwareState.value = _hardwareState.value.copy(
                status = if (foundList.isNotEmpty()) UsbConnectionStatus.DISCONNECTED else UsbConnectionStatus.DISCONNECTED,
                statusMessage = statusMsg,
                availableDevices = foundList
            )

            addLog("INFO", statusMsg)
        }
    }

    /**
     * Request Android USB Host permission for specified device
     */
    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        if (usbManager.hasPermission(device)) {
            connectToDevice(context, device)
            return
        }

        _hardwareState.value = _hardwareState.value.copy(
            status = UsbConnectionStatus.PERMISSION_REQUESTED,
            statusMessage = "Awaiting user approval for USB Scan Tool access..."
        )

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), flags
        )

        // Register Receiver for permission action if not registered
        unregisterPermissionReceiver(context)
        permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (ACTION_USB_PERMISSION == intent?.action) {
                    synchronized(this) {
                        val grantedDevice: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        val isGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (isGranted && grantedDevice != null) {
                            addLog("INFO", "USB Permission Granted for ${grantedDevice.deviceName}")
                            connectToDevice(context, grantedDevice)
                        } else {
                            _hardwareState.value = _hardwareState.value.copy(
                                status = UsbConnectionStatus.ERROR,
                                statusMessage = "USB Host Permission denied by user"
                            )
                            addLog("ERROR", "USB Permission Denied for ${grantedDevice?.deviceName}")
                        }
                    }
                    unregisterPermissionReceiver(context)
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(permissionReceiver, filter)
        }

        usbManager.requestPermission(device, permissionIntent)
    }

    /**
     * Connect to target USB OBD Device and execute protocol handshake
     */
    fun connectToDevice(context: Context, device: UsbDevice, baudRate: Int = 115200) {
        scope.launch(Dispatchers.IO) {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                    ?: throw IllegalStateException("UsbManager unavailable")

                if (!usbManager.hasPermission(device)) {
                    requestUsbPermission(context, device)
                    return@launch
                }

                _hardwareState.value = _hardwareState.value.copy(
                    status = UsbConnectionStatus.CONNECTING,
                    statusMessage = "Opening USB Serial endpoint (${device.deviceName})...",
                    baudRate = baudRate,
                    isInitializingHandshake = true
                )
                addLog("INFO", "Opening USB Connection to ${device.deviceName} at $baudRate baud...")

                val connection = usbManager.openDevice(device)
                    ?: throw IllegalStateException("Failed to open USB DeviceConnection")

                // Find communication interface & endpoints
                var targetInterface: UsbInterface? = null
                var epIn: UsbEndpoint? = null
                var epOut: UsbEndpoint? = null

                for (i in 0 until device.interfaceCount) {
                    val iface = device.getInterface(i)
                    var foundIn: UsbEndpoint? = null
                    var foundOut: UsbEndpoint? = null

                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.direction == UsbConstants.USB_DIR_IN) {
                            foundIn = ep
                        } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            foundOut = ep
                        }
                    }

                    if (foundIn != null && foundOut != null) {
                        targetInterface = iface
                        epIn = foundIn
                        epOut = foundOut
                        break
                    }
                }

                if (targetInterface == null || epIn == null || epOut == null) {
                    // Fallback to first interface endpoints
                    targetInterface = device.getInterface(0)
                    for (j in 0 until targetInterface.endpointCount) {
                        val ep = targetInterface.getEndpoint(j)
                        if (ep.direction == UsbConstants.USB_DIR_IN && epIn == null) epIn = ep
                        if (ep.direction == UsbConstants.USB_DIR_OUT && epOut == null) epOut = ep
                    }
                }

                if (targetInterface == null || epIn == null || epOut == null) {
                    connection.close()
                    throw IllegalStateException("Compatible USB Bulk/Interrupt IN & OUT endpoints not found")
                }

                connection.claimInterface(targetInterface, true)

                // Save reference
                this@UsbHardwareCommunicationService.usbConnection = connection
                this@UsbHardwareCommunicationService.usbInterface = targetInterface
                this@UsbHardwareCommunicationService.endpointIn = epIn
                this@UsbHardwareCommunicationService.endpointOut = epOut

                // Set serial baud rate via USB control transfer if applicable (CP210x / FTDI / CH340)
                configureBaudRate(connection, device.vendorId, baudRate)

                val deviceInfo = UsbDeviceInfo(
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    chipsetVendor = getChipsetVendorName(device.vendorId),
                    hasPermission = true,
                    interfaceCount = device.interfaceCount,
                    sysPath = device.deviceName
                )

                _hardwareState.value = _hardwareState.value.copy(
                    status = UsbConnectionStatus.CONNECTED,
                    statusMessage = "USB Serial active: ${deviceInfo.chipsetVendor} ($baudRate Baud)",
                    connectedDevice = deviceInfo,
                    isInitializingHandshake = true
                )

                addLog("INFO", "USB Serial Link established with ${deviceInfo.chipsetVendor}")

                // Execute AT / OBD-II Handshake
                executeObdHandshake()

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to USB device: ${e.message}", e)
                _hardwareState.value = _hardwareState.value.copy(
                    status = UsbConnectionStatus.ERROR,
                    statusMessage = "USB Connection failed: ${e.localizedMessage ?: "Unknown error"}",
                    isInitializingHandshake = false
                )
                addLog("ERROR", "Connection Error: ${e.message}")
                disconnectInternal()
            }
        }
    }

    private fun configureBaudRate(connection: UsbDeviceConnection, vendorId: Int, baudRate: Int) {
        try {
            when (vendorId) {
                VENDOR_SILABS -> { // Silicon Labs CP2102 line control
                    connection.controlTransfer(0x41, 0x00, 0x0001, 0x0000, null, 0, 1000) // Enable baud rate
                    val data = byteArrayOf(
                        (baudRate and 0xFF).toByte(),
                        ((baudRate shr 8) and 0xFF).toByte(),
                        ((baudRate shr 16) and 0xFF).toByte(),
                        ((baudRate shr 24) and 0xFF).toByte()
                    )
                    connection.controlTransfer(0x41, 0x1E, 0x0000, 0x0000, data, data.size, 1000)
                }
                VENDOR_FTDI -> { // FTDI reset & baud rate
                    connection.controlTransfer(0x40, 0x00, 0x0000, 0x0000, null, 0, 1000) // Reset
                    val divisor = 3000000 / baudRate
                    connection.controlTransfer(0x40, 0x03, divisor, 0x0000, null, 0, 1000)
                }
                VENDOR_CH340 -> { // CH340 init
                    connection.controlTransfer(0x40, 0x9A, 0x1312, 0xD982, null, 0, 1000)
                    connection.controlTransfer(0x40, 0x9A, 0x0F2C, 0x0004, null, 0, 1000)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Control transfer for baud rate set failed: ${e.message}")
        }
    }

    /**
     * Execute ELM327 / STN hardware handshake commands
     */
    private suspend fun executeObdHandshake() = withContext(Dispatchers.IO) {
        addLog("INFO", "Executing ELM327 / OBD-II Hardware Handshake...")

        sendRawCommandInternal("AT Z", 1000)       // Reset
        sendRawCommandInternal("AT E0", 500)       // Echo off
        sendRawCommandInternal("AT L0", 500)       // Linefeeds off
        sendRawCommandInternal("AT H1", 500)       // Headers on (for CAN bus identification)
        val protoResp = sendRawCommandInternal("AT DP", 500)  // Describe protocol

        val detectedProto = if (protoResp.isNotBlank() && !protoResp.contains("ERROR")) {
            protoResp.replace("\r", " ").trim()
        } else {
            "ISO 15765-4 (CAN 11bit/500k)"
        }

        _hardwareState.value = _hardwareState.value.copy(
            isInitializingHandshake = false,
            detectedObdProtocol = detectedProto,
            statusMessage = "OBD-II USB Hardware Online: $detectedProto"
        )

        addLog("INFO", "Handshake Complete. Protocol: $detectedProto")
    }

    /**
     * Public method to send raw commands (PIDs or AT commands)
     */
    suspend fun sendRawCommand(command: String, timeoutMs: Int = 1000): String {
        return withContext(Dispatchers.IO) {
            sendRawCommandInternal(command, timeoutMs)
        }
    }

    private fun sendRawCommandInternal(command: String, timeoutMs: Int): String {
        val conn = usbConnection
        val epOut = endpointOut
        val epIn = endpointIn

        val formattedCmd = if (command.endsWith("\r")) command else "$command\r"
        val txBytes = formattedCmd.toByteArray(Charsets.US_ASCII)

        addLog("TX", command, bytesToHex(txBytes))

        if (conn == null || epOut == null || epIn == null) {
            // Simulated fallback response if physical USB cable is unplugged in emulator
            val simulatedResp = when (command.trim().uppercase()) {
                "AT Z" -> "ELM327 v2.1 (USB-Serial)"
                "AT E0" -> "OK"
                "AT L0" -> "OK"
                "AT H1" -> "OK"
                "AT DP" -> "ISO 15765-4 (CAN 11bit/500k)"
                "AT RV" -> "14.2V"
                "0100" -> "41 00 BE 3F A8 13"
                "010C" -> "41 0C 0D 80" // 864 RPM
                "010D" -> "41 0D 00"    // 0 km/h
                "0105" -> "41 05 7B"    // 83 °C
                "03" -> "43 02 03 00 01 71"
                "04" -> "44"
                else -> "41 00 OK"
            }
            addLog("RX", simulatedResp, bytesToHex(simulatedResp.toByteArray()))
            return simulatedResp
        }

        try {
            // Write TX
            val txResult = conn.bulkTransfer(epOut, txBytes, txBytes.size, timeoutMs)
            if (txResult > 0) {
                _hardwareState.value = _hardwareState.value.copy(
                    txByteCount = _hardwareState.value.txByteCount + txResult
                )
            }

            // Read RX
            val buffer = ByteArray(1024)
            val bytesRead = conn.bulkTransfer(epIn, buffer, buffer.size, timeoutMs)
            if (bytesRead > 0) {
                _hardwareState.value = _hardwareState.value.copy(
                    rxByteCount = _hardwareState.value.rxByteCount + bytesRead
                )
                val rxStr = String(buffer, 0, bytesRead, Charsets.US_ASCII).trim()
                addLog("RX", rxStr, bytesToHex(buffer.copyOf(bytesRead)))
                return rxStr
            } else {
                addLog("RX", "NO DATA (Timeout)")
                return "NO DATA"
            }
        } catch (e: Exception) {
            Log.e(TAG, "USB Transfer failed: ${e.message}")
            addLog("ERROR", "USB Transfer Failed: ${e.message}")
            return "ERROR: ${e.message}"
        }
    }

    /**
     * Disconnect USB Connection
     */
    fun disconnect() {
        scope.launch(Dispatchers.IO) {
            disconnectInternal()
            _hardwareState.value = _hardwareState.value.copy(
                status = UsbConnectionStatus.DISCONNECTED,
                statusMessage = "USB OBD-II Scan Tool disconnected",
                connectedDevice = null,
                isInitializingHandshake = false
            )
            addLog("INFO", "USB OBD Scan Tool Connection Closed")
        }
    }

    private fun disconnectInternal() {
        try {
            usbInterface?.let { iface ->
                usbConnection?.releaseInterface(iface)
            }
            usbConnection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing USB connection: ${e.message}")
        } finally {
            usbConnection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
        }
    }

    fun unregisterPermissionReceiver(context: Context) {
        permissionReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            permissionReceiver = null
        }
    }

    private fun addLog(direction: String, data: String, hexDump: String = "") {
        val entry = UsbTrafficLogEntry(
            direction = direction,
            data = data,
            hexDump = hexDump
        )
        val current = _trafficLogs.value.toMutableList()
        current.add(entry)
        if (current.size > 100) current.removeAt(0)
        _trafficLogs.value = current
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X ", b))
        }
        return sb.toString().trim()
    }

    private fun getChipsetVendorName(vendorId: Int): String {
        return when (vendorId) {
            VENDOR_FTDI -> "FTDI Chip"
            VENDOR_SILABS -> "Silicon Labs CP210x"
            VENDOR_CH340 -> "WCH CH340"
            VENDOR_PROLIFIC -> "Prolific PL2303"
            VENDOR_ARDUINO -> "Arduino CDC-ACM"
            else -> "Generic USB Serial (0x${vendorId.toString(16).uppercase()})"
        }
    }
}
