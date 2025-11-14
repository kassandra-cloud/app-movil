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

    private val _actas = MutableStateFlow<List<ActaDto>>(emptyList())
    val actas: StateFlow<List<ActaDto>> = _actas

    // Almacena las asistencias por ID de reunión (reunionId -> List<AsistenciaDto>)
    private val _asistencias = MutableStateFlow<Map<Int, List<AsistenciaDto>>>(emptyMap())
    val asistencias: StateFlow<Map<Int, List<AsistenciaDto>>> = _asistencias

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Carga actas y asistencias, utilizando el token para la autorización.
     * Esto resuelve el error 401 si la API de Reuniones es privada.
     */
    fun cargarActas(token: String) { // 💡 Recibir token aquí
        viewModelScope.launch {
            if (token.isBlank()) {
                _error.value = "Sesión no válida. Por favor, inicie sesión."
                return@launch
            }

            _loading.value = true
            _error.value = null

            try {
                // 💡 CORRECCIÓN: Crear el cliente autorizado en el scope
                val api: ReunionesApi = ApiClient.createAuthorized(token, ReunionesApi::class.java)

                // 1. Cargar Actas
                // (Asumiendo que ReunionesApi tiene una función para listar actas)
                val actasList = api.getActas()
                _actas.value = actasList

                // 2. Cargar Asistencias (Esto es solo un placeholder, la lógica real de API variará)
                // Usamos un cliente de API para cargar asistencias (asumiendo que la API tiene un endpoint para esto)
                // Si tienes un endpoint para TODAS las asistencias de un usuario o por reunión:
                // val asistenciasData = api.getAsistencias().groupBy { it.reunion }

                // Placeholder para evitar errores de compilación por DTOs
                val asistenciasData = emptyMap<Int, List<AsistenciaDto>>()

                _asistencias.value = asistenciasData

            } catch (e: Exception) {
                // Manejo de error de red o serialización
                _error.value = "Error al cargar actas: ${e.message}. Verifique la conexión o el token."
            } finally {
                _loading.value = false
            }
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}