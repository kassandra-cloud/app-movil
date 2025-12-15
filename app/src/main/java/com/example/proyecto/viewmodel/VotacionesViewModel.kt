package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
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
// 🔥 Imports necesarios para el filtro combinado
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// --------------------------------------------------
// 🔥 Enum para los estados de filtrado
// --------------------------------------------------
enum class VotacionFilter {
    EN_CURSO, // Votaciones abiertas
    CERRADAS, // Votaciones cerradas
    TODAS // Mostrar todas (usado como valor inicial/fallback)
}

// Estado de la pantalla de votaciones
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

    private val _filtro = MutableStateFlow(VotacionFilter.EN_CURSO)
    val filtro: StateFlow<VotacionFilter> = _filtro.asStateFlow()

    private var autoJob: Job? = null

    private fun authHeader(token: String) = "Token $token"

    // --------------------------------------------------
    // 🔥 NUEVO: Lista de votaciones filtrada (se combina con ui y filtro)
    // --------------------------------------------------
    val filteredVotaciones: StateFlow<List<VotacionDto>> = combine(ui, filtro) { ui, currentFilter ->
        when (currentFilter) {
            VotacionFilter.EN_CURSO -> ui.abiertas.filter { it.estaAbierta }
            VotacionFilter.CERRADAS -> ui.abiertas.filter { !it.estaAbierta }
            VotacionFilter.TODAS -> ui.abiertas
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _ui.value.abiertas.filter { it.estaAbierta }
    )

    // --------------------------------------------------
    // 🔥 NUEVO: Función para cambiar el filtro
    // --------------------------------------------------
    fun setFilter(filter: VotacionFilter) {
        _filtro.value = filter
    }

    // --------------------------------------------------
    // Cargar votaciones abiertas
    // --------------------------------------------------
    fun cargarAbiertas(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                val resp = api.votacionesAbiertasV1(authHeader(token))
                if (resp.isSuccessful) {
                    _ui.update {
                        it.copy(
                            abiertas = resp.body().orEmpty(),
                            cargando = false
                        )
                    }
                } else {
                    _ui.update {
                        it.copy(
                            cargando = false,
                            error = "No pudimos cargar las votaciones. Inténtalo nuevamente."
                        )
                    }
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        cargando = false,
                        error = "Error de conexión. Verifica tu internet e inténtalo otra vez."
                    )
                }
            }
        }
    }

    // --------------------------------------------------
    // Solicitar código MFA para votar
    // --------------------------------------------------
    fun solicitarCodigo(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(error = null, mensaje = null) } // Limpiamos mensajes anteriores
            try {
                val resp = api.solicitarCodigoVotacion(authHeader(token))
                if (resp.isSuccessful) {
                    // Mensaje de éxito para el reenvío
                    _ui.update {
                        it.copy(
                            mensaje = "Se ha reenviado el código de verificación a tu correo."
                        )
                    }
                } else {
                    _ui.update {
                        it.copy(
                            error = "No pudimos enviar el código. Revisa tu correo y vuelve a intentarlo."
                        )
                    }
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        error = "Error al pedir el código de verificación. Intenta nuevamente."
                    )
                }
            }
        }
    }

    // --------------------------------------------------
    // Enviar voto
    // --------------------------------------------------
    fun votar(token: String, votacionId: Int, opcionId: Int, codigo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargando = true, error = null, mensaje = null) }
            try {
                val request = VotarRequest(opcionId = opcionId, codigo = codigo)
                val resp = api.votarV1(votacionId, request, authHeader(token))

                if (resp.isSuccessful) {
                    // Actualizamos la votación en memoria como "ya votó"
                    val nuevas = _ui.value.abiertas.map { v ->
                        if (v.id == votacionId)
                            v.copy(yaVote = true, opcionVotadaId = opcionId)
                        else
                            v
                    }

                    _ui.update {
                        it.copy(
                            abiertas = nuevas,
                            mensaje = "¡Tu voto fue registrado correctamente! 🎉",
                            cargando = false
                        )
                    }

                    // Recargar resultados parciales
                    cargarResultados(token, votacionId)

                } else {
                    val errorRaw = resp.errorBody()?.string().orEmpty()

                    // Mapeamos el texto “técnico” a un mensaje amigable
                    val mensajeLindo = when {
                        "Código incorrecto o expirado" in errorRaw ->
                            "El código ingresado es incorrecto o ya expiró. Solicita uno nuevo e inténtalo nuevamente."
                        "La votación ya cerró" in errorRaw ->
                            "Esta votación ya se encuentra cerrada."
                        else ->
                            "No pudimos registrar tu voto. Inténtalo nuevamente en unos minutos."
                    }

                    _ui.update {
                        it.copy(
                            error = mensajeLindo,
                            cargando = false
                        )
                    }
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        cargando = false,
                        error = "Ocurrió un problema al enviar tu voto. Revisa tu conexión e inténtalo otra vez."
                    )
                }
            }
        }
    }

    // --------------------------------------------------
    // Resultados parciales (CORREGIDO Y ROBUSTO)
    // --------------------------------------------------
    fun cargarResultados(token: String, votacionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = api.resultadosVotacionV1(votacionId, authHeader(token))
                if (resp.isSuccessful) {
                    resp.body()?.let { r ->
                        _resultados.update { it + (votacionId to r) }
                    }
                } else {
                    // Si la respuesta HTTP no es exitosa (ej. 404, 500)
                    _ui.update {
                        it.copy(
                            error = "API Falló (${resp.code()}). Resultados no disponibles."
                        )
                    }
                    // Limpiamos la entrada del mapa para cerrar el spinner
                    _resultados.update { it - votacionId }
                }
            } catch (e: Exception) {
                // Notificamos el error de conexión o de serialización (JSON Mismatch)
                _ui.update {
                    it.copy(
                        error = "Error al procesar datos: ${e.message?.take(50)}"
                    )
                }
                // Limpiamos la entrada del mapa para cerrar el spinner
                _resultados.update { it - votacionId }
            }
        }
    }

    // --------------------------------------------------
    // Auto-refresh de votaciones
    // --------------------------------------------------
    fun startAutoRefresh(token: String, periodMs: Long = 10_000L) {
        stopAutoRefresh()
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
                }
                delay(periodMs)
            }
        }
    }

    fun stopAutoRefresh() {
        autoJob?.cancel()
        autoJob = null
    }

    // --------------------------------------------------
    // Limpiar mensajes (para cuando se cierra el snackbar/alerta)
    // --------------------------------------------------
    fun clearMessages() {
        _ui.update { it.copy(error = null, mensaje = null) }
    }
}