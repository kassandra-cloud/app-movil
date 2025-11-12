package com.example.proyecto.data.reuniones


data class ActaDto(
    val reunion: Int,
    val contenido: String,
    val aprobada: Boolean,
    val reunion_titulo: String,
    val reunion_fecha: String,
    val reunion_tipo: String,
    val resumen: String?
)



