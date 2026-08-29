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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.data.VehicleEntity
import com.forge.app.ui.theme.*

@Composable
fun GarageScreen(
    vehicles: List<VehicleEntity>,
    onAddVehicle: (String, String, String, String) -> Unit,
    onSelectVehicle: (Long) -> Unit
) {
    var showAddModal by remember { mutableStateOf(false) }
    var vinInput by remember { mutableStateOf("WAUZZZF58MA019284") }
    var makeInput by remember { mutableStateOf("Audi") }
    var modelInput by remember { mutableStateOf("S5 Sportback") }
    var yearInput by remember { mutableStateOf("2021") }

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
                    text = "VEHICLE GARAGE & PROFILES",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeAmber
                )
                Text(
                    text = "Manage customer & project vehicle diagnostic profiles",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD VEHICLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (vehicles.isEmpty()) {
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No vehicles saved in garage yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForgeOnSurface)
                    Text("Tap ADD VEHICLE to store a profile.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vehicles) { veh ->
                    Surface(
                        color = ForgeSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (veh.isConnected) ForgeAmber else ForgeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${veh.year} ${veh.make} ${veh.model}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeOnSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VIN: ${veh.vin}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Protocol: ${veh.protocol}",
                                    fontSize = 11.sp,
                                    color = ForgeCyan
                                )
                            }

                            if (veh.isConnected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ForgeAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onSelectVehicle(veh.id) },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
                                ) {
                                    Text("SELECT", fontSize = 10.sp, color = ForgeAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddModal) {
        AlertDialog(
            onDismissRequest = { showAddModal = false },
            title = { Text("Add Vehicle Profile", color = ForgeAmber) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vinInput, onValueChange = { vinInput = it }, label = { Text("VIN Number") })
                    OutlinedTextField(value = makeInput, onValueChange = { makeInput = it }, label = { Text("Make (e.g. Audi)") })
                    OutlinedTextField(value = modelInput, onValueChange = { modelInput = it }, label = { Text("Model (e.g. S5)") })
                    OutlinedTextField(value = yearInput, onValueChange = { yearInput = it }, label = { Text("Year (e.g. 2021)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vinInput.isNotBlank()) {
                            onAddVehicle(vinInput, makeInput, modelInput, yearInput)
                            showAddModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black)
                ) {
                    Text("Save Vehicle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModal = false }) { Text("Cancel", color = Color.White) }
            },
            containerColor = ForgeSurface
        )
    }
}
