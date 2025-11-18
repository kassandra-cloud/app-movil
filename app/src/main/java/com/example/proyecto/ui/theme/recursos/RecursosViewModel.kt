package com.example.proyecto.ui.theme.recursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.RecursosApi
import com.example.proyecto.data.recursos.CrearSolicitudReq
import com.example.proyecto.data.recursos.RecursoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecursosViewModel(private val recursosApi: RecursosApi) : ViewModel() {

    private val _recursos = MutableStateFlow<List<RecursoDto>>(emptyList())
    val recursos: StateFlow<List<RecursoDto>> = _recursos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _reservaMessage = MutableStateFlow<String?>(null)
    val reservaMessage: StateFlow<String?> = _reservaMessage



    fun cargarRecursos(search: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // disponible = null permite que el backend calcule el estado real
                val response = recursosApi.listarRecursos(
                    disponible = null,
                    search = search,
                    page = 1,
                    pageSize = 20
                )
                _recursos.value = response.results
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar recursos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun crearSolicitud(req: CrearSolicitudReq) {
        viewModelScope.launch {
            _isLoading.value = true
            _reservaMessage.value = null
            try {
                val response = recursosApi.crearSolicitud(req)
                _reservaMessage.value = "Solicitud enviada con éxito para ${response.recursoNombre}"

                // Recargar la lista inmediatamente para bloquear el botón si es necesario
                cargarRecursos()

            } catch (e: Exception) {
                _reservaMessage.value = "Error: ${e.message}. Verifica fechas o duplicados."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearReservaMessage() {
        _reservaMessage.value = null
    }
}