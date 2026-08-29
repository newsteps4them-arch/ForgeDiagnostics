package com.forge.app.hardware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket


/**
 * A connection that talks to the local ECU Simulator over TCP/IP (usually for automated testing).
 */
class SimulatorConnection(
    private val host: String = "10.0.2.2", // Default for Android Emulator to host localhost
    private val port: Int = 35000 // A port we'll use for our proxy to the CAN simulator
) : VehicleConnection {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket(host, port)
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()
            _isConnected.value = true
            println("SimulatorConnection: Connected to ECU Simulator at $host:$port")
            true
        } catch (e: Exception) {
            println("SimulatorConnection: Failed to connect to simulator: ${e.message}")
            _isConnected.value = false
            false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            _isConnected.value = false
        }
    }

    override suspend fun sendCommand(command: String): String? = withContext(Dispatchers.IO) {
        if (!_isConnected.value || socket == null) return@withContext null

        try {
            val toSend = if (command.endsWith("\r")) command else "$command\r"
            outputStream?.write(toSend.toByteArray())
            outputStream?.flush()

            // Basic read loop for OBD-like responses ending with prompt '>'
            val buffer = ByteArray(1024)
            val responseBuilder = StringBuilder()
            var bytesRead: Int

            while (true) {
                // Ensure we don't block forever if simulator dies
                if (inputStream?.available() ?: 0 > 0) {
                    bytesRead = inputStream!!.read(buffer)
                    if (bytesRead == -1) break
                    val chunk = String(buffer, 0, bytesRead)
                    responseBuilder.append(chunk)
                    if (chunk.contains(">")) {
                        break
                    }
                } else {
                    kotlinx.coroutines.delay(50) // Small delay to prevent tight loop
                    // Could add a timeout here
                }
            }
            responseBuilder.toString()
        } catch (e: Exception) {
            println("SimulatorConnection: Error sending command to simulator: ${e.message}")
            disconnect()
            null
        }
    }
}
