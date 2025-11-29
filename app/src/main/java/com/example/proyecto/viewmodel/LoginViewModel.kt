package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.ApiService
import com.example.proyecto.data.AppScreen
import com.example.proyecto.data.LoginRequest
import com.example.proyecto.data.SessionData
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
                val cleanUser = user.trim()

                // Llamada a la API
                val response = ApiClient.apiService.login(LoginRequest(cleanUser, pass))

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse != null && loginResponse.token != null) {

                        // Guardamos el token en la sesión global
                        SessionData.token = loginResponse.token

                        // === LÓGICA DE NOMBRE REAL ===
                        val nombre = loginResponse.user?.first_name
                        val apellido = loginResponse.user?.last_name

                        val nombreMostrar = if (!nombre.isNullOrBlank()) {
                            "$nombre ${apellido ?: ""}".trim()
                        } else {
                            loginResponse.user?.username ?: cleanUser
                        }

                        // === LÓGICA DE REDIRECCIÓN (CAMBIO DE CONTRASEÑA) ===
                        val debeCambiarPass = loginResponse.must_change_password == true

                        val proximaPantalla = if (debeCambiarPass) {
                            AppScreen.CHANGE_PASSWORD
                        } else {
                            AppScreen.MAIN_MENU
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                token = loginResponse.token,
                                currentUser = nombreMostrar,
                                currentScreen = proximaPantalla
                            )
                        }
                    } else {
                        val msg = loginResponse?.message ?: "Error en las credenciales"
                        throw Exception(msg)
                    }
                } else {
                    val errorMsg = "Error del servidor: ${response.code()} ${response.message()}"
                    throw Exception(errorMsg)
                }

            } catch (e: Exception) {
                val msg = if (e.message?.contains("401") == true)
                    "Usuario o contraseña incorrectos"
                else
                    e.message ?: "Error desconocido"

                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    // -----------------------------------------------------------
    // CAMBIAR CONTRASEÑA INICIAL (Lógica Real Agregada)
    // -----------------------------------------------------------
    fun changeInitialPassword(newPass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 1. Obtenemos el token de la sesión actual
                val token = SessionData.token
                if (token == null) {
                    throw Exception("No hay sesión activa. Por favor, inicia sesión nuevamente.")
                }

                // 2. Preparamos el header Auth
                val authHeader = "Token $token"

                // 3. Llamamos al endpoint del backend
                val response = ApiClient.apiService.cambiarPasswordInicial(
                    auth = authHeader,
                    body = mapOf("new_password" to newPass)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val msg = body?.get("message")?.toString() ?: "Contraseña actualizada exitosamente"

                    // 4. Éxito: Navegamos al menú principal
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = msg,
                            currentScreen = AppScreen.MAIN_MENU
                        )
                    }
                } else {
                    val errorMsg = response.message()
                    throw Exception("Error al actualizar: $errorMsg")
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

    // -----------------------------------------------------------
    // NAVEGACIÓN Y OTROS
    // -----------------------------------------------------------
    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null, successMessage = null) }
    }

    fun goBackToMainMenu() {
        _uiState.update { it.copy(currentScreen = AppScreen.MAIN_MENU, selectedReunionEnCurso = null, selectedActa = null) }
    }

    fun logout() {
        SessionData.token = null
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
    fun openActaDesdeReunion(actaId: Int) { }
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