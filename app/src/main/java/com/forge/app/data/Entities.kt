// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val vehicleVin: String = "",
    val customerName: String = "",
    val status: String = "Active", // Active, In Progress, Completed, On Hold
    val budget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vin: String,
    val make: String,
    val model: String,
    val year: String,
    val protocol: String = "CAN 11-bit / 500kbps",
    val isConnected: Boolean = false,
    val lastConnectedTime: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 0,
    val title: String,
    val description: String,
    val status: String = "Pending", // Pending, In Progress, Completed
    val priority: String = "Medium", // Low, Medium, High, Urgent
    val category: String = "Diagnostic",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partNumber: String,
    val name: String,
    val category: String,
    val stockQuantity: Int,
    val price: Double,
    val cost: Double = 0.0,
    val reorderPoint: Int = 2,
    val location: String = "Bin A-1",
    val supplier: String = "OEM Parts Co"
)

@Serializable
@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectTitle: String,
    val vehicleVin: String,
    val status: String = "Draft", // Draft, Approved, In Progress, Completed
    val totalCost: Double = 0.0,
    val laborHours: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vin: String,
    val dtcCode: String,
    val description: String,
    val status: String = "Stored", // Stored, Pending, Permanent
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Serializable
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "AI"
    val text: String,
    val skillName: String = "General",
    val vehicleVin: String = "",
    val projectTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "obd_telemetry_records")
data class ObdTelemetryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vin: String = "",
    val engineRpm: Int = 0,
    val vehicleSpeedMph: Int = 0,
    val coolantTempCelsius: Int = 0,
    val intakeAirTempCelsius: Int = 0,
    val throttlePositionPercent: Float = 0f,
    val batteryVoltage: Float = 0f,
    val boostPressurePsi: Float = 0f,
    val fuelTrimShortPercent: Float = 0f,
    val fuelTrimLongPercent: Float = 0f,
    val oilPressurePsi: Float = 0f,
    val dtcCodesFormatted: String = "", // Comma-separated list of active DTC codes e.g. "P0300,P0171"
    val connectionType: String = "USB_OTG", // USB_OTG, BLUETOOTH, SIMULATED
    val connectionStatusText: String = "Connected",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "dtc_error_codes")
data class DtcErrorCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vin: String = "",
    val dtcCode: String, // e.g. "P0300", "P0171"
    val systemCategory: String = "Powertrain", // Powertrain, Chassis, Body, Network (U-codes)
    val description: String = "",
    val status: String = "Stored", // Stored, Pending, Permanent
    val severity: String = "Medium", // High, Medium, Low
    val freezeFrameRpm: Int = 0,
    val freezeFrameCoolantTemp: Int = 0,
    val freezeFrameSpeedMph: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing a real-time parsed telemetry snapshot for UI presentation and stream processing
 */
@Serializable
data class ObdTelemetrySnapshot(
    val vin: String = "",
    val rpm: Int = 850,
    val speedMph: Int = 0,
    val coolantTempC: Int = 90,
    val intakeAirTempC: Int = 24,
    val throttlePosPct: Int = 14,
    val batteryVoltage: Float = 14.2f,
    val boostPressurePsi: Float = 0.0f,
    val activeDtcs: List<String> = emptyList(),
    val connectionType: String = "USB_OTG",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class for detailed DTC diagnostic analysis & guided troubleshooting steps
 */
@Serializable
data class DtcDetail(
    val code: String,
    val category: String,
    val definition: String,
    val possibleCauses: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList(),
    val severityLevel: String = "Medium",
    val isClearable: Boolean = true
)

/**
 * Data class for individual PID sensor metadata and raw readings
 */
@Serializable
data class ObdSensorPidData(
    val pidHex: String,
    val parameterName: String,
    val currentValue: String,
    val unit: String,
    val minNormalValue: String = "",
    val maxNormalValue: String = "",
    val status: String = "NORMAL" // NORMAL, WARNING, CRITICAL
)

