package com.forge.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.UpdateStatus

@Composable
fun UpdateDialog(
    updateStatus: UpdateStatus,
    currentVersion: String,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (updateStatus !is UpdateStatus.UpdateAvailable &&
        updateStatus !is UpdateStatus.Downloading &&
        updateStatus !is UpdateStatus.ReadyToInstall
    ) {
        return
    }

    AlertDialog(
        onDismissRequest = {
            if (updateStatus !is UpdateStatus.Downloading) {
                onDismiss()
            }
        },
        containerColor = Color(0xFF121820),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(16.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "System Update Icon",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "SOFTWARE UPDATE AVAILABLE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (updateStatus) {
                    is UpdateStatus.UpdateAvailable -> {
                        Surface(
                            color = Color(0xFF1E2836),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Current: v$currentVersion ➔ New: v${updateStatus.latestVersion}",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                updateStatus.release.name?.let { releaseName ->
                                    Text(
                                        text = releaseName,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        updateStatus.release.body?.let { notes ->
                            if (notes.isNotBlank()) {
                                Text(
                                    text = "RELEASE NOTES:",
                                    color = Color(0xFF8A99AD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = notes.take(300) + if (notes.length > 300) "..." else "",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    is UpdateStatus.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DOWNLOADING OTA BUILD...",
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            LinearProgressIndicator(
                                progress = { updateStatus.progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF00E676),
                                trackColor = Color(0xFF1E2836)
                            )
                            Text(
                                text = "${(updateStatus.progress * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    is UpdateStatus.ReadyToInstall -> {
                        Text(
                            text = "APK DOWNLOAD COMPLETE. LAUNCHING PACKAGE INSTALLER...",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {}
                }
            }
        },
        confirmButton = {
            if (updateStatus is UpdateStatus.UpdateAvailable) {
                Button(
                    onClick = onUpdateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Update Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPDATE NOW",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        dismissButton = {
            if (updateStatus is UpdateStatus.UpdateAvailable) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8A99AD))
                ) {
                    Text(
                        text = "LATER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
