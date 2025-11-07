package com.example.proyecto.data

import com.google.gson.annotations.SerializedName

data class TallerDto(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    @SerializedName("cupos_totales") val cuposTotales: Int,
    @SerializedName("inscritos_count") val inscritosCount: Int,
    @SerializedName("cupos_disponibles") val cuposDisponibles: Int,
    @SerializedName("esta_inscrito") val estaInscrito: Boolean
)
