package com.example.proyecto.data

// 💡 CAMBIO: Usaremos solo las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// -------------------- Adjuntos --------------------
@JsonClass(generateAdapter = true) // 👈 CORRECCIÓN: Añadido para Moshi
data class AdjuntoDto(
    val id: Int,
    val archivo: String?, // ruta relativa en el backend (/media/...)
    @Json(name = "tipo_archivo") val tipoArchivo: String, // 👈 CORRECCIÓN: Reemplazado @SerializedName por @Json
    val url: String       // URL absoluta que armamos en el serializer
)

// -------------------- Comentarios --------------------
@JsonClass(generateAdapter = true) // 👈 CORRECCIÓN: Añadido para Moshi
data class ComentarioDto(
    val id: Int,
    @Json(name = "autor_username") val autor: String, // 👈 CORRECCIÓN: Reemplazado @SerializedName por @Json
    val contenido: String,
    @Json(name = "fecha_creacion") val fechaCreacion: String, // 👈 CORRECCIÓN: Reemplazado @SerializedName por @Json
    val parent: Int? = null
)

// Para crear comentario (POST)
@JsonClass(generateAdapter = true) // 👈 CORRECCIÓN: Añadido para Moshi
data class ComentarioCrearRequest(
    val texto: String,
    val parent: Int? = null
)

// -------------------- Publicaciones --------------------
@JsonClass(generateAdapter = true) // 👈 CORRECCIÓN: Añadido para Moshi
data class PublicacionDto(
    val id: Int,

    // OJO: Si el backend envía la PK (Int) como "autor", esto está bien.
    // Si el backend envía un string, debes usar @Json(name="autor") val autor: String
    val autor: Int,

    val contenido: String,
    @Json(name = "fecha_creacion") val fechaCreacion: String, // 👈 CORRECCIÓN: Reemplazado @SerializedName por @Json

    val adjuntos: List<AdjuntoDto> = emptyList(),
    val comentarios: List<ComentarioDto> = emptyList()
)