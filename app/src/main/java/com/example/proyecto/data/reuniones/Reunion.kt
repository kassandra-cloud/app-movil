package com.example.proyecto.data.reuniones

import java.time.LocalDateTime

data class Reunion(
    val id: Int,
    val titulo: String,
    val descripcion: String? = null,
    val inicio: LocalDateTime,
    val fin: LocalDateTime? = null
)