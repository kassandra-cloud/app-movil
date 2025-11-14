package com.example.proyecto.data.reuniones

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 💡 CORREGIDO: Añadir la anotación para la generación de código
@JsonClass(generateAdapter = true)
data class AsistenciaDto(
    val id: Int,
    val reunion: Int,

    // 💡 CORREGIDO: Mapeo de snake_case a camelCase
    @Json(name = "nombre_usuario") val nombreUsuario: String?,
    @Json(name = "nombre_completo") val nombreCompleto: String?,

    val rut: String?,
    val presente: Boolean
)