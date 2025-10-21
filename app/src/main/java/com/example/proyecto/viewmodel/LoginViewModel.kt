package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    MAIN_MENU,
    ACTAS,
    ASISTENCIA,
    VOTACION,
    TALLERES
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: String? = null,
    val token: String? = null,                    // 👈 AÑADIDO
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun testConnection() {
        viewModelScope.launch {
            try {
                val testData = mapOf("test" to "connection")
                val response = ApiClient.apiService.testConnection(testData)

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Conexión exitosa: ${response.body()}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error de conexión: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Por favor, completa todos los campos"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            try {
                val loginRequest = LoginRequest(username, password)
                val response = ApiClient.apiService.login(loginRequest)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = username,
                            token = loginResponse.token,         // 👈 GUARDA EL TOKEN
                            currentScreen = AppScreen.MAIN_MENU,
                            successMessage = "¡Bienvenido, $username!"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = loginResponse?.message ?: "Error de autenticación"
                        )
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Usuario o contraseña incorrectos"
                        400 -> "Datos de login inválidos"
                        500 -> "Error interno del servidor"
                        else -> "Error del servidor: ${response.code()}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        _uiState.value = LoginUiState()  // resetea también el token
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun goBackToMainMenu() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.MAIN_MENU)
    }
}
