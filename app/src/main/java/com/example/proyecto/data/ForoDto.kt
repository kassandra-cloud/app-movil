package com.example.proyecto.data

import com.google.gson.annotations.SerializedName

// -------------------- Adjuntos --------------------
// Backend: ArchivoAdjuntoSerializer => fields: id, archivo, tipo_archivo, url
data class AdjuntoDto(
    val id: Int,
    val archivo: String?, // ruta relativa en el backend (/media/...)
    @SerializedName("tipo_archivo") val tipoArchivo: String,
    val url: String       // URL absoluta que armamos en el serializer
)

// -------------------- Comentarios --------------------
// Backend: ComentarioSerializer => id, autor_username, contenido, fecha_creacion, parent
data class ComentarioDto(
    val id: Int,
    @SerializedName("autor_username") val autor: String,
    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    val parent: Int? = null
)

// Para crear comentario (POST)
data class ComentarioCrearRequest(
    val texto: String,
    val parent: Int? = null
)

// -------------------- Publicaciones --------------------
// Backend: PublicacionSerializer => id, autor, contenido, fecha_creacion, adjuntos, comentarios
data class PublicacionDto(
    val id: Int,

    // OJO: el backend envía "autor" como PK (int del usuario)
    // Si después quieres mostrar el username, podemos agregar "autor_username" en el serializer.
    val autor: Int,

    val contenido: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String,

    val adjuntos: List<AdjuntoDto> = emptyList(),
    val comentarios: List<ComentarioDto> = emptyList()
)
