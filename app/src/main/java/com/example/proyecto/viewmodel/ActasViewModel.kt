package com.example.proyecto.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.ActaDto
import com.example.proyecto.data.AsistenciaDto
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

                // 2) Limpiar y cargar asistencias por reunión (en paralelo)
                _asistencias.value = emptyMap()

                lista.forEach { acta ->
                    viewModelScope.launch {
                        val reunionId = acta.reunion
                        try {
                            val resp: Response<List<AsistenciaDto>> =
                                ApiClient.reunionesApi.listarAsistencias(
                                    reunionId = reunionId,
                                    pageSize = 200
                                )

                            val list: List<AsistenciaDto> =
                                if (resp.isSuccessful) resp.body() ?: emptyList()
                                else {
                                    Log.e("ActasVM", "HTTP ${resp.code()} asistencias reunion=$reunionId")
                                    emptyList()
                                }

                            // Actualiza siempre el mapa (evita “Cargando…” infinito)
                            _asistencias.value = _asistencias.value + (reunionId to list)

                        } catch (e: Exception) {
                            Log.e("ActasVM", "Error asistencias reunion=$reunionId", e)
                            _asistencias.value = _asistencias.value + (reunionId to emptyList())
                        }
                    }
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando actas"
            } finally {
                _loading.value = false
            }
        }
    }

    fun refrescar() = cargarActas(lastSearch)
    fun limpiarError() { _error.value = null }

    // ---- Utilidades para la UI ----
    fun presentes(reunionId: Int): Int =
        _asistencias.value[reunionId]?.count { it.presente } ?: 0

    fun ausentes(reunionId: Int): Int =
        _asistencias.value[reunionId]?.count { !it.presente } ?: 0
}
