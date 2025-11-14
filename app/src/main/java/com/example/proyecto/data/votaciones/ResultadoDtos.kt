package com.example.proyecto.data.votaciones

// 💡 NECESARIO: Importar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpcionResultadoDto(
    val id: Int,
    val texto: String,
    val votos: Int
)

@JsonClass(generateAdapter = true)
data class ResultadoVotacionDto(
    // 💡 CORREGIDO: Mapeo de snake_case a camelCase
    @Json(name = "votacion_id") val votacionId: Int,
    @Json(name = "total_votos") val totalVotos: Int,
    val opciones: List<OpcionResultadoDto>
)