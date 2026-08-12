package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
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
import com.forge.app.ui.theme.*

data class InspectionItem(
    val title: String,
    val category: String,
    val status: String // PASS, CAUTION, FAIL
)

@Composable
fun DviScreen() {
    val items = remember {
        mutableStateListOf(
            InspectionItem("Front Brake Pads & Rotors", "BRAKES & WHEELS", "PASS"),
            InspectionItem("Rear Brake Pads (4mm remaining)", "BRAKES & WHEELS", "CAUTION"),
            InspectionItem("Tire Tread Depth (5/32\")", "TIRES & SUSPENSION", "CAUTION"),
            InspectionItem("Engine Oil Level & Quality", "FLUIDS & FILTERS", "PASS"),
            InspectionItem("Cabin Air Filter", "FLUIDS & FILTERS", "FAIL"),
            InspectionItem("Battery Health (12.6V Static)", "ELECTRICAL", "PASS"),
            InspectionItem("Exhaust System & Catalytic Converter", "UNDERCARRIAGE", "PASS")
        )
    }

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
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Checklist, contentDescription = null, tint = ForgeGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DIGITAL VEHICLE INSPECTION (DVI)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeGreen
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "7 Points Checked • 4 Pass • 2 Caution • 1 Immediate Attention",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                val badgeColor = when (item.status) {
                    "PASS" -> ForgeGreen
                    "CAUTION" -> ForgeAmber
                    else -> ForgeRed
                }
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
                            Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForgeOnSurface)
                            Text(text = item.category, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = item.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                        }
                    }
                }
            }
        }
    }
}
