package com.example.proyecto.data.recursos

// 💡 NECESARIO: Importar la anotación de Moshi
import com.squareup.moshi.JsonClass

// 💡 CORREGIDO: Añadir la anotación para la generación de código
@JsonClass(generateAdapter = true)
data class RecursoDto(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    // Este campo se actualiza en el backend (ver Solución 1)
    val disponible: Boolean
)