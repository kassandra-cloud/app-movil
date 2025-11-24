package com.example.proyecto.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdjuntoDto(
    val id: Int = 0, // Valor por defecto para ID
    val archivo: String? = null,

    @Json(name = "tipo_archivo") val tipoArchivo: String? = "archivo",
    val url: String? = null,

    @Json(name = "es_mensaje") val esMensaje: Boolean = false,
    @Json(name = "fecha_creacion") val fechaCreacion: String? = null,

    val autor: String? = "Desconocido",
    val descripcion: String? = null,

    @Json(name = "total_likes") val totalLikes: Int = 0,
    @Json(name = "me_gusta_usuario") val meGustaUsuario: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ComentarioDto(
    val id: Int = 0,
    @Json(name = "autor_username") val autor: String? = "Anónimo",
    val contenido: String? = "",
    @Json(name = "fecha_creacion") val fechaCreacion: String? = null,
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
    val id: Int = 0,
    val autor: String? = "Anónimo",
    val contenido: String? = "",
    @Json(name = "fecha_creacion") val fechaCreacion: String? = null,

    // 🛡️ BLINDAJE EXTRA: La lista puede ser nula, la convertimos a vacía por defecto
    val adjuntos: List<AdjuntoDto>? = emptyList(),
    val comentarios: List<ComentarioDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class LikeResponse(
    val liked: Boolean,
    @Json(name = "total_likes") val totalLikes: Int
)