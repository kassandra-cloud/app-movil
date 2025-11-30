package com.example.proyecto.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


// -------------------- Petición de Login --------------------
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") // Enviamos el dato bajo la etiqueta "username"
    val username: String,
    @Json(name = "password")
    val password: String
)

// -------------------- Respuesta de Login --------------------
@JsonClass(generateAdapter = true)
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null,
    @Json(name = "must_change_password")
    val must_change_password: Boolean? = false

)

// -------------------- Modelo de Usuario --------------------
@JsonClass(generateAdapter = true)
data class User(
    val id: Int,
    val username: String,
    val email: String? = null,      // Agregado
    val first_name: String? = null, // Agregado (Nombre)
    val last_name: String? = null   // Agregado (Apellido)
)