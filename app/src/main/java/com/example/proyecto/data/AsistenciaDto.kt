package com.example.proyecto.data

data class AsistenciaDto(
    val id: Int,
    val reunion: Int,
    val nombre_usuario: String?,
    val nombre_completo: String?,
    val rut: String?,
    val presente: Boolean
)
