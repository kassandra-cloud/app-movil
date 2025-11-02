package com.example.proyecto.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://192.168.0.106:8000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Mide latencia de cada request en Logcat (tag NET)
    private val timing = Interceptor { chain ->
        val t0 = System.nanoTime()
        val res = chain.proceed(chain.request())
        val t1 = System.nanoTime()
        android.util.Log.d("NET", "${res.code} ${res.request.url} ${(t1 - t0) / 1_000_000} ms")
        res
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(timing)
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS) // antes 30
        .readTimeout(10, TimeUnit.SECONDS)    // antes 30
        .writeTimeout(10, TimeUnit.SECONDS)   // antes 30
        .callTimeout(12, TimeUnit.SECONDS)    // límite total por llamada
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
    val reunionesApi: ReunionesApi = retrofit.create(ReunionesApi::class.java)
}
