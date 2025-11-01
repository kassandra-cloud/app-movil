package com.example.proyecto.ui.theme

import androidx.compose.ui.graphics.Brush // <-- IMPORT NECESARIO
import androidx.compose.ui.graphics.Color

// Pasteles estilo mockup
val Lavender = Color(0xFFF1E9FF)
val Lilac    = Color(0xFFD9C8FF)
val Peach    = Color(0xFFFFE7F0)
val Sky      = Color(0xFFE7F3FF)
val Mint     = Color(0xFFEAFBF4)

val Primary  = Color(0xFF7B61FF)   // morado principal
val Secondary= Color(0xFF79E3D8)   // aqua
val Tertiary = Color(0xFFFFB3C7)   // rosado

// Texto
val TextPrimary   = Color(0xFF1E1E28)
val TextSecondary = Color(0xFF61667A)
val CardBg        = Color(0xFFFFFFFF)
val ChipBg        = Color(0xFFF4F2FF)

// --- 🎨 TU NUEVA PALETA DE COLORES ---
val tuColorPrincipal = Color(0xFF6C63FF) // Púrpura (de VotacionesScreen)
val tuColorSecundario = Color(0xFF8EC5FC) // Celeste (de VotacionesScreen)
val tuColorFondo = Color(0xFFF8F9FA) // Gris muy claro (para contenido)

// Gradiente de fondo para pantallas principales (Login, Menú)
val tuGradienteFondo = Brush.linearGradient(
    colors = listOf(
        Color(0xFFE0C3FC), // Lila
        Color(0xFF8EC5FC)  // Celeste
    )
)