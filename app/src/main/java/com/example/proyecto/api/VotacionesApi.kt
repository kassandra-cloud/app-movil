package com.example.proyecto.api

import com.example.proyecto.data.votaciones.*
import retrofit2.http.*

interface VotacionesApi {
    @GET("votaciones/api/v1/abiertas/")
    suspend fun listarAbiertas(
        @Header("Authorization") auth: String
    ): List<VotacionDto>

    @POST("votaciones/api/v1/{pk}/votar/")
    suspend fun votar(
        @Path("pk") votacionId: Int,
        @Header("Authorization") auth: String,
        @Body body: VotarRequest
    ): VotarResponse

    @GET("votaciones/api/v1/{pk}/resultados/")
    suspend fun resultados(
        @Path("pk") votacionId: Int,
        @Header("Authorization") auth: String
    ): ResultadoDto
}
