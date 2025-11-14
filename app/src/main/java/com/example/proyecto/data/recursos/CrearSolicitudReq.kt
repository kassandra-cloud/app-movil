package com.example.proyecto.data.recursos

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Data Class para enviar la solicitud de creación de reserva al backend
@JsonClass(generateAdapter = true)
data class CrearSolicitudReq(
    // Campo 'recurso' es la clave foránea (ID del recurso)
    val recurso: Int,

    // Mapeamos los campos snake_case de Django (fecha_inicio) a camelCase de Kotlin (fechaInicio)
    @Json(name = "fecha_inicio")
    val fechaInicio: String,

    @Json(name = "fecha_fin")
    val fechaFin: String,

    // El motivo es opcional (blank=True en Django)
    val motivo: String?
)