// kassandra-cloud/app-movil/.../data/TallerDto.kt

package com.example.proyecto.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TallerDto(
    val id: Int,
    val nombre: String,
    val descripcion: String,

    @Json(name = "fecha_inicio") val fechaInicio: String?,
    @Json(name = "fecha_termino") val fechaTermino: String?,

    @Json(name = "cupos_totales") val cuposTotales: Int,
    @Json(name = "inscritos_count") val inscritosCount: Int,
    @Json(name = "cupos_disponibles") val cuposDisponibles: Int,

    // 🔑 CORRECCIÓN: Agregar valor por defecto 'false'
    @Json(name = "esta_inscrito") val estaInscrito: Boolean = false
)