package com.example.proyecto.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Ajusta la IP según tu entorno (10.0.2.2 para emulador, IP local para físico)
    private const val BASE_URL = "http://192.168.1.8:8000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val timing = Interceptor { chain ->
        val t0 = System.nanoTime()
        val res = chain.proceed(chain.request())
        val t1 = System.nanoTime()
        android.util.Log.d("NET", "${res.code} ${res.request.url} ${(t1 - t0) / 1_000_000} ms")
        res
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(timing)
            .retryOnConnectionFailure(true)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun authClient(token: String): OkHttpClient {
        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder().apply {
                if (token.isNotBlank()) {
                    header("Authorization", "Token $token")
                }
            }.build()
            chain.proceed(req)
        }
        return baseClient.newBuilder()
            .addInterceptor(auth)
            .build()
    }

    fun getClient(token: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authClient(token))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    // --- HELPERS ---

    // 👇 ESTA ES LA FUNCIÓN QUE FALTABA
    fun <T> createPublic(service: Class<T>): T =
        retrofit.create(service)

    fun <T> createAuthorized(token: String, service: Class<T>): T =
        getClient(token).create(service)

    // Accesos rápidos (opcionales si usas los genéricos arriba)
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
    val reunionesApi: ReunionesApi by lazy { retrofit.create(ReunionesApi::class.java) }
    val talleresApi: TalleresApi by lazy { retrofit.create(TalleresApi::class.java) }
    val foroApi: ForoApi by lazy { retrofit.create(ForoApi::class.java) }
    val recursosApi: RecursosApi by lazy { retrofit.create(RecursosApi::class.java) }

    val baseUrl: String get() = retrofit.baseUrl().toString()
}