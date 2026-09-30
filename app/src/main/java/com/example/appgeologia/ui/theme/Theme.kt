package com.example.appgeologia.ui.theme

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
    primary = Terracota80,
    secondary = Arena80,
    tertiary = Pizarra80
)

private val LightColorScheme = lightColorScheme(
    primary = Terracota40,
    secondary = Arena40,
    tertiary = Pizarra40,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A1606),
    secondaryContainer = Color(0xFFF3E1BD),
    onSecondaryContainer = Color(0xFF2B1F05),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7),
    surfaceContainer = Color(0xFFF6EDE6),
    surfaceContainerLow = Color(0xFFFBF3EE),
    surfaceContainerHigh = Color(0xFFF0E6DF),
    surfaceContainerHighest = Color(0xFFEBDFD6),
    surfaceVariant = Color(0xFFEBDFD6)
)

@Composable
fun AppGeologiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivado para que la app use siempre la paleta tierra
    // en lugar de los colores del fondo de pantalla (Android 12+).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}