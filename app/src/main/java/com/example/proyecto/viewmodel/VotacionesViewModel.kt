package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.VotacionesApi
import com.example.proyecto.data.votaciones.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VotacionesUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val abiertas: List<VotacionDto> = emptyList(),
    val mensaje: String? = null
)

class VotacionesViewModel : ViewModel() {
    private val api: VotacionesApi = ApiClient.create(VotacionesApi::class.java)

    private val _ui = MutableStateFlow(VotacionesUiState())
    val ui: StateFlow<VotacionesUiState> = _ui

    fun cargarAbiertas(token: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(cargando = true, error = null, mensaje = null)
            try {
                val data = api.listarAbiertas("Token $token")
                _ui.value = _ui.value.copy(cargando = false, abiertas = data)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    fun votar(token: String, votacionId: Int, opcionId: Int) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(cargando = true, error = null, mensaje = null)
            try {
                val resp = api.votar(votacionId, "Token $token", VotarRequest(opcionId))
                _ui.value = _ui.value.copy(cargando = false, mensaje = resp.mensaje)
                cargarAbiertas(token)  // refrescar estado
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "No se pudo votar")
            }
        }
    }
}
