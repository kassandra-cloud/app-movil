package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.reuniones.ActaDto
import com.example.proyecto.data.reuniones.Reunion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: String? = null,
    val token: String? = null,
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedActa: ActaDto? = null
)

class LoginViewModel : ViewModel() {

    /* ---------- UI STATE ---------- */
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /* ---------- REUNIONES (reactivo) ---------- */
    private val _reuniones = MutableStateFlow(demoReuniones()) // <— mock inicial
    val reuniones: StateFlow<List<Reunion>> = _reuniones.asStateFlow()

    /* ---------- Auth ---------- */
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                currentUser = username,
                                token = body.token,
                                currentScreen = AppScreen.MAIN_MENU,
                                successMessage = "¡Bienvenido, $username!"
                            )
                        }
                        // Cargar reuniones reales después del login (opcional)
                        // loadReuniones()
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = body?.message ?: "Error de autenticación")
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
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error de conexión: ${e.message}") }
            }
        }
    }

    fun logout() {
        _uiState.value = LoginUiState() // resetea todo (incluye token)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /* ---------- Navegación ---------- */
    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun goBackToMainMenu() {
        _uiState.update { it.copy(currentScreen = AppScreen.MAIN_MENU) }
    }

    /* ---------- Actas ---------- */
    fun openActaDetalle(acta: ActaDto) {
        if (!acta.aprobada) {
            _uiState.update { it.copy(errorMessage = "El acta aún no está aprobada.") }
            return
        }
        _uiState.update { it.copy(selectedActa = acta, currentScreen = AppScreen.ACTA_DETALLE) }
    }

    fun closeActaDetalle() {
        _uiState.update { it.copy(selectedActa = null, currentScreen = AppScreen.ACTAS) }
    }

    /* ---------- Carga de reuniones (API) ---------- */
    fun loadReuniones() {
        viewModelScope.launch {
            try {
                // TODO: reemplazar por tu endpoint real
                // val resp = ApiClient.apiService.getReuniones("Bearer ${uiState.value.token}")
                // if (resp.isSuccessful) _reuniones.value = resp.body().orEmpty()
                // else _uiState.update { it.copy(errorMessage = "No se pudieron cargar las reuniones") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error cargando reuniones: ${e.message}") }
            }
        }
    }

    /* ---------- Mock local para compilar y probar UI ---------- */
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
