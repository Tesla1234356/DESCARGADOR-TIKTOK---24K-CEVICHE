package com.example.descargadortiktok.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TikTokPink,
    secondary = TikTokCyan,
    tertiary = TikTokGray,
    background = TikTokBlack,
    surface = TikTokDarkGray,
    onPrimary = Color.White,
    onSecondary = TikTokBlack,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TikTokPink,
    secondary = TikTokCyan,
    tertiary = TikTokGray,
    background = Color.White,
    surface = Color(0xFFF5F5F7),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun DescargadorTikTokTheme(
    darkTheme: Boolean = true, // Force Dark mode for a premium "wow" look
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our customized aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}