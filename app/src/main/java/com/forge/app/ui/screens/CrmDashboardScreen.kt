// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*

data class CustomerOrder(
    val name: String,
    val vehicle: String,
    val service: String,
    val stage: String // Waiting Parts, In Progress, Ready
)

@Composable
fun CrmDashboardScreen() {
    val orders = listOf(
        CustomerOrder("Marcus Vance", "2021 Audi S5", "Diagnostic & Injector Replacement", "In Progress"),
        CustomerOrder("Elena Rostova", "2019 Porsche 911", "DVI & Brake Flush", "Waiting Parts"),
        CustomerOrder("David Miller", "2022 BMW M4", "Stage 2 ECU Alignment", "Ready")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.People, contentDescription = null, tint = ForgeAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WORKSHOP CRM & REPAIR ORDERS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3 Active Repair Orders in Shop Pipeline",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(orders) { order ->
                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = order.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ForgeOnSurface)
                            Text(text = "${order.vehicle} • ${order.service}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ForgeCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = order.stage, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForgeCyan)
                        }
                    }
                }
            }
        }
    }
}
