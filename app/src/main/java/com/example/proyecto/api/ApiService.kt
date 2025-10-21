package com.example.proyecto.api

import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("usuarios/api/login/")     // sin "/" inicial y con "/" final
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    @POST("usuarios/api/test/")
    suspend fun testConnection(@Body testData: Map<String, String>): Response<Map<String, Any>>

}
