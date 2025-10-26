package com.example.proyecto.data

data class OpcionResultadoDto(
    val id: Int,
    val texto: String,
    val votos: Int
)

data class ResultadoVotacionDto(
    val votacion_id: Int,
    val total_votos: Int,
    val opciones: List<OpcionResultadoDto>
)
