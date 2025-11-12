package com.example.proyecto.data.recursos


data class CrearSolicitudReq(
    val recurso: Int,
    val fecha_inicio: String,
    val fecha_fin: String,
    val motivo: String?
)
