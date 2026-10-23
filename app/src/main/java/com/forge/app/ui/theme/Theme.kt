// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

val ForgeAmber = Color(0xFFF5A623)
val ForgeCyan = Color(0xFF00F0FF)
val ForgeGreen = Color(0xFF00FF41)
val ForgeRed = Color(0xFFFF3B30)
val ForgeBackground = Color(0xFF0A0A0A)
val ForgeSurface = Color(0xFF141416)
val ForgeSurfaceVariant = Color(0xFF1E1E22)
val ForgeBorder = Color(0xFF27272A)
val ForgeOnSurface = Color(0xFFE4E4E7)
val ForgeOnSurfaceVariant = Color(0xFFA1A1AA)

private val DarkColorScheme = darkColorScheme(
    primary = ForgeAmber,
    onPrimary = Color.Black,
    secondary = ForgeCyan,
    onSecondary = Color.Black,
    tertiary = ForgeGreen,
    onTertiary = Color.Black,
    background = ForgeBackground,
    onBackground = Color.White,
    surface = ForgeSurface,
    onSurface = ForgeOnSurface,
    surfaceVariant = ForgeSurfaceVariant,
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = ForgeBorder,
    error = ForgeRed
)

@Composable
fun TeamForgeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ForgeBackground.toArgb()
            window.navigationBarColor = ForgeBackground.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
