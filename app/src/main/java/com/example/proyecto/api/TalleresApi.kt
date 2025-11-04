package com.example.proyecto.api

import com.example.proyecto.data.TallerDto
import retrofit2.http.*

interface TalleresApi {
    @GET("talleres/api/talleres/")
    suspend fun listar(): List<TallerDto>

    @POST("talleres/api/talleres/{id}/inscribir/")
    suspend fun inscribir(
        @Path("id") id: Int,
        @Header("Authorization") auth: String   // "Token <clave>"
    ): TallerDto

    @POST("talleres/api/talleres/{id}/desinscribir/")
    suspend fun desinscribir(
        @Path("id") id: Int,
        @Header("Authorization") auth: String
    ): TallerDto
}
