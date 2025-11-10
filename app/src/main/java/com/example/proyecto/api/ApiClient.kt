package com.example.proyecto.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Cambia la IP/host según tu backend
    private const val BASE_URL = "http://192.168.0.100:8000/"

    // Logger de red
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor para medir tiempos (opcional)
    private val timing = Interceptor { chain ->
        val t0 = System.nanoTime()
        val res = chain.proceed(chain.request())
        val t1 = System.nanoTime()
        android.util.Log.d(
            "NET",
            "${res.code} ${res.request.url} ${(t1 - t0) / 1_000_000} ms"
        )
        res
    }

    // Cliente HTTP
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(timing)
            .retryOnConnectionFailure(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Exposición de APIs
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
    val reunionesApi: ReunionesApi by lazy { retrofit.create(ReunionesApi::class.java) }
    val talleresApi: TalleresApi by lazy { retrofit.create(TalleresApi::class.java) }
    val foroApi: ForoApi by lazy { retrofit.create(ForoApi::class.java) }

    // <<< IMPORTANTE PARA absoluteUrl() EN LA APP >>>
    // Expone la base que realmente usa Retrofit
    val baseUrl: String
        get() = retrofit.baseUrl().toString()
}
