package com.example.proyecto.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.ApiService
import com.example.proyecto.api.ActasApi          // ✅ IMPORTANTE: API de actas
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.SessionData     // Token global
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
                    _uiState.update {
                        it.copy(errorMessage = "Error al registrar notificaciones (${resp.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM_REG", "Fallo al registrar token FCM: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error al registrar notificaciones.") }
            }
        }
    }

    // =======================================================
    // LOGIN
    // =======================================================

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, completa todos los campos") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {

                        val jwtToken = body.token
                        SessionData.token = jwtToken // token global

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUser = username,
                                token = jwtToken,
                                currentScreen = AppScreen.MAIN_MENU,
                                successMessage = "¡Bienvenido, $username!"
                            )
                        }

                        // Registrar token FCM en backend
                        if (jwtToken != null) {
                            val fcmToken = getFCMToken()
                            if (fcmToken != null) {
                                sendFCMTokenToServer(jwtToken, fcmToken)
                            } else {
                                Log.w("FCM_REG", "No se pudo obtener el token de FCM.")
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
                        400 -> "Datos de login inválidos"
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

    fun logout() {
        _uiState.value = LoginUiState()  // resetea estado UI
        SessionData.token = null        // limpia token global
        // Si quisieras, aquí podrías también desregistrar el FCM en el backend
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
    // ACTAS: DETALLE DESDE LISTADO Y DESDE REUNIONES REALIZADAS
    // =======================================================

    /**
     * Abre el detalle de acta cuando YA tienes el ActaDto.
     * Se usa, por ejemplo, desde la pantalla de listado de Actas.
     */
    fun openActaDetalle(acta: ActaDto) {
        // Si quieres bloquear actas no aprobadas, deja esta validación
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

    /**
     * Cerrar el detalle de acta.
     * Ahora mismo te devuelve a ACTAS. Si prefieres volver a REUNIONES_REALIZADAS,
     * cambia el currentScreen.
     */
    fun closeActaDetalle() {
        _uiState.update {
            it.copy(
                selectedActa = null,
                currentScreen = AppScreen.REUNIONES_REALIZADAS
            )
        }
    }

    /**
     * Llamado desde ReunionesRealizadasScreen cuando tocas "Ver acta".
     * 1) Usa el token de sesión.
     * 2) Llama al endpoint /reuniones/api/actas/{id}/ vía ActasApi.
     * 3) Si funciona, abre el detalle con openActaDetalle(acta).
     */
    fun openActaDesdeReunion(actaId: Int) {
        val tokenActual = _uiState.value.token ?: return

        viewModelScope.launch {
            try {
                val api = ApiClient.createAuthorized(tokenActual, ActasApi::class.java)
                val acta: ActaDto = api.getActaDetalle(actaId)

                openActaDetalle(acta)

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al cargar acta $actaId", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "No se pudo cargar el detalle del acta."
                    )
                }
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
    // REUNIÓN EN CURSO (detalle)
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
    // CARGA DE REUNIONES (si las traes desde API)
    // =======================================================

    fun loadReuniones() {
        viewModelScope.launch {
            try {
                // Aquí iría tu llamada real a API si ya la tienes implementada.
                // Ejemplo:
                // val resp = ApiClient.apiService.getReuniones("Token ${uiState.value.token}")
                // if (resp.isSuccessful) _reuniones.value = resp.body().orEmpty()
                // else _uiState.update { it.copy(errorMessage = "No se pudieron cargar las reuniones") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error cargando reuniones: ${e.message}")
                }
            }
        }
    }

    // =======================================================
    // DEMO LOCAL DE REUNIONES (solo UI)
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
