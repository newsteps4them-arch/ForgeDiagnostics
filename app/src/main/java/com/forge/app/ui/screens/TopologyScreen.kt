package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*

data class EcuNode(
    val name: String,
    val bus: String, // CAN-H/L, LIN, MOST
    val address: String,
    val dtcCount: Int,
    val status: String // OK, FAULT, OFFLINE
)

@Composable
fun TopologyScreen() {
    val nodes = listOf(
        EcuNode("ECM - Engine Control Module", "CAN-High (500k)", "0x7E0", 2, "FAULT"),
        EcuNode("TCM - Transmission Control Module", "CAN-High (500k)", "0x7E1", 0, "OK"),
        EcuNode("ABS / ESP - Brakes & Stability", "CAN-High (500k)", "0x7E2", 0, "OK"),
        EcuNode("BCM - Body Control Module", "CAN-Low (125k)", "0x7E3", 0, "OK"),
        EcuNode("ADAS - Front Radar & Camera", "CAN-FD (2M)", "0x7E4", 0, "OK"),
        EcuNode("SRS - Airbag Safety Unit", "CAN-High (500k)", "0x7E5", 0, "OK"),
        EcuNode("Gateway - Central Gateway", "Ethernet / DoIP", "0x700", 0, "OK")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Bus Header
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccountTree, contentDescription = null, tint = ForgeCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAN/LIN BUS NETWORK TOPOLOGY",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Active Gateways: 1 • Online Modules: 7/7 • Baud Rate: 500 kbps",
                    fontSize = 12.sp,
                    color = ForgeOnSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(nodes) { node ->
                val isFault = node.status == "FAULT"
                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFault) ForgeRed else ForgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFault) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isFault) ForgeRed else ForgeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = node.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeOnSurface
                                )
                                Text(
                                    text = "Bus: ${node.bus} • Addr: ${node.address}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isFault) ForgeRed.copy(alpha = 0.2f) else ForgeGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isFault) "${node.dtcCount} DTCs" else "NODE OK",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isFault) ForgeRed else ForgeGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
