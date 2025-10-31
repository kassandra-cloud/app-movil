package com.example.proyecto.api


import com.example.proyecto.data.AsistenciaDto
import com.example.proyecto.data.ActaDto
import com.example.proyecto.data.Page
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

interface ReunionesApi {
    @GET("reuniones/api/actas/")
    suspend fun listarActas(
        @Query("reunion") reunionId: Int? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = "-reunion__fecha",
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = 20
    ): Page<ActaDto>

    @GET("reuniones/api/actas/{id}/")
    suspend fun detalleActa(@Path("id") reunionId: Int): ActaDto


    @GET("reuniones/api/asistencias/")
    suspend fun listarAsistencias(
        @Query("reunion") reunionId: Int,
        @Query("page_size") pageSize: Int? = 100
    ): Response<List<AsistenciaDto>>
}
