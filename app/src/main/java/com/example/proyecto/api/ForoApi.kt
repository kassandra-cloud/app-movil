package com.example.proyecto.api

import com.example.proyecto.data.AdjuntoDto
import com.example.proyecto.data.ComentarioCrearRequest
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ForoApi {

    @GET("foro/api/v1/publicaciones/")
    suspend fun listar(): List<PublicacionDto>

    @GET("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentarios(
        @Path("id") publicacionId: Int
    ): List<ComentarioDto>

    @POST("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentar(
        @Path("id") publicacionId: Int,
        @Body body: ComentarioCrearRequest
    ): ComentarioDto

    @Multipart
    @POST("foro/api/v1/publicaciones/{id}/adjuntos/")
    suspend fun subirAdjunto(
        @Path("id") publicacionId: Int,
        @Part archivo: MultipartBody.Part,
        @Part esMensaje: String
    ): AdjuntoDto

    @DELETE("foro/api/v1/comentarios/{id}/")
    suspend fun eliminarComentario(
        @Path("id") comentarioId: Int
    )

    @POST("foro/api/v1/comentarios/{id}/like/")
    suspend fun toggleLike(
        @Path("id") id: Int
    ): Response<LikeResponse>
}

@JsonClass(generateAdapter = true)
data class LikeResponse(
    val liked: Boolean,
    @Json(name = "total_likes") val totalLikes: Int
)