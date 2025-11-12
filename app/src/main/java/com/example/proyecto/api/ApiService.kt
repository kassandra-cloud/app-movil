package com.example.proyecto.api

import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.LoginResponse
import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import retrofit2.Response
import retrofit2.http.*
import com.example.proyecto.data.Page
import com.example.proyecto.data.reuniones.ReunionDto
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

    // Recursos / Solicitudes
    @GET("recursos/api/v1/solicitudes/")
    suspend fun misSolicitudes(
        @Query("mine") mine: Boolean = true,
        @Query("estado") estado: String? = null,
        @Header("Authorization") auth: String
    ): retrofit2.Response<Page<com.example.proyecto.data.recursos.SolicitudDto>>


    @GET("reuniones/api/reuniones/")
    suspend fun listarReuniones(
        @Query("estado") estado: String? = null,  // "programada" | "en_curso" | "realizada"
        @Query("ordering") ordering: String? = "-fecha",
        @Query("page") page: Int? = 1,
        @Query("page_size") pageSize: Int? = 20
    ): Page<ReunionDto>
}
