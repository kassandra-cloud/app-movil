package com.example.proyecto.data.recursos

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 💡 CORREGIDO: Añadir la anotación para la generación de código
@JsonClass(generateAdapter = true)
data class CrearSolicitudReq(
    val recurso: Int,

    // 💡 CORREGIDO: Usar @Json para mapear de snake_case a camelCase (o mantener el nombre)
    @Json(name = "fecha_inicio") val fechaInicio: String, // Cambiado a camelCase por convención Kotlin
    @Json(name = "fecha_fin") val fechaFin: String,       // Cambiado a camelCase por convención Kotlin
    val motivo: String?
)