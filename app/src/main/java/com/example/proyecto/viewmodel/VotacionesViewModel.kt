package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
// 🔴 ELIMINA ESTE IMPORT: import com.example.proyecto.api.VotoRequest
// 🟢 AGREGA ESTE IMPORT CORRECTO:
import com.example.proyecto.data.votaciones.VotarRequest
import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ... (El resto de las data classes VotacionesUi se quedan igual) ...
data class VotacionesUi(
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    val abiertas: List<VotacionDto> = emptyList()
)

class VotacionesViewModel : ViewModel() {
    private val api = ApiClient.apiService
    // ... (Tus variables de estado _ui, _resultados, etc. siguen igual) ...
    private val _ui = MutableStateFlow(VotacionesUi())
    val ui: StateFlow<VotacionesUi> = _ui.asStateFlow()

    private val _resultados = MutableStateFlow<Map<Int, ResultadoVotacionDto>>(emptyMap())
    val resultados: StateFlow<Map<Int, ResultadoVotacionDto>> = _resultados.asStateFlow()

    private var autoJob: Job? = null
    private fun authHeader(token: String) = "Token $token"

    // ... (Función cargarAbiertas y solicitarCodigo siguen igual) ...
    fun cargarAbiertas(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                val resp = api.votacionesAbiertasV1(authHeader(token))
                if (resp.isSuccessful) {
                    _ui.update { it.copy(abiertas = resp.body().orEmpty(), cargando = false) }
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()}", cargando = false) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message, cargando = false) }
            }
        }
    }

    fun solicitarCodigo(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                api.solicitarCodigoVotacion(authHeader(token))
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al pedir código") }
            }
        }
    }

    // 🔥 CORRECCIÓN AQUÍ EN LA FUNCIÓN VOTAR
    fun votar(token: String, votacionId: Int, opcionId: Int, codigo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                // 🟢 USAR LA CLASE CORRECTA: VotarRequest
                val request = VotarRequest(opcionId = opcionId, codigo = codigo)

                val resp = api.votarV1(votacionId, request, authHeader(token))

                if (resp.isSuccessful) {
                    val nuevas = _ui.value.abiertas.map { v ->
                        if (v.id == votacionId) v.copy(yaVote = true, opcionVotadaId = opcionId) else v
                    }
                    _ui.update {
                        it.copy(abiertas = nuevas, mensaje = "¡Voto registrado!", cargando = false)
                    }
                    cargarResultados(token, votacionId)
                } else {
                    val errorMsg = resp.errorBody()?.string() ?: "Error"
                    _ui.update { it.copy(error = "Fallo: $errorMsg", cargando = false) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error: ${e.message}", cargando = false) }
            }
        }
    }

    // ... (Resto de funciones cargarResultados, startAutoRefresh, etc. siguen igual) ...
    fun cargarResultados(token: String, votacionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = api.resultadosVotacionV1(votacionId, authHeader(token))
                if (resp.isSuccessful) {
                    resp.body()?.let { r -> _resultados.update { it + (votacionId to r) } }
                }
            } catch (_: Exception) {}
        }
    }

    fun startAutoRefresh(token: String, periodMs: Long = 10_000L) {
        stopAutoRefresh()
        autoJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val resp = api.votacionesAbiertasV1(authHeader(token))
                    if (resp.isSuccessful) {
                        val nuevas = resp.body().orEmpty()
                        if (nuevas != _ui.value.abiertas) _ui.update { it.copy(abiertas = nuevas) }
                    }
                } catch (_: Exception) {}
                delay(periodMs)
            }
        }
    }

    fun stopAutoRefresh() {
        autoJob?.cancel()
        autoJob = null
    }

    fun clearMessages() {
        _ui.update { it.copy(error = null, mensaje = null) }
    }
}