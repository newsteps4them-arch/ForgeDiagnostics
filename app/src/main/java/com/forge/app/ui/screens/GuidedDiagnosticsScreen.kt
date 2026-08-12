package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.GeminiClient
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GuidedDiagnosticsScreen() {
    val coroutineScope = rememberCoroutineScope()
    var dtcInput by remember { mutableStateOf("P0300") }
    var analysisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = ForgeAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI GUIDED DIAGNOSTIC WORKFLOW",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter any DTC code or symptom to generate step-by-step OEM test procedures.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Code Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dtcInput,
                onValueChange = { dtcInput = it.uppercase() },
                label = { Text("DTC Code (e.g., P0300, P0171, P0420)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeAmber,
                    unfocusedBorderColor = ForgeBorder,
                    focusedContainerColor = ForgeSurface,
                    unfocusedContainerColor = ForgeSurface
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (dtcInput.isNotBlank()) {
                        isLoading = true
                        coroutineScope.launch {
                            analysisResult = GeminiClient.queryAssistant(
                                prompt = "Provide step-by-step diagnostic procedures for DTC code $dtcInput on 2021 Audi S5 Sportback."
                            )
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            }
        }

        // Result Surface
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OEM TEST PROCEDURE & GUIDANCE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeCyan
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ForgeAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Querying OEM Service Bulletins & AI Engine...", fontSize = 12.sp, color = ForgeAmber)
                        }
                    }
                } else {
                    val textToDisplay = if (analysisResult.isBlank()) {
                        "Tap search or enter a DTC code above to analyze causes, symptoms, and pinout checks."
                    } else analysisResult

                    Text(
                        text = textToDisplay,
                        fontSize = 13.sp,
                        color = ForgeOnSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
