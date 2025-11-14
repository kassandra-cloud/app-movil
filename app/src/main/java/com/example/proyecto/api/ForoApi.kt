package com.example.proyecto.api

import com.example.proyecto.data.AdjuntoDto
import com.example.proyecto.data.ComentarioCrearRequest
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import okhttp3.MultipartBody
import retrofit2.http.*

interface ForoApi {

    // Publicaciones del foro
    @GET("foro/api/v1/publicaciones/")
    suspend fun listar(): List<PublicacionDto>

    // Comentarios de una publicación
    @GET("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentarios(
        @Path("id") publicacionId: Int
    ): List<ComentarioDto>

    // Crear comentario
    @POST("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentar(
        @Path("id") publicacionId: Int,
        @Body body: ComentarioCrearRequest
    ): ComentarioDto

    // 💡 NUEVO: Endpoint para subir archivos (audio/imagen) como mensajes
    @Multipart // Indica que la petición es de tipo Multipart
    @POST("foro/api/v1/publicaciones/{id}/adjuntos/")
    suspend fun subirAdjunto(
        @Path("id") publicacionId: Int,
        // El archivo binario (audio o imagen)
        @Part archivo: MultipartBody.Part,
        // Campo adicional para indicar al backend que es un "mensaje"
        @Part esMensaje: String // Se enviará "es_mensaje: True"
    ): AdjuntoDto // Retorna el adjunto creado
}