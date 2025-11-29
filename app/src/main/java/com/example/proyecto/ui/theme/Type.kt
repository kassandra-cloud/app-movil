package com.example.proyecto.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Función que genera la tipografía según una escala
fun getTypography(scale: Float): Typography {
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp * scale,
            lineHeight = 24.sp * scale,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp * scale,
            lineHeight = 28.sp * scale,
            letterSpacing = 0.sp
        )
        // Puedes agregar más estilos aquí (labelSmall, headlineMedium, etc.)
        // multiplicando siempre el fontSize y lineHeight por 'scale'.
    )
}

// Mantenemos la variable original por compatibilidad si algo falla, pero usando escala 1.0
val Typography = getTypography(1.0f)