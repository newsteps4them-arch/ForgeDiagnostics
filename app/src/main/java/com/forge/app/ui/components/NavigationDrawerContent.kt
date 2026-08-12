package com.forge.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector
)

val drawerMenuItems = listOf(
    // Diagnostics Suite
    DrawerMenuItem("dashboard", "Dashboard", "Overview, OBD Gauges & Tasks", "DIAGNOSTICS SUITE", Icons.Default.Dashboard),
    DrawerMenuItem("live_data", "Live Data Telemetry", "Real-time ECU PID sensor streams", "DIAGNOSTICS SUITE", Icons.Default.BarChart),
    DrawerMenuItem("topology", "ECU Network Topology", "CAN bus nodes & fault mapping", "DIAGNOSTICS SUITE", Icons.Default.AccountTree),
    DrawerMenuItem("guided_diag", "Guided AI Diagnostics", "AI trouble code repair assistant", "DIAGNOSTICS SUITE", Icons.Default.Psychology),
    DrawerMenuItem("oscilloscope", "Digital Oscilloscope", "2-channel signal wave analyzer", "DIAGNOSTICS SUITE", Icons.Default.ShowChart),
    DrawerMenuItem("terminal", "ELM327 Terminal", "Direct OBD AT command console", "DIAGNOSTICS SUITE", Icons.Default.Terminal),

    // Workshop & Garage Management
    DrawerMenuItem("garage", "Vehicle Garage", "Manage customer vehicle fleet", "WORKSHOP MANAGEMENT", Icons.Default.DirectionsCar),
    DrawerMenuItem("inventory", "Parts Catalog & Inventory", "Barcode scanner & stock tracking", "WORKSHOP MANAGEMENT", Icons.Default.Inventory2),
    DrawerMenuItem("estimator", "Repair Estimator", "Parts & labor cost calculations", "WORKSHOP MANAGEMENT", Icons.Default.Calculate),
    DrawerMenuItem("dvi", "Digital Inspection (DVI)", "Multi-point vehicle inspection report", "WORKSHOP MANAGEMENT", Icons.Default.Checklist),
    DrawerMenuItem("wiring", "Wiring Diagrams", "Interactive pinout & circuit schematics", "WORKSHOP MANAGEMENT", Icons.Default.Cable),
    DrawerMenuItem("time_clock", "Technician Punch Clock", "Labor time tracking", "WORKSHOP MANAGEMENT", Icons.Default.Schedule),
    DrawerMenuItem("crm", "CRM & Job Status", "Customer jobs & vehicle updates", "WORKSHOP MANAGEMENT", Icons.Default.People),

    // Settings
    DrawerMenuItem("settings", "System Settings", "Dongle connection & units", "SYSTEM", Icons.Default.Settings)
)

@Composable
fun NavigationDrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = ForgeBackground,
        drawerContentColor = ForgeOnSurface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TEAM FORGE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ForgeAmber,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Hardware & Automotive Suite",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCloseDrawer) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ForgeAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ForgeBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Menu Categories
            val categories = drawerMenuItems.map { it.category }.distinct()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { category ->
                    item {
                        Text(
                            text = category,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }

                    val itemsInCategory = drawerMenuItems.filter { it.category == category }
                    items(itemsInCategory) { item ->
                        val selected = currentRoute == item.route
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onNavigate(item.route)
                                    onCloseDrawer()
                                },
                            color = if (selected) ForgeAmber.copy(alpha = 0.15f) else ForgeSurface,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber) else androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) ForgeAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) ForgeAmber else ForgeOnSurface
                                    )
                                    Text(
                                        text = item.subtitle,
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
    }
}
