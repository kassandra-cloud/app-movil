package com.example.proyecto.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Definimos el esquema OSCURO usando tus AppColors pero adaptados
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Principal,      // Azul Vibrante (se ve bien en oscuro)
    secondary = AppColors.Secundario,
    tertiary = AppColors.IconoVotacion, // Usamos el amarillo como acento terciario

    // Colores de fondo para modo oscuro (Hardcoded porque no están en AppColors)
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

// 2. Definimos el esquema CLARO usando tus AppColors
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Secundario,     // Usamos el azul más oscuro para buen contraste
    secondary = AppColors.Principal,
    tertiary = AppColors.IconoTalleres, // Morado como acento

    background = AppColors.GrisClaroFondo,
    surface = AppColors.CardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
)

@Composable
fun ProyectoTheme(
    // Parámetros de configuración (vienen del ViewModel)
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    // Dynamic color (Android 12+)
    dynamicColor: Boolean = true,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Controla si los iconos de la barra (hora, batería) son blancos o negros
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Aquí conectamos la escala de fuente
        typography = getTypography(fontScale),
        content = content
    )
}