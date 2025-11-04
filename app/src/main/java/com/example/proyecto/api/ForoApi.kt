package com.example.proyecto.api

import com.example.proyecto.data.ComentarioCrearRequest
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import retrofit2.http.*

interface ForoApi {

    // Publicaciones del foro
    @GET("foro/api/v1/publicaciones/")
    suspend fun listar(
        @Header("Authorization") auth: String
    ): List<PublicacionDto>

    // Comentarios de una publicación
    @GET("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentarios(
        @Header("Authorization") auth: String,
        @Path("id") publicacionId: Int
    ): List<ComentarioDto>

    // Crear comentario (AJUSTA el path si en tu backend es /crear/)
    @POST("foro/api/v1/publicaciones/{id}/comentarios/")
    suspend fun comentar(
        @Header("Authorization") auth: String,
        @Path("id") publicacionId: Int,
        @Body body: ComentarioCrearRequest
    ): ComentarioDto
}
