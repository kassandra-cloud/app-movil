package com.example.proyecto.data

// 💡 CAMBIO: Usar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// -------------------- Petición de Login --------------------
@JsonClass(generateAdapter = true) // 👈 Moshi requiere esto en TODAS las data class
data class LoginRequest(
    // 💡 CAMBIO: Reemplazar @SerializedName por @Json
    @Json(name = "username")
    val username: String,
    @Json(name = "password")
    val password: String
)

// -------------------- Respuesta de Login --------------------
@JsonClass(generateAdapter = true) // 👈 Moshi requiere esto
data class LoginResponse(
    // En Moshi, si el nombre del campo Kotlin coincide con el JSON, no se necesita @Json(name)
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null
)

// -------------------- Modelo de Usuario --------------------
@JsonClass(generateAdapter = true) // 👈 Moshi requiere esto
data class User(
    val username: String,
    val id: Int
)