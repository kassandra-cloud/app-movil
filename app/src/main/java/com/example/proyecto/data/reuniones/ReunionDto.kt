// app/src/main/java/com/example/proyecto/data/reuniones/ReunionDto.kt (VERSION CORREGIDA)
package com.example.proyecto.data.reuniones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReunionDto(
    val id: Int,
    val titulo: String,

    // El serializer de Django ahora envía 'autor' como Int (ID)
    val autor: Int,

    // Mismo nombre que en Serializer (String)
    val estado: String,

    // Mapeo de snake_case para los campos de fecha y tipo
    @Json(name = "fecha_inicio") val fechaInicio: String,
    @Json(name = "fecha_fin") val fechaFin: String,
    @Json(name = "tipo_reunion") val tipoReunion: String,

    // Campos que el Serializer sí envía
    @Json(name = "asistentes_count") val asistentesCount: Int,

    // Campos que Django no envía o que no deberían estar
    val tabla: String? = null // Mantener como opcional si se usó en el DTO anterior
)