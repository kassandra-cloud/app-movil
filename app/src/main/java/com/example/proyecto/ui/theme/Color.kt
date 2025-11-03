package com.example.proyecto.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Objeto centralizado para gestionar la paleta de colores de la aplicación.
 * Simplifica la paleta al esquema principal Cian/Menta y colores de utilidad.
 */
object AppColors {

    // 🔑 PALETA PRIMARIA (Cian/Menta)
    val Principal = Color(0xFF33BACC) // Cian
    val Secundario = Color(0xFF66D9CE) // Menta

    // COLORES DE UTILIDAD

    // Texto y Fondos
    val TextPrimary = Color(0xFF1E1E28) // Texto oscuro principal
    val GrisOscuroTexto = Color(0xFF616161) // Texto gris secundario
    val CardBg = Color(0xFFFFFFFF) // Fondo de tarjetas (Blanco)
    val GrisClaroFondo = Color(0xFFEEEEEE) // Gris muy claro (Para chips, como "No Aprobada")

    // Acentos Específicos (Dashboard y Botón Salir)
    val BotonSalir = Color(0xFFF06292) // Rosa/Coral

    // Colores de Iconos del Dashboard (Basado en tu diseño)
    val IconoReuniones = Color(0xFF42A5F5) // Azul
    val IconoForo = Color(0xFF66BB6A) // Verde
    val IconoVotacion = Color(0xFFFFB300) // Naranja/Ámbar
    val IconoTalleres = Color(0xFFAA00FF) // Púrpura

    // GRADIENTE UNIFORME
    // Este degradado se usa en fondos y encabezados principales.
    val GradientePrincipal: Brush
        @Composable
        get() = Brush.verticalGradient(listOf(Principal, Secundario))
}