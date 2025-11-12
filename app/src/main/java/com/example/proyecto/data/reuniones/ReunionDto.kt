package com.example.proyecto.data.reuniones

data class ReunionDto(
    val id: Int,
    val fecha: String,
    val tipo: String,
    val titulo: String,
    val tabla: String?,
    val creada_el: String,
    val asistentes_count: Int
)
