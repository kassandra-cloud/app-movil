package com.example.proyecto.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.reuniones.ActaDto
import com.example.proyecto.data.reuniones.AsistenciaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class ActasViewModel : ViewModel() {

    // ---- Estado principal ----
    private val _actas = MutableStateFlow<List<ActaDto>>(emptyList())
    val actas: StateFlow<List<ActaDto>> = _actas.asStateFlow()

    // Mapa: idReunion -> lista de asistencias (para pintar ✅ / ❌)
    private val _asistencias = MutableStateFlow<Map<Int, List<AsistenciaDto>>>(emptyMap())
    val asistencias: StateFlow<Map<Int, List<AsistenciaDto>>> = _asistencias.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Para recordar el último filtro de búsqueda
    private var lastSearch: String? = null

    // ---- Acciones ----
    fun cargarActas(search: String? = null) {
        lastSearch = search
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                // 1) Cargar actas (paginadas)
                val page = ApiClient.reunionesApi.listarActas(search = search)
                val lista = page.results ?: emptyList()
                _actas.value = lista

                // 2) Cargar TODAS MIS asistencias en UNA sola llamada
                val resp = ApiClient.reunionesApi.listarMisAsistencias(pageSize = 500)

                val misAsistencias: List<AsistenciaDto> =
                    if (resp.isSuccessful) resp.body() ?: emptyList()
                    else {
                        Log.e("ActasVM", "HTTP ${resp.code()} al cargar mis asistencias")
                        emptyList()
                    }

                // 3) Mapear por id de reunión: reunionId -> lista de asistencias mías (normalmente 1)
                val mapAgrupado: Map<Int, List<AsistenciaDto>> =
                    misAsistencias.groupBy { it.reunion }

                _asistencias.value = mapAgrupado

            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando actas"
            } finally {
                _loading.value = false
            }
        }
    }

    fun refrescar() = cargarActas(lastSearch)

    fun limpiarError() {
        _error.value = null
    }

    // ---- Utilidades para la UI ----

    /** Cuenta cuántos presentes hay para una reunión. Maneja 'presente' como Boolean? */
    fun presentes(reunionId: Int): Int =
        _asistencias.value[reunionId]?.count { it.presente == true } ?: 0

    /** Cuenta cuántos ausentes hay para una reunión. Maneja 'presente' como Boolean? */
    fun ausentes(reunionId: Int): Int =
        _asistencias.value[reunionId]?.count { it.presente == false } ?: 0
}
