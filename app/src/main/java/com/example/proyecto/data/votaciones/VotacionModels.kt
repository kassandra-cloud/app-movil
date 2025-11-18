package com.example.proyecto.data.votaciones

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpcionDto(val id: Int, val texto: String)

@JsonClass(generateAdapter = true)
data class VotacionDto(
    val id: Int,
    val pregunta: String,
    @Json(name = "fecha_cierre") val fechaCierre: String,
    val activa: Boolean,
    @Json(name = "esta_abierta") val estaAbierta: Boolean,
    val opciones: List<OpcionDto>,
    @Json(name = "ya_vote") val yaVote: Boolean,
    @Json(name = "opcion_votada_id") val opcionVotadaId: Int?
)

// 🔥 MODIFICADO: Ahora incluye el 'codigo' de verificación
@JsonClass(generateAdapter = true)
data class VotarRequest(
    @Json(name = "opcion_id") val opcionId: Int,
    @Json(name = "codigo") val codigo: String // <--- Nuevo campo obligatorio
)

@JsonClass(generateAdapter = true)
data class ResultadoVotacionDto(
    val votacion: VotacionHeader,
    @Json(name = "total_votos") val totalVotos: Int,
    val opciones: List<ResultadoOpcion>
)

@JsonClass(generateAdapter = true)
data class VotacionHeader(val id: Int, val pregunta: String)

@JsonClass(generateAdapter = true)
data class ResultadoOpcion(
    @Json(name = "opcion_id") val opcionId: Int,
    val texto: String,
    val votos: Int
)