package com.example.proyecto.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.ApiService
import com.example.proyecto.api.ActasApi
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.SessionData
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.data.reuniones.ActaDto
import com.example.proyecto.data.reuniones.Reunion
import com.example.proyecto.data.reuniones.ReunionDto
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// =============================
// UI STATE
// =============================
data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: String? = null,
    val token: String? = null,
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    //  NUEVO: Bandera para saber si debe cambiar contraseña
    val isPasswordChangeRequired: Boolean = false,

    // Detalles / selección
    val selectedActa: ActaDto? = null,
    val selectedPublicacion: PublicacionDto? = null,
    val selectedReunionEnCurso: ReunionDto? = null,
)

class LoginViewModel : ViewModel() {

    // Instancia del servicio API base
    private val apiService: ApiService = ApiClient.apiService

    /* ---------- UI STATE ---------- */
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /* ---------- REUNIONES (mock local) ---------- */
    private val _reuniones = MutableStateFlow(demoReuniones())
    val reuniones: StateFlow<List<Reunion>> = _reuniones.asStateFlow()

    // =======================================================
    // FCM: Obtener y registrar token de notificaciones
    // =======================================================

    private suspend fun getFCMToken(): String? = suspendCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Token obtenido: $token")
                continuation.resume(token)
            } else {
                Log.e("FCM_TOKEN", "Error al obtener token FCM", task.exception)
                continuation.resume(null)
            }
        }
    }

    private fun sendFCMTokenToServer(apiToken: String, fcmToken: String) {
        viewModelScope.launch {
            try {
                // Tu backend usa DRF Token Auth → "Token <token>"
                val authHeader = "Token $apiToken"

                val body = mapOf(
                    "fcm_token" to fcmToken
                )

                val resp = apiService.registrarFCMToken(authHeader, body)

                if (resp.isSuccessful) {
                    Log.i("FCM_REG", "Token FCM enviado y registrado en el backend. Código: ${resp.code()}")
                } else {
                    Log.e(
                        "FCM_REG",
                        "Error HTTP al registrar token FCM: ${resp.code()} - ${resp.errorBody()?.string()}"
                    )
                    // Opcional: No mostrar error en UI si falla esto en background
                }
            } catch (e: Exception) {
                Log.e("FCM_REG", "Fallo al registrar token FCM: ${e.message}", e)
            }
        }
    }

    // =======================================================
    // LOGIN (MODIFICADO)
    // =======================================================

    fun login(loginInput: String, password: String) {

        //  PASO 1: Limpieza automática (Trim)
        // Quitamos espacios en blanco al inicio y al final de lo que escribió el usuario
        val usuarioLimpio = loginInput.trim()
        val passwordLimpia = password.trim()

        if (usuarioLimpio.isBlank() || passwordLimpia.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, completa todos los campos") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            try {
                //  PASO 2: Enviamos las variables LIMPIAS al servidor
                val response = ApiClient.apiService.login(LoginRequest(usuarioLimpio, passwordLimpia))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {

                        val jwtToken = body.token
                        SessionData.token = jwtToken

                        if (body.must_change_password == true) {
                            // CASO A: Usuario nuevo
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    token = jwtToken,
                                    isPasswordChangeRequired = true,
                                    currentScreen = AppScreen.CHANGE_PASSWORD,
                                    successMessage = "Verificación exitosa. Crea tu nueva contraseña."
                                )
                            }
                        } else {
                            // CASO B: Usuario normal
                            val usuarioObj = body.user

                            val nombreParaMostrar = if (!usuarioObj?.first_name.isNullOrBlank()) {
                                val primerApellido = usuarioObj?.last_name?.trim()?.split(" ")?.firstOrNull() ?: ""
                                "${usuarioObj?.first_name} $primerApellido".trim()
                            } else {
                                usuarioObj?.username ?: usuarioLimpio // Usamos el usuario limpio como respaldo
                            }

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    currentUser = nombreParaMostrar,
                                    token = jwtToken,
                                    isPasswordChangeRequired = false,
                                    currentScreen = AppScreen.MAIN_MENU,
                                    successMessage = "¡Bienvenido, $nombreParaMostrar!"
                                )
                            }

                            if (jwtToken != null) {
                                val fcmToken = getFCMToken()
                                if (fcmToken != null) {
                                    sendFCMTokenToServer(jwtToken, fcmToken)
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = body?.message ?: "Error de autenticación"
                            )
                        }
                    }
                } else {
                    val msg = when (response.code()) {
                        401 -> "Usuario o contraseña incorrectos"
                        400 -> "Datos inválidos"
                        500 -> "Error interno del servidor"
                        else -> "Error del servidor: ${response.code()}"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error de conexión: ${e.message}"
                    )
                }
            }
        }
    }

    //  NUEVA FUNCIÓN: CAMBIAR CONTRASEÑA INICIAL
    fun changeInitialPassword(newPassword: String) {
        val token = _uiState.value.token ?: return
        val apiToken = "Token $token"

        if (newPassword.length < 12) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 12 caracteres.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // ⚠ Debes agregar 'cambiarPasswordInicial' a tu interfaz ApiService
                // @POST("usuarios/api/cambiar-password-inicial/")
                // suspend fun cambiarPasswordInicial(@Header("Authorization") auth: String, @Body body: Map<String, String>): Response<Any>

                val body = mapOf("new_password" to newPassword)
                val response = ApiClient.apiService.cambiarPasswordInicial(apiToken, body)

                if (response.isSuccessful) {
                    // Éxito: Quitamos bandera y vamos al menú
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordChangeRequired = false,
                            currentScreen = AppScreen.MAIN_MENU,
                            successMessage = "Contraseña actualizada correctamente. ¡Bienvenido!"
                        )
                    }

                    // Ahora sí registramos FCM
                    val fcmToken = getFCMToken()
                    if (fcmToken != null) {
                        sendFCMTokenToServer(token, fcmToken)
                    }

                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error al actualizar: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error de conexión: ${e.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        _uiState.value = LoginUiState()  // resetea estado UI
        SessionData.token = null        // limpia token global
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // =======================================================
    // NAVEGACIÓN GENERAL
    // =======================================================

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun goBackToMainMenu() {
        _uiState.update { it.copy(currentScreen = AppScreen.MAIN_MENU) }
    }

    // =======================================================
    // ACTAS: DETALLE
    // =======================================================

    fun openActaDetalle(acta: ActaDto) {
        if (!acta.aprobada) {
            _uiState.update { it.copy(errorMessage = "El acta aún no está aprobada.") }
            return
        }
        _uiState.update {
            it.copy(
                selectedActa = acta,
                currentScreen = AppScreen.ACTA_DETALLE
            )
        }
    }

    fun closeActaDetalle() {
        _uiState.update {
            it.copy(
                selectedActa = null,
                currentScreen = AppScreen.REUNIONES_REALIZADAS
            )
        }
    }

    fun openActaDesdeReunion(actaId: Int) {
        val tokenActual = _uiState.value.token ?: return

        viewModelScope.launch {
            try {
                val api = ApiClient.createAuthorized(tokenActual, ActasApi::class.java)
                val acta: ActaDto = api.getActaDetalle(actaId)
                openActaDetalle(acta)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al cargar acta $actaId", e)
                _uiState.update { it.copy(errorMessage = "No se pudo cargar el detalle del acta.") }
            }
        }
    }

    // =======================================================
    // FORO / ASISTENCIA
    // =======================================================

    fun openPublicacionDetalle(pub: PublicacionDto) {
        _uiState.update {
            it.copy(
                selectedPublicacion = pub,
                currentScreen = AppScreen.ASISTENCIA_DETALLE
            )
        }
    }

    fun closePublicacionDetalle() {
        _uiState.update {
            it.copy(
                selectedPublicacion = null,
                currentScreen = AppScreen.ASISTENCIA
            )
        }
    }

    // =======================================================
    // REUNIÓN EN CURSO
    // =======================================================

    fun openReunionEnCurso(reunion: ReunionDto) {
        _uiState.update {
            it.copy(
                selectedReunionEnCurso = reunion,
                currentScreen = AppScreen.REUNION_EN_CURSO_DETALLE
            )
        }
    }

    fun closeReunionEnCurso() {
        _uiState.update {
            it.copy(
                selectedReunionEnCurso = null,
                currentScreen = AppScreen.REUNIONES_EN_CURSO
            )
        }
    }

    fun updateSelectedReunionEnCurso(actualizada: ReunionDto) {
        _uiState.update { current ->
            if (current.selectedReunionEnCurso?.id == actualizada.id) {
                current.copy(selectedReunionEnCurso = actualizada)
            } else {
                current
            }
        }
    }

    // =======================================================
    // CARGA DE REUNIONES
    // =======================================================

    fun loadReuniones() {
        // Implementar lógica de carga real si es necesario
    }

    // =======================================================
    // DEMO LOCAL DE REUNIONES
    // =======================================================

    private fun demoReuniones(): List<Reunion> = listOf(
        Reunion(
            id = 1,
            titulo = "Directiva",
            descripcion = "Acta #15",
            inicio = LocalDateTime.now().minusDays(3),
            fin = LocalDateTime.now().minusDays(3).plusHours(1)
        ),
        Reunion(
            id = 2,
            titulo = "Asamblea mensual",
            descripcion = "Sede JJVV",
            inicio = LocalDateTime.now().plusDays(2).withHour(19).withMinute(0),
            fin = LocalDateTime.now().plusDays(2).withHour(20).withMinute(30)
        ),
        Reunion(
            id = 3,
            titulo = "Comité de seguridad",
            descripcion = "Invitados: Carabineros",
            inicio = LocalDateTime.now().plusDays(5).withHour(18).withMinute(30),
            fin = LocalDateTime.now().plusDays(5).withHour(19).withMinute(30)
        ),
        Reunion(
            id = 4,
            titulo = "Cierre de proyecto",
            descripcion = "Entrega de informe",
            inicio = LocalDateTime.now().minusDays(10).withHour(20).withMinute(0),
            fin = LocalDateTime.now().minusDays(10).withHour(21).withMinute(0)
        )
    )
}