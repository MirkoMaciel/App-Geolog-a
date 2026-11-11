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

/*
 * Cómo se usan los colores en la app:
 *  - primary   (Pizarra)      → barras superiores, encabezado de Inicio, botones comunes.
 *  - tertiary  (Óxido)        → acción principal: nueva salida, nueva parada, nuevo horizonte.
 *  - secondary (Olivino)      → estados ("Finalizada") e íconos secundarios.
 *  - background/surface (Caliza) y surfaceContainer* → fondo, tarjetas y desplegables.
 */
private val LightColorScheme = lightColorScheme(
    primary = Pizarra,
    onPrimary = Color.White,
    primaryContainer = PizarraClara,
    onPrimaryContainer = PizarraProfunda,
    secondary = Olivino,
    onSecondary = Color.White,
    secondaryContainer = OlivinoClaro,
    onSecondaryContainer = OlivinoProfundo,
    tertiary = OxidoHierro,
    onTertiary = Color.White,
    tertiaryContainer = OxidoClaro,
    onTertiaryContainer = OxidoProfundo,
    background = Caliza,
    onBackground = Pizarra,
    surface = Caliza,
    onSurface = Pizarra,
    surfaceVariant = CalizaTarjeta,
    onSurfaceVariant = GrisPizarra,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CalizaTarjetaSuave,
    surfaceContainer = CalizaTarjetaSuave,
    surfaceContainerHigh = CalizaTarjeta,
    surfaceContainerHighest = CalizaTarjeta,
    outline = BordePizarra,
    outlineVariant = Color(0xFFCFC9BD)
)

private val DarkColorScheme = darkColorScheme(
    primary = PizarraNoche,
    onPrimary = PizarraProfunda,
    primaryContainer = Pizarra,
    onPrimaryContainer = PizarraClara,
    secondary = OlivinoNoche,
    onSecondary = OlivinoProfundo,
    secondaryContainer = Color(0xFF3E5218),
    onSecondaryContainer = OlivinoClaro,
    tertiary = OxidoNoche,
    onTertiary = OxidoProfundo,
    tertiaryContainer = Color(0xFF8A3A0C),
    onTertiaryContainer = OxidoClaro,
    background = Color(0xFF161B21),
    surface = Color(0xFF161B21),
    surfaceVariant = Color(0xFF2A323B),
    surfaceContainer = Color(0xFF212830),
    surfaceContainerHigh = Color(0xFF2A323B),
    surfaceContainerHighest = Color(0xFF333C46)
)

@Composable
fun AppGeologiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivado para que la app use siempre la paleta "Cartografía y Estratos"
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