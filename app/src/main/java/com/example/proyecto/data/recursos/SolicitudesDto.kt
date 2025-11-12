// app/src/main/java/com/example/proyecto/data/recursos/SolicitudesDto.kt
package com.example.proyecto.data.recursos

data class SolicitudDto(
    val id: Int,
    val recurso: Int,
    val recurso_nombre: String?,
    val fecha_inicio: String,  // "YYYY-MM-DD"
    val fecha_fin: String,     // "YYYY-MM-DD"
    val motivo: String,
    val estado: String,        // "PENDIENTE" | "APROBADA" | "RECHAZADA"
    val creado_el: String
)

// Si ya usas Page<T> genérico, reutilízalo; si no, crea uno simple:
data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)
