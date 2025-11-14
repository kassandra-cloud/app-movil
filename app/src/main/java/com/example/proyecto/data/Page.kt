package com.example.proyecto.data

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.JsonClass

// 💡 CORREGIDO: Añadir la anotación para la generación de código
// Es importante que sea genérica para que funcione con cualquier DTO (T)
@JsonClass(generateAdapter = true)
data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)