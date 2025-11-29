package com.example.proyecto.api

import com.example.proyecto.data.reuniones.ActaDto
import retrofit2.Response // <-- Importar Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST // <-- Importar POST
import retrofit2.http.Headers // <-- Importar Headers

interface ActasApi {

    // 👈 Esta ruta coincide con tu Django: /reuniones/api/actas/<pk>/
    @GET("reuniones/api/actas/{id}/")
    suspend fun getActaDetalle(@Path("id") id: Int): ActaDto

    // 🆕 NUEVA FUNCIÓN PARA REGISTRAR LA CONSULTA
    @Headers("Content-Type: application/json") // No enviamos cuerpo, pero es buena práctica
    @POST("reuniones/api/actas/{id}/consultar/")
    // Retornamos Response<Unit> para manejar el status HTTP 204 No Content
    suspend fun registrarConsultaActa(@Path("id") actaId: Int): Response<Unit>
}