package com.example.proyecto.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.taller.TallerDto
import kotlinx.coroutines.launch

class TalleresViewModel : ViewModel() {

    data class UiState(
        val cargando: Boolean = false,
        val talleres: List<TallerDto> = emptyList(),
        val error: String? = null,
        val inscribiendoId: Int? = null
    )

    var uiState by mutableStateOf(UiState(cargando = true))
        private set

    init { cargar() }

    fun cargar() {
        uiState = uiState.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                val data = ApiClient.talleresApi.listar()
                uiState = uiState.copy(cargando = false, talleres = data)
            } catch (e: Exception) {
                uiState = uiState.copy(cargando = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    fun inscribir(id: Int, token: String) {
        if (uiState.inscribiendoId != null) return
        uiState = uiState.copy(inscribiendoId = id, error = null)
        viewModelScope.launch {
            try {
                val actualizado = ApiClient.talleresApi.inscribir(id, "Token $token")
                uiState = uiState.copy(
                    talleres = uiState.talleres.map { if (it.id == id) actualizado else it },
                    inscribiendoId = null
                )
            } catch (e: Exception) {
                uiState = uiState.copy(inscribiendoId = null, error = e.message ?: "No se pudo inscribir")
            }
        }
    }

    fun desinscribir(id: Int, token: String) {
        if (uiState.inscribiendoId != null) return
        uiState = uiState.copy(inscribiendoId = id, error = null)
        viewModelScope.launch {
            try {
                val actualizado = ApiClient.talleresApi.desinscribir(id, "Token $token")
                uiState = uiState.copy(
                    talleres = uiState.talleres.map { if (it.id == id) actualizado else it },
                    inscribiendoId = null
                )
            } catch (e: Exception) {
                uiState = uiState.copy(inscribiendoId = null, error = e.message ?: "No se pudo desinscribir")
            }
        }
    }
}
