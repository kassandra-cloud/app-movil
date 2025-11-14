package com.example.proyecto.data.reuniones

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 💡 CORREGIDO: Añadir la anotación para la generación de código
@JsonClass(generateAdapter = true)
data class ActaDto(
    val reunion: Int,
    val contenido: String,
    val aprobada: Boolean,

    // 💡 CORREGIDO: Mapeo de snake_case a camelCase
    @Json(name = "reunion_titulo") val reunionTitulo: String,
    @Json(name = "reunion_fecha") val reunionFecha: String,
    @Json(name = "reunion_tipo") val reunionTipo: String,

    val resumen: String?
)