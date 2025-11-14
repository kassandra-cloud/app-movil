package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.VotoRequest
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
import retrofit2.Response

data class VotacionesUi(
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    val abiertas: List<VotacionDto> = emptyList()
)

class VotacionesViewModel : ViewModel() {
    private val api = ApiClient.apiService

    private val _ui = MutableStateFlow(VotacionesUi())
    val ui: StateFlow<VotacionesUi> = _ui.asStateFlow()

    private val _resultados = MutableStateFlow<Map<Int, ResultadoVotacionDto>>(emptyMap())
    val resultados: StateFlow<Map<Int, ResultadoVotacionDto>> = _resultados.asStateFlow()

    private var autoJob: Job? = null

    private fun authHeader(token: String) = "Token $token" // o "Bearer $token"

    /** Primer fetch manual o por pull-to-refresh */
    fun cargarAbiertas(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                val resp = api.votacionesAbiertasV1(authHeader(token))
                if (resp.isSuccessful) {
                    _ui.update { it.copy(abiertas = resp.body().orEmpty(), cargando = false) }
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()} al cargar votaciones", cargando = false) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al cargar votaciones: ${e.message}", cargando = false) }
            }
        }
    }

    /** Refresco automático en segundo plano mientras la pantalla esté visible */
    fun startAutoRefresh(token: String, periodMs: Long = 10_000L) {
        autoJob?.cancel()
        autoJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val resp = api.votacionesAbiertasV1(authHeader(token))
                    if (resp.isSuccessful) {
                        val nuevas = resp.body().orEmpty()
                        if (nuevas != _ui.value.abiertas) {
                            _ui.update { it.copy(abiertas = nuevas) }
                        }
                    }
                } catch (_: Exception) {
                    // Ignorar errores intermitentes de red en el refresco automático
                }
                delay(periodMs)
            }
        }
    }

    fun stopAutoRefresh() {
        autoJob?.cancel()
        autoJob = null
    }

    fun cargarResultados(token: String, votacionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = api.resultadosVotacionV1(votacionId, authHeader(token))
                if (resp.isSuccessful) {
                    resp.body()?.let { r -> _resultados.update { it + (votacionId to r) } }
                } else {
                    _ui.update { it.copy(error = "Error resultados: ${resp.code()}") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error resultados: ${e.message}") }
            }
        }
    }

    fun votar(token: String, votacionId: Int, opcionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                // El VotoRequest usa 'opcion_id', lo cual es correcto para el backend.
                val resp = api.votarV1(votacionId, VotoRequest(opcion_id = opcionId), authHeader(token))
                if (resp.isSuccessful) {
                    val nuevas = _ui.value.abiertas.map { v ->
                        // ⬅️ CORRECCIÓN: Usar yaVote y opcionVotadaId
                        if (v.id == votacionId) v.copy(yaVote = true, opcionVotadaId = opcionId) else v
                    }
                    _ui.update { it.copy(abiertas = nuevas, mensaje = "¡Voto registrado!", cargando = false) }
                    cargarResultados(token, votacionId) // refresca resultados opcional
                } else {
                    _ui.update { it.copy(error = "No se pudo votar (${resp.code()})", cargando = false) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al votar: ${e.message}", cargando = false) }
            }
        }
    }

    // (Opcional, por si lo usas en algún flujo)
    private fun <T> errorResp(resp: Response<T>, accion: String): Nothing {
        throw IllegalStateException("Error ${resp.code()} al $accion")
    }
}