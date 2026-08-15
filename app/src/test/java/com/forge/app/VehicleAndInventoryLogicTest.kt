package com.forge.app

import com.forge.app.data.InventoryEntity
import com.forge.app.data.ProjectEntity
import com.forge.app.data.TaskEntity
import com.forge.app.data.VehicleEntity
import org.junit.Assert.*
import org.junit.Test

class VehicleAndInventoryLogicTest {

    object VinValidator {
        private val transliterationValues = mapOf(
            'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7, 'H' to 8,
            'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'P' to 7, 'R' to 9,
            'S' to 2, 'T' to 3, 'U' to 4, 'V' to 5, 'W' to 6, 'X' to 7, 'Y' to 8, 'Z' to 9,
            '0' to 0, '1' to 1, '2' to 2, '3' to 3, '4' to 4, '5' to 5, '6' to 6, '7' to 7, '8' to 8, '9' to 9
        )
        private val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

        fun isValidVinLength(vin: String): Boolean {
            val clean = vin.trim().uppercase()
            return clean.length == 17 && !clean.contains("I") && !clean.contains("O") && !clean.contains("Q")
        }

        fun computeCheckDigit(vin: String): Char {
            val clean = vin.trim().uppercase()
            if (clean.length != 17) return ' '
            var sum = 0
            for (i in 0 until 17) {
                val char = clean[i]
                val value = transliterationValues[char] ?: return ' '
                sum += value * weights[i]
            }
            val remainder = sum % 11
            return if (remainder == 10) 'X' else ('0' + remainder)
        }
    }

    object WorkshopInventoryCalculator {
        fun calculateTotalInventoryValue(items: List<InventoryEntity>): Double {
            return items.sumOf { it.stockQuantity * it.price }
        }

        fun identifyLowStockItems(items: List<InventoryEntity>): List<InventoryEntity> {
            return items.filter { it.stockQuantity <= it.reorderPoint }
        }

        fun calculateProjectCost(tasks: List<TaskEntity>, hourlyRate: Double = 125.0): Double {
            val estimatedLabor = tasks.size * 1.5 * hourlyRate
            return estimatedLabor
        }
    }

    @Test
    fun testVinValidation() {
        val validVin = "1HGCR2F83HA000000"
        assertTrue(VinValidator.isValidVinLength(validVin))
        assertEquals(17, validVin.length)

        // Invalid letters in VIN (I, O, Q not allowed per ISO standard)
        assertFalse(VinValidator.isValidVinLength("1HGCR2F83HA00000I"))
        assertFalse(VinValidator.isValidVinLength("1HGCR2F83HA00000O"))
        assertFalse(VinValidator.isValidVinLength("1HGCR2F83HA00000Q"))
        assertFalse(VinValidator.isValidVinLength("SHORTVIN123"))
    }

    @Test
    fun testInventoryCalculations() {
        val inventory = listOf(
            InventoryEntity(id = 1L, name = "Brembo Ceramic Brake Pads", partNumber = "P85020N", category = "Brakes", stockQuantity = 8, price = 75.0, cost = 50.0, reorderPoint = 3, location = "Bin A-12"),
            InventoryEntity(id = 2L, name = "NGK Iridium Spark Plugs", partNumber = "ILKER7A8EGS", category = "Ignition", stockQuantity = 2, price = 14.5, cost = 9.0, reorderPoint = 4, location = "Bin C-04"),
            InventoryEntity(id = 3L, name = "Motul 300V 5W-40 Synthetic Oil", partNumber = "104240", category = "Fluids", stockQuantity = 1, price = 22.0, cost = 16.0, reorderPoint = 2, location = "Rack 1"),
            InventoryEntity(id = 4L, name = "Bosch Direct Injection Injector", partNumber = "0261500073", category = "Fuel", stockQuantity = 6, price = 110.0, cost = 80.0, reorderPoint = 2, location = "Cabinet 3")
        )

        // Total inventory: (8*75) + (2*14.5) + (1*22) + (6*110) = 600 + 29 + 22 + 660 = $1311.00
        val totalVal = WorkshopInventoryCalculator.calculateTotalInventoryValue(inventory)
        assertEquals(1311.0, totalVal, 0.001)

        // Low stock items (stock <= reorderPoint): Spark plugs (2 <= 4) and Motul oil (1 <= 2)
        val lowStock = WorkshopInventoryCalculator.identifyLowStockItems(inventory)
        assertEquals(2, lowStock.size)
        assertEquals("NGK Iridium Spark Plugs", lowStock[0].name)
        assertEquals("Motul 300V 5W-40 Synthetic Oil", lowStock[1].name)
    }

    @Test
    fun testTaskAndProjectWorkflow() {
        val project = ProjectEntity(
            id = 101L,
            name = "Audi S5 Stage 2+ Dual Pulley Build",
            vehicleVin = "WAUZZZF58MA019284",
            customerName = "Alex Vance",
            status = "In Progress",
            budget = 4500.0
        )

        val tasks = listOf(
            TaskEntity(id = 1L, projectId = project.id, title = "Bench Flash ECU & TCU with Forge Map", description = "Load stage 2 ECU map", status = "Completed", priority = "High"),
            TaskEntity(id = 2L, projectId = project.id, title = "Install CTS Turbo Upgraded Intercooler", description = "Fit heat exchanger", status = "In Progress", priority = "High"),
            TaskEntity(id = 3L, projectId = project.id, title = "Dyno Logging & Fuel Trim Validation", description = "WOT runs in 3rd/4th", status = "Pending", priority = "Urgent")
        )

        assertEquals("Audi S5 Stage 2+ Dual Pulley Build", project.name)
        assertEquals(3, tasks.size)

        val completedTasks = tasks.count { it.status == "Completed" }
        val progressPercentage = (completedTasks.toFloat() / tasks.size) * 100f
        assertEquals(33.33f, progressPercentage, 0.05f)

        val laborCost = WorkshopInventoryCalculator.calculateProjectCost(tasks, 150.0)
        assertEquals(675.0, laborCost, 0.01)
    }

    @Test
    fun testVehicleEntityCreation() {
        val vehicle = VehicleEntity(
            id = 1L,
            vin = "WAUZZZF58MA019284",
            make = "Audi",
            model = "S5 Sportback",
            year = "2021",
            protocol = "CAN 11-bit / 500kbps",
            isConnected = true
        )

        assertEquals("Audi", vehicle.make)
        assertEquals("S5 Sportback", vehicle.model)
        assertEquals("2021", vehicle.year)
        assertTrue(vehicle.isConnected)
        assertEquals("CAN 11-bit / 500kbps", vehicle.protocol)
    }
}
