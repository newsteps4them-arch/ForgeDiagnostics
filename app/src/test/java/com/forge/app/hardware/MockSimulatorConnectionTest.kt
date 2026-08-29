package com.forge.app.hardware

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * A true automated test that stands up a mock socket server representing the ECU Simulator,
 * and tests our SimulatorConnection HAL against it.
 */
class MockSimulatorConnectionTest {

    private lateinit var connection: SimulatorConnection
    private var serverSocket: ServerSocket? = null
    private var isServerRunning = true

    @Before
    fun setup() {
        // Start a mock server on a random port
        serverSocket = ServerSocket(0)
        val port = serverSocket!!.localPort

        connection = SimulatorConnection("127.0.0.1", port)

        thread {
            try {
                while (isServerRunning) {
                    val socket = serverSocket?.accept()
                    socket?.let { handleClient(it) }
                }
            } catch (e: Exception) {
                // Socket closed, thread exits
            }
        }
    }

    private fun handleClient(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val buffer = ByteArray(1024)

        try {
            while (isServerRunning) {
                if (input.available() > 0) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    val command = String(buffer, 0, bytesRead).trim()

                    // Mock OBD-II responses
                    val response = when (command) {
                        "01 0C" -> "41 0C 1A F8\r\r>" // RPM 1726
                        "09 02" -> "49 02 01 00 00 00 00\r\r>" // VIN partial mock
                        else -> "?\r\r>"
                    }
                    output.write(response.toByteArray())
                    output.flush()
                } else {
                    Thread.sleep(10)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    @After
    fun teardown() = runTest {
        connection.disconnect()
        isServerRunning = false
        serverSocket?.close()
    }

    @Test
    fun testSimulatorConnectionAndRpmRequest() = runTest {
        assertFalse(connection.isConnected.first())

        val connectResult = connection.connect()
        assertTrue("Should connect to mock server successfully", connectResult)
        assertTrue(connection.isConnected.first())

        val rpmResponse = connection.sendCommand("01 0C")
        assertNotNull(rpmResponse)
        assertTrue("Response should contain RPM bytes", rpmResponse!!.contains("41 0C"))
        assertTrue("Response should end with prompt", rpmResponse.endsWith(">"))

        connection.disconnect()
        assertFalse(connection.isConnected.first())
    }
}
