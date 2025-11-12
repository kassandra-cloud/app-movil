package com.example.proyecto.api

import com.example.proyecto.data.Page
import com.example.proyecto.data.recursos.RecursoDto
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.proyecto.data.recursos.SolicitudDto
import com.example.proyecto.data.recursos.CrearSolicitudReq
import retrofit2.http.*

interface RecursosApi {
    @GET("recursos/api/v1/recursos/")
    suspend fun listarRecursos(
        @Query("disponible") disponible: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = 20
    ): Page<RecursoDto>

    @GET("recursos/api/v1/solicitudes/")
    suspend fun misSolicitudes(
        @Query("todas") todas: Int? = null   // normal: null -> solo mías
    ): Page<SolicitudDto>

    @POST("recursos/api/v1/solicitudes/")
    suspend fun crearSolicitud(@Body body: CrearSolicitudReq): SolicitudDto
    @GET("recursos/api/v1/solicitudes/")
    suspend fun misSolicitudes(
        @Query("mine") mine: Boolean = true,
        @Query("estado") estado: String? = null
    ): com.example.proyecto.data.Page<com.example.proyecto.data.recursos.SolicitudDto>
}
