package com.example.proyecto.api

import com.example.proyecto.data.*
import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.data.votaciones.VotarRequest // Usamos el nuevo DTO
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.data.recursos.SolicitudDto
import retrofit2.Response
import retrofit2.http.*
import com.squareup.moshi.Json
import okhttp3.ResponseBody

data class FcmTokenRequest(@Json(name = "fcm_token") val fcm_token: String)

interface ApiService {

    // ... (Otros endpoints de Login, FCM, etc. se mantienen igual) ...
    @POST("fcm/register/")
    suspend fun registrarFCMToken(@Header("Authorization") auth: String, @Body body: Map<String, String>): Response<ResponseBody>

    @POST("usuarios/api/login/")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    // ================= VOTACIONES (Actualizado) =================

    @GET("votaciones/api/v1/abiertas/")
    suspend fun votacionesAbiertasV1(@Header("Authorization") auth: String): Response<List<VotacionDto>>

    // Solicitar código antes de votar
    @POST("votaciones/api/v1/solicitar-codigo/")
    suspend fun solicitarCodigoVotacion(
        @Header("Authorization") auth: String
    ): Response<Map<String, Any>> // Respuesta simple {"ok": true}

    //Recibe VotarRequest (que ahora tiene el código)
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

    // ... (Resto de endpoints de Recursos y Reuniones siguen igual) ...
    @GET("recursos/api/v1/solicitudes/")
    suspend fun misSolicitudes(@Query("mine") mine: Boolean = true, @Query("estado") estado: String? = null, @Header("Authorization") auth: String): Response<Page<SolicitudDto>>

    @GET("reuniones/api/reuniones/")
    suspend fun listarReuniones(@Query("estado") estado: String? = null, @Query("ordering") ordering: String? = "-fecha", @Query("page") page: Int? = 1, @Query("page_size") pageSize: Int? = 20): Page<ReunionDto>
    //Endpoint para obtener anuncios
    @GET("api/anuncios/") suspend fun obtenerAnuncios(@Header("Authorization") auth: String): Response<List<AnuncioDto>>
    @DELETE("foro/api/v1/comentarios/{id}/")
    suspend fun eliminarComentario(
        @Path("id") comentarioId: Int,
        @Header("Authorization") auth: String
    ): Response<Unit>

}