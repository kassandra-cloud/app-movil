// app/src/main/java/com/example/proyecto/data/reuniones/ActaDto.kt (VERSION CORREGIDA)
package com.example.proyecto.data.reuniones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActaDto(
    val reunion: Int, // PK/FK a Reunion
    val contenido: String,
    val aprobada: Boolean,

    @Json(name = "reunion_titulo") val reunionTitulo: String,
    @Json(name = "reunion_fecha") val reunionFecha: String,
    @Json(name = "reunion_tipo") val reunionTipo: String,

    // 💡 CORRECCIÓN APLICADA: Para coincidir con el ActaSerializer
    @Json(name = "autor_username") val autorUsername: String,

    val resumen: String?
)