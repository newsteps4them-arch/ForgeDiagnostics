package com.forge.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY lastConnectedTime DESC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE vin = :vin LIMIT 1")
    fun getVehicleByVin(vin: String): Flow<VehicleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET isConnected = 0")
    suspend fun disconnectAllVehicles()

    @Query("UPDATE vehicles SET isConnected = 1 WHERE id = :id")
    suspend fun setConnected(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksForProject(projectId: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY name ASC")
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE stockQuantity <= reorderPoint ORDER BY name ASC")
    fun getLowStockInventory(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE name LIKE '%' || :query || '%' OR partNumber LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchInventory(query: String): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(item: InventoryEntity): Long

    @Update
    suspend fun updateInventory(item: InventoryEntity)

    @Delete
    suspend fun deleteInventory(item: InventoryEntity)
}

@Dao
interface WorkOrderDao {
    @Query("SELECT * FROM work_orders ORDER BY createdAt DESC")
    fun getAllWorkOrders(): Flow<List<WorkOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkOrder(workOrder: WorkOrderEntity): Long

    @Update
    suspend fun updateWorkOrder(workOrder: WorkOrderEntity)

    @Delete
    suspend fun deleteWorkOrder(workOrder: WorkOrderEntity)
}

@Dao
interface DiagnosticLogDao {
    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DiagnosticLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DiagnosticLogEntity): Long

    @Query("DELETE FROM diagnostic_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}

@Dao
interface UserSettingDao {
    @Query("SELECT value FROM user_settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: UserSettingEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}

