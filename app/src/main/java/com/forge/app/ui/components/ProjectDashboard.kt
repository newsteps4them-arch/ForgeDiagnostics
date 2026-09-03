package com.forge.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.data.ProjectEntity
import com.forge.app.data.TaskEntity
import com.forge.app.ui.theme.*

@Composable
fun ProjectDashboard(
    projects: List<ProjectEntity>,
    tasks: List<TaskEntity>,
    onAddTask: (projectId: Long, title: String, description: String, priority: String, category: String) -> Unit,
    onUpdateTaskStatus: (task: TaskEntity, newStatus: String) -> Unit,
    onDeleteTask: (task: TaskEntity) -> Unit,
    onAddProject: (name: String, vehicleVin: String, customerName: String, budget: Double) -> Unit,
    onUpdateProjectStatus: (project: ProjectEntity, newStatus: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var filterStatus by remember { mutableStateOf("All") }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Selected project or fallback to first
    val activeProject = projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()

    // Filtered tasks for active project (or all tasks if no project selected)
    val projectTasks = remember(tasks, activeProject, filterStatus) {
        val base = if (activeProject != null) {
            tasks.filter { it.projectId == activeProject.id || it.projectId == 0L }
        } else {
            tasks
        }
        if (filterStatus == "All") base else base.filter { it.status == filterStatus }
    }

    // Calculated metrics
    val totalProjects = projects.size
    val activeProjectsCount = projects.count { it.status == "Active" || it.status == "In Progress" }
    val completedTasksCount = tasks.count { it.status == "Completed" }
    val totalTasksCount = tasks.size.coerceAtLeast(1)
    val overallTaskProgress = (completedTasksCount.toFloat() / totalTasksCount.toFloat()).coerceIn(0f, 1f)
    val animatedTaskProgress by animateFloatAsState(
        targetValue = overallTaskProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "TaskProgressAnimation"
    )
    val totalBudget = projects.sumOf { it.budget }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header Banner & Aggregated Summary Cards ---
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Project Dashboard",
                            tint = ForgeAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROJECT & TASK AGGREGATOR",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddProjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Project", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Metric Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectMetricTile(
                        label = "ACTIVE PROJECTS",
                        value = "$activeProjectsCount / $totalProjects",
                        color = ForgeCyan,
                        icon = Icons.Default.Engineering,
                        modifier = Modifier.weight(1f)
                    )
                    ProjectMetricTile(
                        label = "TASK PROGRESS",
                        value = "${(overallTaskProgress * 100).toInt()}%",
                        color = ForgeGreen,
                        icon = Icons.Default.TaskAlt,
                        modifier = Modifier.weight(1f)
                    )
                    ProjectMetricTile(
                        label = "BUDGET ALLOC",
                        value = "\$${String.format("%.0f", totalBudget)}",
                        color = ForgeAmber,
                        icon = Icons.Default.AttachMoney,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Overall Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Overall Workshop Task Completion",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$completedTasksCount of ${tasks.size} tasks complete",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeGreen
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedTaskProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ForgeGreen,
                        trackColor = ForgeBorder
                    )
                }
            }
        }

        // --- Active Project Selector Tabs ---
        if (projects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT WORKSHOP PROJECT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeOnSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects) { project ->
                        val isSelected = (activeProject?.id == project.id)
                        Surface(
                            onClick = { selectedProjectId = project.id },
                            color = if (isSelected) ForgeAmber.copy(alpha = 0.15f) else ForgeSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ForgeAmber else ForgeBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (project.status) {
                                                "Active", "In Progress" -> ForgeGreen
                                                "Completed" -> ForgeCyan
                                                else -> ForgeRed
                                            }
                                        )
                                )
                                Column {
                                    Text(
                                        text = project.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ForgeAmber else ForgeOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${project.customerName} • ${project.status}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Active Project Detail Card ---
        activeProject?.let { proj ->
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = proj.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = when (proj.status) {
                                    "Completed" -> ForgeGreen.copy(alpha = 0.2f)
                                    "In Progress", "Active" -> ForgeAmber.copy(alpha = 0.2f)
                                    else -> ForgeRed.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = proj.status.uppercase(),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when (proj.status) {
                                        "Completed" -> ForgeGreen
                                        "In Progress", "Active" -> ForgeAmber
                                        else -> ForgeRed
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customer: ${proj.customerName} | VIN: ${proj.vehicleVin.ifBlank { "N/A" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BUDGET",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeOnSurfaceVariant
                        )
                        Text(
                            text = "\$${String.format("%.2f", proj.budget)}",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber
                        )
                    }
                }
            }
        }

        // --- Task List Section Header & Filter ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ACTIVE TASKS (${projectTasks.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeCyan
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Pending", "In Progress", "Completed").forEach { statusOption ->
                    val selected = (filterStatus == statusOption)
                    Surface(
                        onClick = { filterStatus = statusOption },
                        color = if (selected) ForgeCyan.copy(alpha = 0.2f) else ForgeSurface,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) ForgeCyan else ForgeBorder
                        )
                    ) {
                        Text(
                            text = statusOption,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) ForgeCyan else ForgeOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showAddTaskDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Task", tint = ForgeCyan)
                }
            }
        }

        // --- Task Item Cards with Entrance & Exit Animations ---
        if (projectTasks.isEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks found matching current filter for this project.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                projectTasks.forEach { task ->
                    key(task.id) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                                animationSpec = tween(200)
                            )
                        ) {
                            TaskCardItem(
                                task = task,
                                onStatusToggle = {
                                    val nextStatus = when (task.status) {
                                        "Pending" -> "In Progress"
                                        "In Progress" -> "Completed"
                                        else -> "Pending"
                                    }
                                    onUpdateTaskStatus(task, nextStatus)
                                },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Add Project Modal Dialog ---
    if (showAddProjectDialog) {
        var nameInput by remember { mutableStateOf("") }
        var vinInput by remember { mutableStateOf("") }
        var customerInput by remember { mutableStateOf("") }
        var budgetInput by remember { mutableStateOf("1000.00") }

        AlertDialog(
            onDismissRequest = { showAddProjectDialog = false },
            title = {
                Text("Create New Workshop Project", color = ForgeAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Project Title") },
                        placeholder = { Text("e.g. Audi S5 Fuel Injector Replacement") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerInput,
                        onValueChange = { customerInput = it },
                        label = { Text("Customer / Fleet Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vinInput,
                        onValueChange = { vinInput = it },
                        label = { Text("Vehicle VIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Project Budget ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onAddProject(
                                nameInput,
                                vinInput,
                                customerInput.ifBlank { "General Customer" },
                                budgetInput.toDoubleOrNull() ?: 500.0
                            )
                            showAddProjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black)
                ) {
                    Text("Create Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProjectDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = ForgeSurface
        )
    }

    // --- Add Task Modal Dialog ---
    if (showAddTaskDialog) {
        var titleInput by remember { mutableStateOf("") }
        var descInput by remember { mutableStateOf("") }
        var priorityInput by remember { mutableStateOf("High") }
        var categoryInput by remember { mutableStateOf("Diagnostic") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = {
                Text("Add Project Task", color = ForgeCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Replace Cylinder 1 Spark Plug") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description & Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Priority Selector
                    Text("Priority Level", fontSize = 11.sp, color = ForgeOnSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                            FilterChip(
                                selected = (priorityInput == p),
                                onClick = { priorityInput = p },
                                label = { Text(p, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Category Selector
                    Text("Category", fontSize = 11.sp, color = ForgeOnSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Diagnostic", "Mechanical", "Electrical", "Parts").forEach { c ->
                            FilterChip(
                                selected = (categoryInput == c),
                                onClick = { categoryInput = c },
                                label = { Text(c, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            onAddTask(
                                activeProject?.id ?: 1L,
                                titleInput,
                                descInput,
                                priorityInput,
                                categoryInput
                            )
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black)
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = ForgeSurface
        )
    }
}

@Composable
private fun TaskCardItem(
    task: TaskEntity,
    onStatusToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Smooth animated color transitions for card state changes
    val animatedBorderColor by animateColorAsState(
        targetValue = when (task.status) {
            "Completed" -> ForgeGreen.copy(alpha = 0.5f)
            "In Progress" -> ForgeAmber.copy(alpha = 0.8f)
            else -> ForgeBorder
        },
        animationSpec = tween(300),
        label = "TaskBorderColor"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = when (task.status) {
            "Completed" -> ForgeGreen.copy(alpha = 0.05f)
            "In Progress" -> ForgeAmber.copy(alpha = 0.05f)
            else -> ForgeSurface
        },
        animationSpec = tween(300),
        label = "TaskBgColor"
    )

    val animatedTitleColor by animateColorAsState(
        targetValue = if (task.status == "Completed") ForgeOnSurfaceVariant else ForgeOnSurface,
        animationSpec = tween(250),
        label = "TaskTitleColor"
    )

    val iconTint by animateColorAsState(
        targetValue = when (task.status) {
            "Completed" -> ForgeGreen
            "In Progress" -> ForgeAmber
            else -> ForgeOnSurfaceVariant
        },
        animationSpec = tween(250),
        label = "TaskIconTint"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (task.status == "Completed") 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "TaskIconScale"
    )

    Surface(
        color = animatedBgColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, animatedBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox / Status Toggle with Animated Icon Transition
                IconButton(
                    onClick = onStatusToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    AnimatedContent(
                        targetState = task.status,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.7f))
                                .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.7f))
                        },
                        label = "TaskStatusIconAnimation"
                    ) { currentStatus ->
                        Icon(
                            imageVector = when (currentStatus) {
                                "Completed" -> Icons.Default.CheckCircle
                                "In Progress" -> Icons.Default.Pending
                                else -> Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = "Toggle status for task: ${task.title}",
                            tint = iconTint,
                            modifier = Modifier.scale(iconScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = task.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedTitleColor,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (task.status == "Completed") androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority Badge
                        Surface(
                            color = when (task.priority) {
                                "Urgent", "High" -> ForgeRed.copy(alpha = 0.2f)
                                "Medium" -> ForgeAmber.copy(alpha = 0.2f)
                                else -> ForgeCyan.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = task.priority.uppercase(),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when (task.priority) {
                                    "Urgent", "High" -> ForgeRed
                                    "Medium" -> ForgeAmber
                                    else -> ForgeCyan
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        // Category Tag
                        if (task.category.isNotBlank()) {
                            Text(
                                text = "• ${task.category}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeOnSurfaceVariant
                            )
                        }

                        // Animated Status Badge
                        AnimatedVisibility(
                            visible = task.status != "Pending",
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f)
                        ) {
                            Surface(
                                color = when (task.status) {
                                    "Completed" -> ForgeGreen.copy(alpha = 0.2f)
                                    else -> ForgeAmber.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = task.status.uppercase(),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.status == "Completed") ForgeGreen else ForgeAmber,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete task: ${task.title}",
                    tint = ForgeOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProjectMetricTile(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ForgeBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurfaceVariant
                )
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Text(
                text = value,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
