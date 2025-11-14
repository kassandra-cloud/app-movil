package com.example.proyecto.data

// 💡 CAMBIO: Usaremos solo las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
// Se eliminó: import com.google.gson.annotations.SerializedName

@JsonClass(generateAdapter = true) // 👈 Moshi requiere esto en TODAS las data class
data class TallerDto(
    val id: Int,
    val nombre: String,
    val descripcion: String,

    // 💡 CAMBIO: Reemplazado @SerializedName por @Json
    @Json(name = "cupos_totales") val cuposTotales: Int,
    @Json(name = "inscritos_count") val inscritosCount: Int,
    @Json(name = "cupos_disponibles") val cuposDisponibles: Int,
    @Json(name = "esta_inscrito") val estaInscrito: Boolean
)