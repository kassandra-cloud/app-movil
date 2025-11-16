package com.example.proyecto.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Objeto centralizado para gestionar la paleta de colores de la aplicación.
 * Simplifica la paleta al esquema principal de Azules unificados.
 */
object AppColors {

    // 🔑 PALETA PRIMARIA UNIFICADA (Todos los azules usan estos valores)
    val Principal = Color(0xFF42A5F5) // Azul Vibrante
    val Secundario = Color(0xFF1E88E5) // Azul Oscuro

    // COLORES DE UTILIDAD

    // Texto y Fondos
    val TextPrimary = Color(0xFF212121) // Texto oscuro principal
    val GrisOscuroTexto = Color(0xFF616161) // Texto gris secundario
    val CardBg = Color(0xFFFFFFFF) // Fondo de tarjetas (Blanco)
    val GrisClaroFondo = Color(0xFFEEEEEE) // Gris muy claro (Para chips, como "No Aprobada")

    // Acentos Específicos (Dashboard y Botón Salir)
    val BotonSalir = Color(0xFFEF5350) // Rosa/Coral (Usando el color de salir de MainActivity)

    // Colores de Iconos del Dashboard (Usando el nuevo azul principal)
    val IconoReuniones = Principal
    val IconoForo = Color(0xFF66BB6A)
    val IconoVotacion = Color(0xFFFFB300)
    val IconoTalleres = Color(0xFFAA00FF)

    // GRADIENTE UNIFORME - ¡CORREGIDO! Ya no es @Composable.
    val GradientePrincipal: Brush = Brush.verticalGradient(listOf(Principal, Secundario))
}