package com.example.proyecto.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdjuntoDto(
    val id: Int,
    val archivo: String?,
    @Json(name = "tipo_archivo") val tipoArchivo: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class ComentarioDto(
    val id: Int,
    @Json(name = "autor_username") val autor: String,
    val contenido: String,
    @Json(name = "fecha_creacion") val fechaCreacion: String,
    val parent: Int? = null,
    @Json(name = "total_likes") val totalLikes: Int = 0,
    @Json(name = "me_gusta_usuario") val meGustaUsuario: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ComentarioCrearRequest(
    val texto: String,
    val parent: Int? = null
)

@JsonClass(generateAdapter = true)
data class PublicacionDto(
    val id: Int,
    val autor: Int,
    val contenido: String,
    @Json(name = "fecha_creacion") val fechaCreacion: String,
    val adjuntos: List<AdjuntoDto> = emptyList(),
    val comentarios: List<ComentarioDto> = emptyList()
)