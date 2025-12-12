package com.example.proyecto.network

import com.example.proyecto.data.SessionData
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // Cliente HTTP con Interceptor para el Token
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val token = SessionData.token

            val requestBuilder = original.newBuilder()

            // Si tenemos token, lo agregamos a la cabecera
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Token $token")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            // Asegúrate que esta IP sea correcta para tu red
            .baseUrl("https://junta-de-vecinos-villa-vista-al-mar.onrender.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> create(service: Class<T>): T {
        return retrofit.create(service)
    }
}