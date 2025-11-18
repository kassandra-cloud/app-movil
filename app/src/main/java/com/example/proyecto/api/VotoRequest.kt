package com.example.proyecto.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VotoRequest(
    // Mapeamos "opcion_id" del JSON a "opcionId" en Kotlin (camelCase es mejor práctica)
    @Json(name = "opcion_id") val opcionId: Int,

    // 🔥 AGREGADO: El código de verificación es obligatorio ahora
    @Json(name = "codigo") val codigo: String
)