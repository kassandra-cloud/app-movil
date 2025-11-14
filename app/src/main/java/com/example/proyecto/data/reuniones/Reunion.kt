package com.example.proyecto.data.reuniones

import com.squareup.moshi.JsonClass // Añadido
import java.time.LocalDateTime

@JsonClass(generateAdapter = true) // Añadido para Moshi
data class Reunion(
    val id: Int,
    val titulo: String,
    val descripcion: String? = null,
    val inicio: LocalDateTime,
    val fin: LocalDateTime? = null
)
// NOTA: LocalDateTime necesita un adaptador personalizado de Moshi,
// que asumimos está configurado en tu ApiClient.