package com.forge.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        VehicleEntity::class,
        TaskEntity::class,
        InventoryEntity::class,
        WorkOrderEntity::class,
        DiagnosticLogEntity::class,
        UserSettingEntity::class,
        ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun taskDao(): TaskDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun diagnosticLogDao(): DiagnosticLogDao
    abstract fun userSettingDao(): UserSettingDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "forge_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

typealias ForgeDatabase = AppDatabase

