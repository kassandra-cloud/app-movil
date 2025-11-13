package com.example.proyecto.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.Page
import com.example.proyecto.data.reuniones.ReunionDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReunionesViewModel : ViewModel() {

    enum class ReunionEstado { PROGRAMADA, EN_CURSO, REALIZADA }

    data class SectionState(
        val items: List<ReunionDto> = emptyList(),
        val page: Int = 1,
        val hasNext: Boolean = false,
        val loading: Boolean = false,
        val error: String? = null,
        val initialized: Boolean = false      // 👈 NUEVO
    )

    private val _state = MutableStateFlow(
        mapOf(
            ReunionEstado.PROGRAMADA to SectionState(),
            ReunionEstado.EN_CURSO   to SectionState(),
            ReunionEstado.REALIZADA  to SectionState()
        )
    )
    val state: StateFlow<Map<ReunionEstado, SectionState>> = _state.asStateFlow()

    // Slices seguros (sin fugas)
    val programadas: StateFlow<SectionState> = state
        .map { it[ReunionEstado.PROGRAMADA] ?: SectionState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SectionState())

    val enCurso: StateFlow<SectionState> = state
        .map { it[ReunionEstado.EN_CURSO] ?: SectionState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SectionState())

    val realizadas: StateFlow<SectionState> = state
        .map { it[ReunionEstado.REALIZADA] ?: SectionState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SectionState())

    private fun estadoParam(e: ReunionEstado): String = when (e) {
        ReunionEstado.PROGRAMADA -> "programada"
        ReunionEstado.EN_CURSO   -> "en_curso"
        ReunionEstado.REALIZADA  -> "realizada"
    }

    private fun update(e: ReunionEstado, reducer: (SectionState) -> SectionState) {
        val current = _state.value.toMutableMap()
        current[e] = reducer(current[e] ?: SectionState())
        _state.value = current
    }
    /** Carga solo si la sección aún no ha sido inicializada. */
    fun ensureLoaded(estado: ReunionEstado, pageSize: Int = 20) {
        val s = _state.value[estado] ?: SectionState()
        if (s.initialized || s.loading) return   // ya hay datos o se está cargando
        refresh(estado, pageSize)
    }
    /** Carga inicial o recarga completa del estado dado (reinicia a page=1). */
    fun refresh(estado: ReunionEstado, pageSize: Int = 20) {
        load(estado, page = 1, pageSize = pageSize, reset = true)
    }

    /** Carga “siguiente página” si existe. */
    fun nextPage(estado: ReunionEstado, pageSize: Int = 20) {
        val s = _state.value[estado] ?: return
        if (!s.hasNext || s.loading) return
        load(estado, page = s.page + 1, pageSize = pageSize, reset = false)
    }

    /** Limpia el error visible de una sección. */
    fun clearError(estado: ReunionEstado) {
        update(estado) { it.copy(error = null) }
    }

    // Shortcuts
    fun cargarProgramadas(pageSize: Int = 20) = refresh(ReunionEstado.PROGRAMADA, pageSize)
    fun cargarEnCurso(pageSize: Int = 20)     = refresh(ReunionEstado.EN_CURSO, pageSize)
    fun cargarRealizadas(pageSize: Int = 20)  = refresh(ReunionEstado.REALIZADA, pageSize)

    private fun load(
        estado: ReunionEstado,
        page: Int,
        pageSize: Int,
        reset: Boolean
    ) {
        viewModelScope.launch {
            update(estado) { it.copy(loading = true, error = null, page = if (reset) 1 else it.page) }
            try {
                val resp: Page<ReunionDto> = ApiClient.reunionesApi.listarReuniones(
                    estado = estadoParam(estado),
                    ordering = "-fecha",
                    page = page,
                    pageSize = pageSize
                )
                update(estado) { prev ->
                    val merged = if (reset) resp.results else prev.items + resp.results
                    prev.copy(
                        items   = merged,
                        page    = page,
                        hasNext = resp.next != null,
                        loading = false,
                        error   = null,
                        initialized = true          // 👈 MARCAMOS COMO CARGADO
                    )
                }
            } catch (e: Exception) {
                update(estado) { it.copy(loading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }
}
