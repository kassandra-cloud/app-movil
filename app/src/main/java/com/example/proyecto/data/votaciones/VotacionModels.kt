package com.example.proyecto.data.votaciones

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpcionDto(val id: Int, val texto: String)

@JsonClass(generateAdapter = true)
data class VotacionDto(
    val id: Int,
    val pregunta: String,
    // 💡 CORREGIDO: Mapeo de snake_case
    @Json(name = "fecha_cierre") val fechaCierre: String,
    val activa: Boolean,
    @Json(name = "esta_abierta") val estaAbierta: Boolean,
    val opciones: List<OpcionDto>,
    @Json(name = "ya_vote") val yaVote: Boolean,
    @Json(name = "opcion_votada_id") val opcionVotadaId: Int?
)

@JsonClass(generateAdapter = true)
data class VotarRequest(
    @Json(name = "opcion_id") val opcionId: Int // 💡 CORREGIDO
)

@JsonClass(generateAdapter = true)
data class VotarResponse(val ok: Boolean, val mensaje: String)

@JsonClass(generateAdapter = true)
data class ResultadoDto(
    val votacion: VotacionHeader,
    @Json(name = "total_votos") val totalVotos: Int, // 💡 CORREGIDO
    val opciones: List<ResultadoOpcion>
)

@JsonClass(generateAdapter = true)
data class VotacionHeader(val id: Int, val pregunta: String)

@JsonClass(generateAdapter = true)
data class ResultadoOpcion(
    @Json(name = "opcion_id") val opcionId: Int, // 💡 CORREGIDO
    val texto: String,
    val votos: Int
)