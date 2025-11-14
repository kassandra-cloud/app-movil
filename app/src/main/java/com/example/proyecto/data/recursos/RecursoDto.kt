package com.example.proyecto.data.recursos

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecursoDto(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    // El campo 'disponible' indica si el recurso está libre en general (global)
    val disponible: Boolean,

    // ✅ CAMPO NUEVO: Indica si el usuario actual tiene una solicitud activa (pendiente o aprobada)
    @Json(name = "solicitud_activa_usuario")
    val solicitudActivaUsuario: Boolean = false,
    @Json(name = "estado_ultima_solicitud")
    val estadoUltimaSolicitud: String? = null
)