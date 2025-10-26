package com.example.proyecto.api

import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.LoginResponse
import com.example.proyecto.data.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("usuarios/api/login/")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("usuarios/api/test/")
    suspend fun testConnection(@Body data: Map<String, String>): Response<Map<String, Any>>

    // Votaciones (v1)
    @GET("votaciones/api/v1/abiertas/")
    suspend fun votacionesAbiertasV1(@Header("Authorization") auth: String): Response<List<VotacionDto>>

    @POST("votaciones/api/v1/{id}/votar/")
    suspend fun votarV1(
        @Path("id") votacionId: Int,
        @Body body: VotoRequest,
        @Header("Authorization") auth: String
    ): Response<Unit>

    @GET("votaciones/api/v1/{id}/resultados/")
    suspend fun resultadosVotacionV1(
        @Path("id") votacionId: Int,
        @Header("Authorization") auth: String
    ): Response<ResultadoVotacionDto>
}
