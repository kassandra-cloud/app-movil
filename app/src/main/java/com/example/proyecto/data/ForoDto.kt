package com.example.proyecto.data

import com.google.gson.annotations.SerializedName

// -------------------- Adjuntos --------------------
data class AdjuntoDto(
    val id: Int,
    val url: String,
    @SerializedName("tipo_archivo") val tipoArchivo: String,
    val nombre: String? = null
)

// -------------------- Comentarios --------------------
data class ComentarioDto(
    val id: Int,
    @SerializedName("autor_username") val autor: String,   // viene así del backend
    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    val parent: Int? = null
)

/** Body para crear comentario o respuesta (coincide con tu API) */
data class ComentarioCrearRequest(
    val texto: String,
    val parent: Int? = null
)

// -------------------- Publicaciones --------------------
data class PublicacionDto(
    val id: Int,
    val autor: String,
    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    val adjuntos: List<AdjuntoDto> = emptyList(),
    val comentarios: List<ComentarioDto> = emptyList()
)
