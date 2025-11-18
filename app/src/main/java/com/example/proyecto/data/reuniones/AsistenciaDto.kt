package com.example.proyecto.data.reuniones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AsistenciaDto(
    @Json(name = "id")
    val id: Int,

    @Json(name = "reunion")
    val reunion: Int,

    // Ajusta estos nombres según tu serializer de Django
    @Json(name = "presente")
    val presente: Boolean? = null,

    @Json(name = "estado")
    val estado: String? = null
)
