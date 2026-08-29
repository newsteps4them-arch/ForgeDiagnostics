// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.data

import kotlinx.coroutines.flow.Flow

class ForgeRepository(private val db: AppDatabase) {
    val projects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val vehicles: Flow<List<VehicleEntity>> = db.vehicleDao().getAllVehicles()
    val tasks: Flow<List<TaskEntity>> = db.taskDao().getAllTasks()
    val inventory: Flow<List<InventoryEntity>> = db.inventoryDao().getAllInventory()
    val lowStockInventory: Flow<List<InventoryEntity>> = db.inventoryDao().getLowStockInventory()
    val workOrders: Flow<List<WorkOrderEntity>> = db.workOrderDao().getAllWorkOrders()
    val diagnosticLogs: Flow<List<DiagnosticLogEntity>> = db.diagnosticLogDao().getAllLogs()

    suspend fun addProject(project: ProjectEntity) = db.projectDao().insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = db.projectDao().updateProject(project)
    suspend fun deleteProject(project: ProjectEntity) = db.projectDao().deleteProject(project)

    suspend fun addVehicle(vehicle: VehicleEntity) = db.vehicleDao().insertVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: VehicleEntity) = db.vehicleDao().deleteVehicle(vehicle)
    suspend fun setConnectedVehicle(id: Long) {
        db.vehicleDao().disconnectAllVehicles()
        db.vehicleDao().setConnected(id)
    }

    suspend fun addTask(task: TaskEntity) = db.taskDao().insertTask(task)
    suspend fun updateTask(task: TaskEntity) = db.taskDao().updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = db.taskDao().deleteTask(task)

    suspend fun addInventory(item: InventoryEntity) = db.inventoryDao().insertInventory(item)
    suspend fun updateInventory(item: InventoryEntity) = db.inventoryDao().updateInventory(item)
    suspend fun deleteInventory(item: InventoryEntity) = db.inventoryDao().deleteInventory(item)
    fun searchInventory(query: String): Flow<List<InventoryEntity>> = db.inventoryDao().searchInventory(query)

    suspend fun addWorkOrder(workOrder: WorkOrderEntity) = db.workOrderDao().insertWorkOrder(workOrder)
    suspend fun updateWorkOrder(workOrder: WorkOrderEntity) = db.workOrderDao().updateWorkOrder(workOrder)
    suspend fun deleteWorkOrder(workOrder: WorkOrderEntity) = db.workOrderDao().deleteWorkOrder(workOrder)

    suspend fun addDiagnosticLog(log: DiagnosticLogEntity) = db.diagnosticLogDao().insertLog(log)
    suspend fun clearLogs() = db.diagnosticLogDao().clearLogs()

    val chatMessages: Flow<List<ChatMessageEntity>> = db.chatMessageDao().getAllChatMessages()

    suspend fun addChatMessage(message: ChatMessageEntity) = db.chatMessageDao().insertChatMessage(message)
    suspend fun clearChatHistory() = db.chatMessageDao().clearChatHistory()

    suspend fun getSetting(key: String): String? = db.userSettingDao().getSetting(key)

    suspend fun saveSetting(key: String, value: String) {
        db.userSettingDao().saveSetting(UserSettingEntity(key, value))
    }

    // OBD-II Real-time Telemetry & DTC Methods
    val telemetryRecords: Flow<List<ObdTelemetryRecordEntity>> = db.obdTelemetryDao().getAllTelemetryRecords()
    val dtcErrorCodes: Flow<List<DtcErrorCodeEntity>> = db.dtcErrorCodeDao().getAllDtcCodes()
    val latestTelemetryRecord: Flow<ObdTelemetryRecordEntity?> = db.obdTelemetryDao().getLatestTelemetryRecord()

    fun getTelemetryForVin(vin: String): Flow<List<ObdTelemetryRecordEntity>> = db.obdTelemetryDao().getTelemetryForVin(vin)
    fun getDtcCodesForVin(vin: String): Flow<List<DtcErrorCodeEntity>> = db.dtcErrorCodeDao().getDtcCodesForVin(vin)

    suspend fun addTelemetryRecord(record: ObdTelemetryRecordEntity): Long = db.obdTelemetryDao().insertTelemetryRecord(record)
    suspend fun clearTelemetryRecords() = db.obdTelemetryDao().clearTelemetryRecords()

    suspend fun addDtcCode(dtc: DtcErrorCodeEntity): Long = db.dtcErrorCodeDao().insertDtcCode(dtc)
    suspend fun addDtcCodes(dtcs: List<DtcErrorCodeEntity>) = db.dtcErrorCodeDao().insertDtcCodes(dtcs)
    suspend fun deleteDtcCodeById(id: Long) = db.dtcErrorCodeDao().deleteDtcCodeById(id)
    suspend fun clearDtcCodesForVin(vin: String) = db.dtcErrorCodeDao().clearDtcCodesForVin(vin)
    suspend fun clearAllDtcCodes() = db.dtcErrorCodeDao().clearAllDtcCodes()
}


