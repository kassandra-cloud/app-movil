package com.example.proyecto.data.votaciones

data class OpcionDto(val id: Int, val texto: String)

data class VotacionDto(
    val id: Int,
    val pregunta: String,
    val fecha_cierre: String,
    val activa: Boolean,
    val esta_abierta: Boolean,
    val opciones: List<OpcionDto>,
    val ya_vote: Boolean,
    val opcion_votada_id: Int?
)

data class VotarRequest(val opcion_id: Int)
data class VotarResponse(val ok: Boolean, val mensaje: String)

data class ResultadoDto(
    val votacion: VotacionHeader,
    val total_votos: Int,
    val opciones: List<ResultadoOpcion>
)
data class VotacionHeader(val id: Int, val pregunta: String)
data class ResultadoOpcion(val opcion_id: Int, val texto: String, val votos: Int)
