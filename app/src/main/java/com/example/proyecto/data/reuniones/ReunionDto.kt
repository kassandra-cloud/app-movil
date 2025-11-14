package com.example.proyecto.data.reuniones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReunionDto(
    val id: Int,
    val titulo: String,

    // Alinamiento con el Serializer de Django
    @Json(name = "autor") val autor: Int,
    @Json(name = "estado") val estado: String,


    @Json(name = "fecha_inicio") val fechaInicio: String, // <-- Vuelve a ser fecha_inicio
@Json(name = "tipo_reunion") val tipoReunion: String, // <-- Vuelve a ser tipo_reunion

@Json(name = "asistentes_count") val asistentesCount: Int,

// Estos campos también están en el Serializer
@Json(name = "tabla") val tabla: String? = null,
@Json(name = "creada_el") val creadaEl: String? = null
)