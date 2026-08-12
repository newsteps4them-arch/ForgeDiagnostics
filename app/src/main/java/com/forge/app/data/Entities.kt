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

