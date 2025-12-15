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

// MODIFICADO: Ahora incluye el 'codigo' de verificación
@JsonClass(generateAdapter = true)
data class VotarRequest(
    @Json(name = "opcion_id") val opcionId: Int,
    @Json(name = "codigo") val codigo: String
)

// 🔥 CORRECCIÓN CLAVE: Estructura aplanada para coincidir con el JSON de resultados de la API.
// Se eliminó la dependencia a VotacionHeader.
@JsonClass(generateAdapter = true)
data class ResultadoVotacionDto(
    @Json(name = "votacion_id") val votacionId: Int, // Mapea directamente al campo 'votacion_id'
    val pregunta: String, // Mapea directamente al campo 'pregunta'
    @Json(name = "total_votos") val totalVotos: Int,
    val opciones: List<ResultadoOpcion>
)

// VotacionHeader ha sido eliminado ya que sus campos se movieron a ResultadoVotacionDto.
// @JsonClass(generateAdapter = true)
// data class VotacionHeader(val id: Int, val pregunta: String)

@JsonClass(generateAdapter = true)
data class ResultadoOpcion(
    // El ID se hizo opcional (Int?) porque el JSON de resultados no siempre lo incluye.
    // Si la API lo envía como "opcion_id" y no como "id" se mantiene el alias original.
    @Json(name = "opcion_id") val opcionId: Int?,
    val texto: String,
    val votos: Int
)