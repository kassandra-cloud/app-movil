package com.example.proyecto.api

import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.data.votaciones.VotarRequest
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody // Import necesario para una respuesta simple

interface VotacionesApi {

    @GET("votaciones/api/v1/abiertas/")
    suspend fun votacionesAbiertasV1(@Header("Authorization") auth: String): Response<List<VotacionDto>>

    // Solicitar código antes de votar (para MFA)
    @POST("votaciones/api/v1/solicitar-codigo/")
    suspend fun solicitarCodigoVotacion(
        @Header("Authorization") auth: String
    ): Response<Map<String, Any>> // Respuesta simple {"ok": true}

    // Envía el voto, incluyendo el nuevo campo 'codigo' en VotarRequest
    @POST("votaciones/api/v1/{id}/votar/")
    suspend fun votarV1(
        @Path("id") votacionId: Int,
        @Body body: VotarRequest,
        @Header("Authorization") auth: String
    ): Response<Unit>

    @GET("votaciones/api/v1/{id}/resultados/")
    suspend fun resultadosVotacionV1(
        @Path("id") votacionId: Int,
        @Header("Authorization") auth: String
    ): Response<ResultadoVotacionDto>
}