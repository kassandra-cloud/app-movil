package com.example.proyecto.api

import com.example.proyecto.data.Page
import com.example.proyecto.data.recursos.RecursoDto
import com.example.proyecto.data.recursos.SolicitudDto
import com.example.proyecto.data.recursos.CrearSolicitudReq
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
        @Query("todas") todas: Int? = null,  // null = Solo mías, 1 = Ver todas (si eres admin)
        @Query("estado") estado: String? = null
    ): Page<SolicitudDto>

    @POST("recursos/api/v1/solicitudes/")
    suspend fun crearSolicitud(@Body body: CrearSolicitudReq): SolicitudDto
}