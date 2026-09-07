// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*

@Composable
fun EstimatorScreen() {
    var laborHours by remember { mutableStateOf("2.5") }
    var laborRate by remember { mutableStateOf("145.00") }
    var partsCost by remember { mutableStateOf("320.00") }
    var taxRatePct by remember { mutableStateOf("8.5") }

    val hours = laborHours.toDoubleOrNull() ?: 0.0
    val rate = laborRate.toDoubleOrNull() ?: 0.0
    val parts = partsCost.toDoubleOrNull() ?: 0.0
    val taxPct = taxRatePct.toDoubleOrNull() ?: 0.0

    val totalLabor = hours * rate
    val subtotal = totalLabor + parts
    val tax = subtotal * (taxPct / 100.0)
    val grandTotal = subtotal + tax

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = ForgeAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REPAIR & LABOR COST ESTIMATOR",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calculate shop job estimates, parts markup, and labor quotes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = laborHours,
                    onValueChange = { laborHours = it },
                    label = { Text("Labor Hours (e.g. 2.5)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = laborRate,
                    onValueChange = { laborRate = it },
                    label = { Text("Shop Labor Rate ($/hr)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = partsCost,
                    onValueChange = { partsCost = it },
                    label = { Text("Total Parts Subtotal ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxRatePct,
                    onValueChange = { taxRatePct = it },
                    label = { Text("Sales Tax Rate (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Summary Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("QUOTE BREAKDOWN SUMMARY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = ForgeBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Labor Charge ($hours hrs @ \$$rate/hr):", fontSize = 13.sp, color = ForgeOnSurface)
                    Text("\$${"%.2f".format(totalLabor)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Parts Subtotal:", fontSize = 13.sp, color = ForgeOnSurface)
                    Text("\$${"%.2f".format(parts)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Tax ($taxPct%):", fontSize = 13.sp, color = ForgeOnSurface)
                    Text("\$${"%.2f".format(tax)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                }
                HorizontalDivider(color = ForgeBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ESTIMATED GRAND TOTAL:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                    Text("\$${"%.2f".format(grandTotal)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ForgeAmber, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
