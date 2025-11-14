// app/src/main/java/com/example/proyecto/data/recursos/SolicitudesDto.kt
package com.example.proyecto.data.recursos

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SolicitudDto(
    val id: Int,
    val recurso: Int,

    // 💡 CORREGIDO: Mapeo de snake_case a camelCase (recurso_nombre -> recursoNombre)
    @Json(name = "recurso_nombre") val recursoNombre: String?,
    @Json(name = "fecha_inicio") val fechaInicio: String,  // "YYYY-MM-DD"
    @Json(name = "fecha_fin") val fechaFin: String,     // "YYYY-MM-DD"
    val motivo: String,
    val estado: String,        // "PENDIENTE" | "APROBADA" | "RECHAZADA"
    @Json(name = "creado_el") val creadoEl: String
)

// Si ya usas Page<T> genérico, reutilízalo; si no, crea uno simple:
@JsonClass(generateAdapter = true)
data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)