package com.example.proyecto.data

// 💡 CAMBIO: Usar las anotaciones de Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==============================================================
// >>> OBJETO PARA ALMACENAR EL ESTADO DE SESIÓN (SOLUCIÓN) <<<
// ==============================================================
/**
 * Objeto Singleton para almacenar datos de la sesión del usuario,
 * principalmente el token JWT.
 */
object SessionData {
    // El token debe ser mutable y opcional (null) para reflejar la sesión no iniciada.
    var token: String? = null
    // Otros datos de sesión que quieras guardar aquí...
}
// ==============================================================


// -------------------- Petición de Login --------------------
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username")
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
    val user: User? = null
)

// -------------------- Modelo de Usuario --------------------
@JsonClass(generateAdapter = true)
data class User(
    val username: String,
    val id: Int
)