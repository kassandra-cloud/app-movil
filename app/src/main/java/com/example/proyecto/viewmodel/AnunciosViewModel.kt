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
import kotlinx.coroutines.launch
import com.example.proyecto.data.SessionData

class AnunciosViewModel : ViewModel() {
    var anuncios by mutableStateOf<List<AnuncioDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun cargarAnuncios() {
        val token = SessionData.token
        if (token.isNullOrBlank()) {
            errorMessage = "Error: No hay token de sesión. Cierra sesión y vuelve a entrar."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                Log.d("AnunciosVM", "Solicitando anuncios...")
                val response = ApiClient.apiService.obtenerAnuncios("Token $token")

                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    Log.d("AnunciosVM", "Éxito! Recibidos ${lista.size} anuncios.")
                    anuncios = lista
                } else {
                    val errorBody = response.errorBody()?.string()
                    errorMessage = "Error servidor (${response.code()}): $errorBody"
                    Log.e("AnunciosVM", errorMessage!!)
                }
            } catch (e: Exception) {
                // 🔥 Esto capturará errores de Moshi si el JSON no coincide
                Log.e("AnunciosVM", "Excepción", e)
                errorMessage = "Error App: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }
}

class AnunciosViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnunciosViewModel::class.java)) {
            return AnunciosViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}