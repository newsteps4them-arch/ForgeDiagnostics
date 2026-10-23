package com.forge.app.hardware

import kotlinx.coroutines.flow.Flow

/**
 * Hardware Abstraction Layer for connecting to a vehicle (or simulator).
 */
interface VehicleConnection {

    /**
     * Attempts to connect to the vehicle.
     * @return true if successful, false otherwise.
     */
    suspend fun connect(): Boolean

    /**
     * Disconnects from the vehicle.
     */
    suspend fun disconnect()

    /**
     * Returns a flow indicating if the connection is currently active.
     */
    val isConnected: Flow<Boolean>

    /**
     * Sends a raw command to the vehicle/ECU and returns the raw response.
     * @param command The string command (e.g., "01 0C\r")
     * @return The raw response string from the ECU, or null if it failed.
     */
    suspend fun sendCommand(command: String): String?
}
