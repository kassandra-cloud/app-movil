package com.example.proyecto.data.reuniones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReunionDto(
    val id: Int,
    val titulo: String,

    @Json(name = "autor")
    val autor: Int,

    @Json(name = "estado")
    val estado: String,

    @Json(name = "fecha_inicio")
    val fechaInicio: String,

    @Json(name = "tipo_reunion")
    val tipoReunion: String,

    @Json(name = "asistentes_count")
    val asistentesCount: Int,

    @Json(name = "tabla")
    val tabla: String? = null,

    @Json(name = "creada_el")
    val creadaEl: String? = null,

    @Json(name = "acta_contenido")
    val actaContenido: String? = null,

    @Json(name = "acta_estado_transcripcion")
    val actaEstadoTranscripcion: String? = null
)
