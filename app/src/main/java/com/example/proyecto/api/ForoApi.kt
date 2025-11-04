package com.example.proyecto.api

import com.example.proyecto.data.*
import retrofit2.http.*

interface ForoApi {

    // Lista publicaciones (ARRAY, sin paginar)
    @GET("foro/api/publicaciones/")
    suspend fun listar(
        @Header("Authorization") auth: String
    ): List<PublicacionDto>

    // Comentarios (plano por defecto). Si quieres árbol, pasa tree=1.
    @GET("foro/api/publicaciones/{id}/comentarios/")
    suspend fun comentarios(
        @Header("Authorization") auth: String,
        @Path("id") publicacionId: Int,
        @Query("tree") tree: Int? = null
    ): List<ComentarioDto>

    // Crear comentario o respuesta (parent opcional)
    @POST("foro/api/publicaciones/{id}/comentarios/")
    suspend fun comentar(
        @Header("Authorization") auth: String,
        @Path("id") publicacionId: Int,
        @Body body: ComentarioCreateRequest
    ): ComentarioDto
}
