// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.forge.app.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val mainNavItems = listOf(
    BottomNavItem("dashboard", "Dashboard", Icons.Default.Dashboard),
    BottomNavItem("live_data", "Live PIDs", Icons.Default.BarChart),
    BottomNavItem("topology", "ECU Bus", Icons.Default.AccountTree),
    BottomNavItem("guided_diag", "AI Diag", Icons.Default.Psychology),
    BottomNavItem("garage", "Garage", Icons.Default.DirectionsCar)
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = ForgeSurface,
        contentColor = ForgeOnSurface,
        modifier = Modifier
            .border(width = 1.dp, color = ForgeBorder)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        mainNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) ForgeAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        color = if (selected) ForgeAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ForgeAmber.copy(alpha = 0.2f)
                )
            )
        }
    }
}
