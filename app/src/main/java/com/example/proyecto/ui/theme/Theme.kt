package com.example.proyecto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Secondary,
    onSecondary = TextPrimary,
    tertiary = Tertiary,
    background = Lavender,
    surface = CardBg,
    onSurface = TextPrimary
)

@Composable
fun ProyectoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forzamos claro para el estilo pastel (cámbialo si quieres soportar dark)
    val colors = LightColors

    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        ),
        typography = Typography(
            displayMedium = Typography().displayMedium.copy(lineHeight = 40.sp),
            titleLarge   = Typography().titleLarge.copy(fontSize = 22.sp),
            bodyLarge    = Typography().bodyLarge.copy(fontSize = 18.sp),
            labelLarge   = Typography().labelLarge.copy(fontSize = 16.sp)
        ),
        content = content
    )
}
