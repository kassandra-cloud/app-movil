package com.example.proyecto.api

import com.example.proyecto.data.AdjuntoDto
import com.example.proyecto.data.ComentarioCrearRequest
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.LikeResponse
import com.example.proyecto.data.PublicacionDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ForoApi {

    @GET("foro/api/v1/publicaciones/")
    suspend fun listar(): List<PublicacionDto>

    @GET("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentarios(@Path("id") publicacionId: Int): List<ComentarioDto>

    @POST("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentar(
        @Path("id") publicacionId: Int,
        @Body body: ComentarioCrearRequest
    ): ComentarioDto

    // 🔹 SUBIR FOTO/ARCHIVO CON DESCRIPCIÓN (como mensaje)
    @Multipart
    @POST("foro/api/v1/publicaciones/{id}/adjuntos/")
    suspend fun subirAdjunto(
        @Path("id") publicacionId: Int,
        @Part archivo: MultipartBody.Part,
        @Part("esMensaje") esMensaje: RequestBody,
        @Part("descripcion") descripcion: RequestBody? = null
    ): AdjuntoDto

    // 🔹 ELIMINAR COMENTARIO / ADJUNTO
    @DELETE("foro/api/v1/comentarios/{id}/")
    suspend fun eliminarComentario(@Path("id") comentarioId: Int)

    @DELETE("foro/api/v1/adjuntos/{id}/")
    suspend fun eliminarAdjunto(@Path("id") adjuntoId: Int)

    // ✅ ELIMINAR PUBLICACIÓN (POST)
    @DELETE("foro/api/v1/publicaciones/{id}/")
    suspend fun eliminarPublicacion(@Path("id") publicacionId: Int)

    // 🔹 LIKES
    @POST("foro/api/v1/comentarios/{id}/like/")
    suspend fun toggleLike(@Path("id") id: Int): Response<LikeResponse>

    @POST("foro/api/v1/adjuntos/{id}/like/")
    suspend fun toggleLikeAdjunto(@Path("id") id: Int): Response<LikeResponse>

    // (Opcional) Enviar mensaje (texto + archivo)
    @Multipart
    @POST("foro/api/v1/publicaciones/{id}/mensaje/")
    suspend fun enviarMensaje(
        @Path("id") publicacionId: Int,
        @Part("texto") texto: RequestBody?,
        @Part archivo: MultipartBody.Part?
    ): PublicacionDto
}
