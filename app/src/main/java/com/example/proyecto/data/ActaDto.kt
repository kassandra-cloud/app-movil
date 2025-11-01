package com.example.proyecto.data


data class ActaDto(
    val reunion: Int,
    val contenido: String,
    val aprobada: Boolean,
    val reunion_titulo: String,
    val reunion_fecha: String,
    val reunion_tipo: String,
    val resumen: String?
)

data class ReunionDto(
    val id: Int,
    val fecha: String,
    val tipo: String,
    val titulo: String,
    val tabla: String,
    val creada_el: String,
    val asistentes_count: Int
)

data class Page<T>(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T>? = null
)
