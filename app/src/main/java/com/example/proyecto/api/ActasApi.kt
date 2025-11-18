package com.example.proyecto.api

import com.example.proyecto.data.reuniones.ActaDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ActasApi {

    // 👈 Esta ruta coincide con tu Django: /reuniones/api/actas/<pk>/
    @GET("reuniones/api/actas/{id}/")
    suspend fun getActaDetalle(@Path("id") id: Int): ActaDto
}
