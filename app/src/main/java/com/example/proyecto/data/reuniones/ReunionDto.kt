package com.example.proyecto.data.reuniones

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 💡 CORREGIDO: Añadir la anotación para la generación de código
@JsonClass(generateAdapter = true)
data class ReunionDto(
    val id: Int,
    val fecha: String,
    val tipo: String,
    val titulo: String,
    val tabla: String?,

    // 💡 CORREGIDO: Mapeo de snake_case a camelCase
    @Json(name = "creada_el") val creadaEl: String,
    @Json(name = "asistentes_count") val asistentesCount: Int
)