// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.data.InventoryEntity
import com.forge.app.ui.components.BarcodeScannerSheet
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PartsCatalogScreen(
    inventory: List<InventoryEntity>,
    onAddPart: (String, String, String, Int, Double, String) -> Unit,
    onUpdateStock: (InventoryEntity, Int) -> Unit
) {
    var showBarcodeScannerSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("NGK-SILZKFR8E7S") }
    var isSearchingB2b by remember { mutableStateOf(false) }
    var b2bParts by remember { mutableStateOf<List<com.forge.app.services.NexpartPartItem>>(emptyList()) }
    var lastOrderConfirmation by remember { mutableStateOf<com.forge.app.services.NexpartOrderResponse?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        b2bParts = com.forge.app.services.NexpartClient.searchB2bInventory(partNumberQuery = searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PARTS CATALOG & NEXPART B2B",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeAmber
                )
                Text(
                    text = "Live AutoZone Commercial, Worldpac, & Advance Pro stock.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showBarcodeScannerSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BARCODE SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Nexpart B2B Distributor Live Query Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NEXPART B2B WHOLESALE DISTRIBUTOR SEARCH", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                    if (isSearchingB2b) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ForgeAmber, strokeWidth = 2.dp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Part # or Keyword") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            isSearchingB2b = true
                            coroutineScope.launch {
                                b2bParts = com.forge.app.services.NexpartClient.searchB2bInventory(partNumberQuery = searchQuery)
                                isSearchingB2b = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Search B2B")
                    }
                }

                if (lastOrderConfirmation != null) {
                    Surface(
                        color = ForgeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ORDER CONFIRMED: ${lastOrderConfirmation?.orderId}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForgeGreen, fontFamily = FontFamily.Monospace)
                            Text("Distributor: ${lastOrderConfirmation?.distributorRef} • Arrival: ${lastOrderConfirmation?.estimatedArrival}", fontSize = 11.sp, color = ForgeOnSurface)
                        }
                    }
                }

                // B2B Search Results List
                b2bParts.forEach { b2bPart ->
                    Surface(
                        color = ForgeSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${b2bPart.brand} ${b2bPart.partNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForgeOnSurface)
                                Text(b2bPart.description, fontSize = 11.sp, color = ForgeOnSurfaceVariant)
                                Text("${b2bPart.distributorName} • Delivery: ${b2bPart.estimatedDeliveryTime}", fontSize = 10.sp, color = ForgeCyan)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Wholesale: \$${b2bPart.wholesalePrice}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForgeGreen)
                                Text("Retail: \$${b2bPart.retailPrice}", fontSize = 10.sp, color = ForgeOnSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val confirmation = com.forge.app.services.NexpartClient.placeB2bPartOrder(b2bPart.partNumber, quantity = 1)
                                            lastOrderConfirmation = confirmation
                                            onAddPart(b2bPart.partNumber, "${b2bPart.brand} ${b2bPart.description}", b2bPart.category, 1, b2bPart.retailPrice, "A1-B2B")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Order Hot Shot", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Automated Low-Stock Re-Order & Replenishment Watchdog Card
        val lowStockItems = remember(inventory) { inventory.filter { it.stockQuantity <= 2 } }
        if (lowStockItems.isNotEmpty()) {
            Surface(
                color = ForgeAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = ForgeAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUTOMATED RE-ORDER WATCHDOG (${lowStockItems.size} LOW)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                        }
                        Surface(
                            color = ForgeAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "THRESHOLD <= 2",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "The inventory engine detected ${lowStockItems.size} item(s) below reserve threshold (${lowStockItems.joinToString { "${it.name} (${it.stockQuantity})" }}).",
                        fontSize = 11.sp,
                        color = ForgeOnSurface,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                lowStockItems.forEach { item ->
                                    onUpdateStock(item, item.stockQuantity + 4)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ AUTO-REORDER ALL LOW-STOCK ITEMS (+4 Each)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Inventory Category Distribution Visual Chart Summary Card
        if (inventory.isNotEmpty()) {
            val categoryColors = listOf(
                ForgeCyan,
                ForgeAmber,
                ForgeGreen,
                ForgeRed,
                Color(0xFFAB47BC), // Purple
                Color(0xFFFF7043)  // Deep Orange
            )

            val categoryGroup = remember(inventory) {
                inventory.groupBy { it.category }
                    .mapValues { entry ->
                        val count = entry.value.sumOf { it.stockQuantity }
                        val value = entry.value.sumOf { it.price * it.stockQuantity }
                        count to value
                    }
            }

            val totalStockCount = remember(inventory) { inventory.sumOf { it.stockQuantity } }.coerceAtLeast(1)
            val totalValuation = remember(inventory) { inventory.sumOf { it.price * it.stockQuantity } }

            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = ForgeCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WORKSHOP CATEGORY OVERVIEW",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeCyan
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "VALUATION: \$${String.format("%.2f", totalValuation)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                            Text(
                                text = "$totalStockCount Total Units in Stock",
                                fontSize = 10.sp,
                                color = ForgeOnSurfaceVariant
                            )
                        }
                    }

                    // Visual Donut Chart & Stacked Distribution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Canvas Donut Chart
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var startAngle = -90f
                                val strokeWidth = 12.dp.toPx()
                                var colorIdx = 0

                                categoryGroup.forEach { (_, data) ->
                                    val (count, _) = data
                                    val sweepAngle = (count.toFloat() / totalStockCount) * 360f
                                    val color = categoryColors[colorIdx % categoryColors.size]

                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    startAngle += sweepAngle
                                    colorIdx++
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${categoryGroup.size}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeOnSurface
                                )
                                Text(
                                    text = "CATEGORIES",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeOnSurfaceVariant
                                )
                            }
                        }

                        // Category Percentage Legend Chips
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            var colorIdx = 0
                            categoryGroup.entries.take(4).forEach { (cat, data) ->
                                val (count, valuated) = data
                                val pct = (count.toFloat() / totalStockCount) * 100f
                                val color = categoryColors[colorIdx % categoryColors.size]
                                colorIdx++

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForgeOnSurface
                                        )
                                    }

                                    Text(
                                        text = "${String.format("%.1f", pct)}% ($count pcs • \$${valuated.toInt()})",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (inventory.isEmpty()) {
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No parts logged in inventory yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForgeOnSurface)
                    Text("Tap BARCODE SCAN to scan workshop tags or register parts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(inventory) { item ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ForgeOnSurface)
                                Text(
                                    text = "PN: ${item.partNumber} • Category: ${item.category}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Bin Location: ${item.location} • Supplier: ${item.supplier}",
                                    fontSize = 11.sp,
                                    color = ForgeCyan
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "\$${item.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateStock(item, (item.stockQuantity - 1).coerceAtLeast(0)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Deduct", tint = ForgeRed, modifier = Modifier.size(16.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (item.stockQuantity > 0) ForgeGreen.copy(alpha = 0.2f) else ForgeRed.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${item.stockQuantity} IN STOCK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.stockQuantity > 0) ForgeGreen else ForgeRed
                                        )
                                    }

                                    IconButton(
                                        onClick = { onUpdateStock(item, item.stockQuantity + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Receive", tint = ForgeGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBarcodeScannerSheet) {
        BarcodeScannerSheet(
            inventory = inventory,
            onUpdateStock = onUpdateStock,
            onAddPart = { pn, name, cat, qty, price, bin ->
                onAddPart(pn, name, cat, qty, price, bin)
            },
            onDismiss = { showBarcodeScannerSheet = false }
        )
    }
}


