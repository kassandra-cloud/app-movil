package com.example.proyecto.data

import com.google.gson.annotations.SerializedName

data class AdjuntoDto(
    val id: Int,
    val url: String,
    @SerializedName("tipo_archivo") val tipoArchivo: String,
    val nombre: String? = null
)

data class ComentarioDto(
    val id: Int,
    @SerializedName("autor_username") val autor: String,  // ← mapea al nombre que muestra el backend
    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    val parent: Int?
)

/** Request para crear comentario o respuesta */
data class ComentarioCreateRequest(
    val contenido: String,
    val parent: Int? = null         // ← ¡este es el campo que faltaba!
)

data class PublicacionDto(
    val id: Int,
    val autor: String,
    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    val adjuntos: List<AdjuntoDto> = emptyList(),
    val comentarios: List<ComentarioDto> = emptyList()
)