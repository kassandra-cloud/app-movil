package com.example.proyecto.data.reuniones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActaDto(
    @Json(name = "reunion")
    val reunion: Int,

    @Json(name = "contenido")
    val contenido: String,

    @Json(name = "aprobada")
    val aprobada: Boolean,

    @Json(name = "reunion_titulo")
    val reunionTitulo: String,

    @Json(name = "reunion_fecha")
    val reunionFecha: String,

    @Json(name = "reunion_tipo")
    val reunionTipo: String,

    // 👇 CAMBIO IMPORTANTE: AHORA ES OPCIONAL
    @Json(name = "autor_username")
    val autorUsername: String? = null,

    // 👇 TAMBIÉN OPCIONAL (por si el backend no siempre lo manda)
    @Json(name = "resumen")
    val resumen: String? = null,
)
