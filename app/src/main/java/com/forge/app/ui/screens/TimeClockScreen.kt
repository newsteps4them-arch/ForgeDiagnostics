// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
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
fun TimeClockScreen() {
    var isClockedIn by remember { mutableStateOf(true) }
    var clockedTime by remember { mutableStateOf("03:42:15") }

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
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isClockedIn) "CLOCKED IN - JOB #1092" else "CLOCKED OUT",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isClockedIn) ForgeGreen else ForgeRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = clockedTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeAmber
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { isClockedIn = !isClockedIn },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isClockedIn) ForgeRed else ForgeGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isClockedIn) "PUNCH OUT" else "PUNCH IN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
