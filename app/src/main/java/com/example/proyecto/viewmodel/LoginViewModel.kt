package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.ApiService
import com.example.proyecto.data.AppScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la UI
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val token: String? = null,
    val currentUser: String? = null,
    val currentScreen: AppScreen = AppScreen.LOGIN,

    // Datos extra para navegación
    val selectedReunionEnCurso: com.example.proyecto.data.reuniones.ReunionDto? = null,
    val selectedActa: com.example.proyecto.data.reuniones.ActaDto? = null,
    val selectedPublicacion: com.example.proyecto.data.PublicacionDto? = null
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // -----------------------------------------------------------
    // LOGIN
    // -----------------------------------------------------------
    fun login(user: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Usamos la instancia 'apiService' que ya existe en ApiClient
                val response = ApiClient.apiService.login(com.example.proyecto.data.LoginRequest(user, pass))

                // Suponiendo que tu endpoint devuelve un objeto con 'token'
                // Ajusta esto según tu respuesta real (LoginResponse)
                // Aquí simulo éxito para que veas el flujo:
                delay(1000)

                // SI TU LOGIN REAL FUNCIONA, DESCOMENTA ESTO Y COMENTA LA SIMULACIÓN:
                /*
                if (response.token != null) {
                     _uiState.update {
                        it.copy(
                            isLoading = false,
                            token = response.token,
                            currentUser = user,
                            currentScreen = AppScreen.MAIN_MENU
                        )
                    }
                } else {
                    throw Exception("Credenciales inválidas")
                }
                */

                // SIMULACIÓN (BORRAR CUANDO CONECTES EL LOGIN REAL):
                if (user.lowercase() == "error") throw Exception("Credenciales incorrectas")
                _uiState.update {
                    it.copy(isLoading = false, token = "dummy_token", currentUser = user, currentScreen = AppScreen.MAIN_MENU)
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // -----------------------------------------------------------
    // RECUPERACIÓN PASO 1: ENVIAR CÓDIGO
    // -----------------------------------------------------------
    fun sendRecoveryCode(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un correo válido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                // Creamos el servicio público (sin token)
                val api = ApiClient.createPublic(ApiService::class.java)

                val response = api.solicitarCodigo(mapOf("email" to email))

                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Código enviado a $email") }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No se pudo enviar el código. Verifica el correo.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error de conexión: ${e.message}") }
            }
        }
    }

    // -----------------------------------------------------------
    // RECUPERACIÓN PASO 2: CAMBIAR CON CÓDIGO
    // -----------------------------------------------------------
    fun resetPasswordWithCode(email: String, code: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val api = ApiClient.createPublic(ApiService::class.java)

                val response = api.restablecerPassword(mapOf(
                    "email" to email,
                    "code" to code,
                    "new_password" to newPass
                ))

                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, successMessage = "¡Contraseña actualizada! Inicia sesión.") }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Código incorrecto o expirado") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun changeInitialPassword(newPass: String) {
        // Lógica existente para primer login
    }

    // -----------------------------------------------------------
    // NAVEGACIÓN
    // -----------------------------------------------------------
    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null, successMessage = null) }
    }

    fun goBackToMainMenu() {
        _uiState.update { it.copy(currentScreen = AppScreen.MAIN_MENU, selectedReunionEnCurso = null, selectedActa = null) }
    }

    fun logout() {
        _uiState.update { LoginUiState(currentScreen = AppScreen.LOGIN) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // Navegación de detalles
    fun openReunionEnCurso(dto: com.example.proyecto.data.reuniones.ReunionDto) {
        _uiState.update { it.copy(selectedReunionEnCurso = dto, currentScreen = AppScreen.REUNION_EN_CURSO_DETALLE) }
    }
    fun closeReunionEnCurso() {
        _uiState.update { it.copy(selectedReunionEnCurso = null, currentScreen = AppScreen.REUNIONES_EN_CURSO) }
    }
    fun updateSelectedReunionEnCurso(dto: com.example.proyecto.data.reuniones.ReunionDto) {
        _uiState.update { it.copy(selectedReunionEnCurso = dto) }
    }
    fun openActaDesdeReunion(actaId: Int) { /* Implementar carga por ID si necesario */ }
    fun openActaDetalle(acta: com.example.proyecto.data.reuniones.ActaDto) {
        _uiState.update { it.copy(selectedActa = acta, currentScreen = AppScreen.ACTA_DETALLE) }
    }
    fun closeActaDetalle() {
        _uiState.update { it.copy(selectedActa = null, currentScreen = AppScreen.ACTAS) }
    }
    fun openPublicacionDetalle(pub: com.example.proyecto.data.PublicacionDto) {
        _uiState.update { it.copy(selectedPublicacion = pub, currentScreen = AppScreen.ASISTENCIA_DETALLE) }
    }
    fun closePublicacionDetalle() {
        _uiState.update { it.copy(selectedPublicacion = null, currentScreen = AppScreen.ASISTENCIA) }
    }
}