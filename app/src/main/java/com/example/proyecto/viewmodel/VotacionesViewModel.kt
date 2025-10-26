package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.VotoRequest
import com.example.proyecto.data.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private fun authHeader(token: String) = "Token $token" // o "Bearer $token"

    fun cargarAbiertas(token: String) {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                val resp = api.votacionesAbiertasV1(authHeader(token))
                if (resp.isSuccessful) {
                    _ui.update { it.copy(abiertas = resp.body().orEmpty()) }
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()} al cargar votaciones") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al cargar votaciones: ${e.message}") }
            } finally {
                _ui.update { it.copy(cargando = false) }
            }
        }
    }

    fun cargarResultados(token: String, votacionId: Int) {
        viewModelScope.launch {
            try {
                val resp = api.resultadosVotacionV1(votacionId, authHeader(token))
                if (resp.isSuccessful) {
                    resp.body()?.let { r -> _resultados.update { it + (votacionId to r) } }
                } else {
                    _ui.update { it.copy(error = "Error resultados: Error ${resp.code()} al obtener resultados") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error resultados: ${e.message}") }
            }
        }
    }

    fun votar(token: String, votacionId: Int, opcionId: Int) {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                val resp = api.votarV1(votacionId, VotoRequest(opcion_id = opcionId), authHeader(token))
                if (resp.isSuccessful) {
                    val nuevas = _ui.value.abiertas.map { v ->
                        if (v.id == votacionId) v.copy(ya_vote = true, opcion_votada_id = opcionId) else v
                    }
                    _ui.update { it.copy(abiertas = nuevas, mensaje = "¡Voto registrado!") }
                    // opcional: refrescar resultados tras votar
                    cargarResultados(token, votacionId)
                } else {
                    _ui.update { it.copy(error = "No se pudo votar (${resp.code()})") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al votar: ${e.message}") }
            } finally {
                _ui.update { it.copy(cargando = false) }
            }
        }
    }

    private fun <T> errorResp(resp: Response<T>, accion: String): Nothing {
        throw IllegalStateException("Error ${resp.code()} al $accion")
    }
}
