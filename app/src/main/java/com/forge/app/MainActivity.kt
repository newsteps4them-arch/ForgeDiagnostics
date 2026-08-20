package com.forge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.forge.app.data.AppDatabase
import com.forge.app.data.ForgeDatabase
import com.forge.app.data.ForgeRepository
import com.forge.app.data.InventoryEntity
import com.forge.app.data.ProjectEntity
import com.forge.app.data.TaskEntity
import com.forge.app.data.VehicleEntity
import com.forge.app.data.WorkOrderEntity
import com.forge.app.services.ObdTelemetryService
import com.forge.app.services.UsbHardwareCommunicationService
import com.forge.app.ui.components.BottomNavBar
import com.forge.app.ui.components.GeminiChatSheet
import com.forge.app.ui.components.NavigationDrawerContent
import com.forge.app.ui.components.PersistentAiFab
import com.forge.app.ui.components.TopStatusBar
import com.forge.app.ui.screens.*
import com.forge.app.ui.theme.TeamForgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: ForgeRepository
    private lateinit var usbHardwareService: UsbHardwareCommunicationService
    private lateinit var telemetryService: ObdTelemetryService
    private lateinit var authAndSyncService: com.forge.app.services.AuthAndSyncService
    private lateinit var geminiService: com.forge.app.services.GeminiService
    private lateinit var openManusService: com.forge.app.services.OpenManusAgentService
    private lateinit var obdHardwareModule: com.forge.app.services.ObdDiagnosticHardwareModule
    private lateinit var agentOrchestrator: com.forge.app.services.ForgeAgentOrchestrator
    private lateinit var cloudConnectorsManager: com.forge.app.services.CloudConnectorsManager
    private lateinit var autoTriageService: com.forge.app.services.AutoTriagePipelineService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        repository = ForgeRepository(db)
        usbHardwareService = UsbHardwareCommunicationService(lifecycleScope)
        telemetryService = ObdTelemetryService(lifecycleScope, usbHardwareService)
        authAndSyncService = com.forge.app.services.AuthAndSyncService(repository = repository)
        cloudConnectorsManager = com.forge.app.services.CloudConnectorsManager(repository = repository, scope = lifecycleScope)
        autoTriageService = com.forge.app.services.AutoTriagePipelineService(repository = repository, authAndSyncService = authAndSyncService, scope = lifecycleScope)
        geminiService = com.forge.app.services.GeminiService()
        openManusService = com.forge.app.services.OpenManusAgentService(geminiService = geminiService)
        obdHardwareModule = com.forge.app.services.ObdDiagnosticHardwareModule(
            scope = lifecycleScope,
            usbHardwareService = usbHardwareService,
            telemetryService = telemetryService,
            openManusService = openManusService
        )
        agentOrchestrator = com.forge.app.services.ForgeAgentOrchestrator(
            scope = lifecycleScope,
            repository = repository,
            usbHardwareService = usbHardwareService,
            telemetryService = telemetryService,
            authAndSyncService = authAndSyncService,
            geminiService = geminiService,
            openManusService = openManusService
        )


        // Seed initial sample vehicle, project, task, and inventory if database is fresh
        lifecycleScope.launch {
            repository.addProject(
                ProjectEntity(
                    name = "Audi S5 Engine Performance Diagnostics",
                    vehicleVin = "WAUZZZF58MA019284",
                    customerName = "Apex Motors",
                    status = "In Progress",
                    budget = 1450.00
                )
            )
            repository.addVehicle(
                VehicleEntity(
                    vin = "WAUZZZF58MA019284",
                    make = "Audi",
                    model = "S5 Sportback",
                    year = "2021",
                    protocol = "ISO 15765-4 (CAN 11bit/500k)",
                    isConnected = true
                )
            )
            repository.addTask(
                TaskEntity(
                    projectId = 1L,
                    title = "P0300 Misfire Diagnostic",
                    description = "Perform cylinder misfire isolation and high-pressure fuel pump check.",
                    status = "In Progress",
                    priority = "High"
                )
            )
            repository.addInventory(
                InventoryEntity(
                    partNumber = "0261500059",
                    name = "Bosch High-Pressure Injector",
                    category = "Fuel System",
                    stockQuantity = 4,
                    price = 112.50,
                    cost = 78.00,
                    reorderPoint = 2,
                    location = "Bin B-3",
                    supplier = "Bosch Automotive"
                )
            )
            repository.addWorkOrder(
                WorkOrderEntity(
                    projectTitle = "Audi S5 Misfire Repair",
                    vehicleVin = "WAUZZZF58MA019284",
                    status = "In Progress",
                    totalCost = 450.00,
                    laborHours = 2.5
                )
            )
        }

        setContent {
            TeamForgeTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()
                var currentRoute by remember { mutableStateOf("dashboard") }
                var showAiChatSheet by remember { mutableStateOf(false) }
                var aiInitialPrompt by remember { mutableStateOf<String?>(null) }

                val telemetry by telemetryService.telemetry.collectAsStateWithLifecycle()
                val projects by repository.projects.collectAsStateWithLifecycle(initialValue = emptyList())
                val vehicles by repository.vehicles.collectAsStateWithLifecycle(initialValue = emptyList())
                val tasks by repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
                val inventory by repository.inventory.collectAsStateWithLifecycle(initialValue = emptyList())

                val activeVehicleName = vehicles.firstOrNull { it.isConnected }?.let {
                    "${it.year} ${it.make} ${it.model}"
                } ?: "2021 Audi S5 Sportback"

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        NavigationDrawerContent(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                currentRoute = route
                                coroutineScope.launch { drawerState.close() }
                            },
                            onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopStatusBar(
                                telemetry = telemetry,
                                activeVehicleName = activeVehicleName,
                                onToggleConnection = { telemetryService.toggleConnection() },
                                onOpenAiChat = { showAiChatSheet = true },
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                        },
                        bottomBar = {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { currentRoute = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentRoute) {
                                "dashboard" -> DashboardScreen(
                                    telemetry = telemetry,
                                    projects = projects,
                                    tasks = tasks,
                                    autoTriageService = autoTriageService,
                                    onNavigate = { currentRoute = it },
                                    onAddTask = { projId, title, desc, prio, cat ->
                                        lifecycleScope.launch {
                                            repository.addTask(
                                                TaskEntity(
                                                    projectId = projId,
                                                    title = title,
                                                    description = desc,
                                                    priority = prio,
                                                    category = cat
                                                )
                                            )
                                        }
                                    },
                                    onUpdateTaskStatus = { task, newStatus ->
                                        lifecycleScope.launch {
                                            repository.updateTask(task.copy(status = newStatus))
                                        }
                                    },
                                    onDeleteTask = { task ->
                                        lifecycleScope.launch {
                                            repository.deleteTask(task)
                                        }
                                    },
                                    onAddProject = { name, vin, customer, budget ->
                                        lifecycleScope.launch {
                                            repository.addProject(
                                                ProjectEntity(
                                                    name = name,
                                                    vehicleVin = vin,
                                                    customerName = customer,
                                                    budget = budget
                                                )
                                            )
                                        }
                                    },
                                    onUpdateProjectStatus = { project, newStatus ->
                                        lifecycleScope.launch {
                                            repository.updateProject(project.copy(status = newStatus))
                                        }
                                    },
                                    onClearDtcs = { telemetryService.clearDtcs() }
                                )
                                "openmanus" -> OpenManusScreen(
                                    openManusService = openManusService,
                                    telemetry = telemetry,
                                    activeVehicleName = activeVehicleName,
                                    hardwareModule = obdHardwareModule,
                                    onAddTaskToWorkOrder = { title, desc ->
                                        lifecycleScope.launch {
                                            repository.addTask(
                                                TaskEntity(
                                                    projectId = 1L,
                                                    title = title,
                                                    description = desc,
                                                    priority = "High",
                                                    category = "OpenManus AI"
                                                )
                                            )
                                        }
                                    }
                                )
                                "live_data" -> LiveDataScreen(telemetry = telemetry)
                                "topology" -> TopologyScreen()
                                "guided_diag" -> GuidedDiagnosticsScreen()
                                "oscilloscope" -> OscilloscopeScreen()
                                "terminal" -> TerminalScreen(usbHardwareService = usbHardwareService)
                                "garage" -> GarageScreen(
                                    vehicles = vehicles,
                                    onAddVehicle = { vin, make, model, year ->
                                        lifecycleScope.launch {
                                            repository.addVehicle(VehicleEntity(vin = vin, make = make, model = model, year = year))
                                        }
                                    },
                                    onSelectVehicle = { id ->
                                        lifecycleScope.launch {
                                            repository.setConnectedVehicle(id)
                                        }
                                    }
                                )
                                "inventory" -> PartsCatalogScreen(
                                    inventory = inventory,
                                    onAddPart = { pn, name, cat, qty, price, bin ->
                                        lifecycleScope.launch {
                                            repository.addInventory(
                                                InventoryEntity(
                                                    partNumber = pn,
                                                    name = name,
                                                    category = cat,
                                                    stockQuantity = qty,
                                                    price = price,
                                                    location = bin
                                                )
                                            )
                                        }
                                    },
                                    onUpdateStock = { item, newQty ->
                                        lifecycleScope.launch {
                                            repository.updateInventory(item.copy(stockQuantity = newQty))
                                        }
                                    }
                                )
                                "estimator" -> EstimatorScreen()
                                "dvi" -> DviScreen()
                                "wiring" -> WiringDiagramsScreen()
                                "time_clock" -> TimeClockScreen()
                                "crm" -> CrmDashboardScreen()
                                "orchestrator" -> AgentOrchestratorScreen(
                                    orchestrator = agentOrchestrator,
                                    onNavigate = { currentRoute = it }
                                )
                                "settings" -> SettingsScreen(
                                    currentConnectionType = telemetry.connectionType,
                                    authAndSyncService = authAndSyncService,
                                    usbHardwareService = usbHardwareService,
                                    cloudConnectorsManager = cloudConnectorsManager,
                                    onConnectionTypeChange = { type -> telemetryService.setConnectionType(type) },
                                    onResetDatabase = {
                                        lifecycleScope.launch {
                                            repository.clearLogs()
                                            repository.clearChatHistory()
                                            telemetryService.clearDtcs()
                                        }
                                    }
                                )

                                else -> DashboardScreen(
                                    telemetry = telemetry,
                                    projects = projects,
                                    tasks = tasks,
                                    onNavigate = { currentRoute = it },
                                    onAddTask = { projId, title, desc, prio, cat ->
                                        lifecycleScope.launch {
                                            repository.addTask(
                                                TaskEntity(
                                                    projectId = projId,
                                                    title = title,
                                                    description = desc,
                                                    priority = prio,
                                                    category = cat
                                                )
                                            )
                                        }
                                    },
                                    onUpdateTaskStatus = { task, newStatus ->
                                        lifecycleScope.launch {
                                            repository.updateTask(task.copy(status = newStatus))
                                        }
                                    },
                                    onDeleteTask = { task ->
                                        lifecycleScope.launch {
                                            repository.deleteTask(task)
                                        }
                                    },
                                    onAddProject = { name, vin, customer, budget ->
                                        lifecycleScope.launch {
                                            repository.addProject(
                                                ProjectEntity(
                                                    name = name,
                                                    vehicleVin = vin,
                                                    customerName = customer,
                                                    budget = budget
                                                )
                                            )
                                        }
                                    },
                                    onUpdateProjectStatus = { project, newStatus ->
                                        lifecycleScope.launch {
                                            repository.updateProject(project.copy(status = newStatus))
                                        }
                                    },
                                    onClearDtcs = { telemetryService.clearDtcs() }
                                )
                            }

                            // Persistent Screen-Specialized AI Assistant floating button
                            PersistentAiFab(
                                currentRoute = currentRoute,
                                onOpenAiChat = { prompt ->
                                    aiInitialPrompt = prompt
                                    showAiChatSheet = true
                                },
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }

                        if (showAiChatSheet) {
                            GeminiChatSheet(
                                currentRoute = currentRoute,
                                initialPrompt = aiInitialPrompt,
                                activeVehicle = activeVehicleName,
                                activeTelemetry = "RPM: ${telemetry.rpm} | Temp: ${telemetry.coolantTempC}°C | Voltage: ${"%.1f".format(telemetry.batteryVoltage)}V",
                                activeProject = "Engine Performance Misfire Diagnostic",
                                repository = repository,
                                authAndSyncService = authAndSyncService,
                                onDismiss = {
                                    showAiChatSheet = false
                                    aiInitialPrompt = null
                                }
                            )
                        }

                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::usbHardwareService.isInitialized) {
            usbHardwareService.unregisterPermissionReceiver(applicationContext)
            usbHardwareService.disconnect()
        }
    }
}
