package com.example.proyecto.data

import com.squareup.moshi.Json

data class AnuncioDto(
    val id: Int,
    val titulo: String,
    val contenido: String,
    @Json(name = "fecha_creacion") val fechaCreacion: String,
    @Json(name = "autor_nombre") val autorNombre: String?
)