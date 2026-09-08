// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.ScreenAiContextRegistry
import com.forge.app.ui.theme.*

/**
 * Persistent Floating AI Action Button with Screen Specialization
 * Persists across every single screen in Team Forge.
 */
@Composable
fun PersistentAiFab(
    currentRoute: String,
    onOpenAiChat: (initialPrompt: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val contextInfo = ScreenAiContextRegistry.getContextForRoute(currentRoute)

    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_pulse_glow"
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .padding(end = 16.dp, bottom = 12.dp)
            .testTag("persistent_ai_fab_container")
    ) {
        // Quick contextual prompt suggestion chip (screen-specific)
        val firstPrompt = contextInfo.suggestedPrompts.firstOrNull()
        if (firstPrompt != null) {
            Surface(
                color = ForgeSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.5f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clickable { onOpenAiChat(firstPrompt) }
                    .testTag("ai_screen_specialist_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Ask Specialist",
                        tint = ForgeCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = firstPrompt,
                        fontSize = 11.sp,
                        color = ForgeOnSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = ForgeAmber,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Main Specialized AI FAB Button
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = ForgeCyan.copy(alpha = pulseGlow)
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .clickable { onOpenAiChat(null) }
                .testTag("persistent_ai_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ForgeCyan.copy(alpha = 0.2f))
                        .border(1.dp, ForgeCyan.copy(alpha = pulseGlow), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Forge AI Specialist",
                        tint = ForgeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ForgeGreen.copy(alpha = pulseGlow))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI SPECIALIST",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Text(
                        text = contextInfo.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForgeOnSurface,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
