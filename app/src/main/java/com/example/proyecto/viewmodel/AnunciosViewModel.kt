package com.example.proyecto.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.AnuncioDto
import com.example.proyecto.data.SessionData
import kotlinx.coroutines.launch

class AnunciosViewModel : ViewModel() {
    // Estado de la UI
    var anuncios by mutableStateOf<List<AnuncioDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun cargarAnuncios() {
        val token = SessionData.token
        if (token.isNullOrBlank()) {
            errorMessage = "No hay sesión iniciada"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.apiService.obtenerAnuncios("Token $token")
                if (response.isSuccessful) {
                    anuncios = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AnunciosVM", "Error red", e)
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}

// Fábrica para crear el ViewModel (boilerplate estándar)
class AnunciosViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnunciosViewModel::class.java)) {
            return AnunciosViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}