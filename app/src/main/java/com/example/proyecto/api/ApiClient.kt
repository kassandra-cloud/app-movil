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

    // Asegúrate de que esta IP sea la correcta donde corre tu Django ahora
    private const val BASE_URL = "http://192.168.104.132:8000/"

    // --- Interceptores útiles ---
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

    // --- Configuración de Moshi para Kotlin ---
    // 🔥 ESTO ES LO QUE SOLUCIONA EL ERROR DE "Unable to create converter"
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // --- Cliente base SIN token (login/público) ---
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

    // --- Retrofit base SIN token ---
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseClient)
            // 🔥 USAMOS NUESTRA CONFIGURACIÓN 'moshi' AQUÍ
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // --- Cliente CON token (para endpoints protegidos) ---
    private fun authClient(token: String): OkHttpClient {
        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder().apply {
                if (token.isNotBlank()) {
                    // DRF TokenAuth
                    header("Authorization", "Token $token")
                }
            }.build()
            chain.proceed(req)
        }
        return baseClient.newBuilder()
            .addInterceptor(auth)
            .build()
    }

    // --- Retrofit CON token ---
    fun getClient(token: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authClient(token))
            // USAMOS NUESTRA CONFIGURACIÓN 'moshi' TAMBIÉN AQUÍ
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    // --- Helpers de creación de servicios ---
    fun <T> createAuthorized(token: String, service: Class<T>): T =
        getClient(token).create(service)

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
    val reunionesApi: ReunionesApi by lazy { retrofit.create(ReunionesApi::class.java) }
    val talleresApi: TalleresApi by lazy { retrofit.create(TalleresApi::class.java) }
    val foroApi: ForoApi by lazy { retrofit.create(ForoApi::class.java) }
    val recursosApi: RecursosApi by lazy { retrofit.create(RecursosApi::class.java) }

    // Para armar URLs absolutas desde la app si lo necesitas
    val baseUrl: String get() = retrofit.baseUrl().toString()
}