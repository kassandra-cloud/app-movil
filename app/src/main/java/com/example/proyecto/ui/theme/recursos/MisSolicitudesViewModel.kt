package com.example.proyecto.ui.theme.recursos

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.RecursosApi
import com.example.proyecto.data.recursos.SolicitudDto
import kotlinx.coroutines.launch

data class MisSolState(
    val cargando: Boolean = false,
    val solicitudes: List<SolicitudDto> = emptyList(),
    val error: String? = null
)

class MisSolicitudesViewModel(private val token: String) : ViewModel() {
    private val api by lazy { ApiClient.getClient(token).create(RecursosApi::class.java) }
    var ui = mutableStateOf(MisSolState())
        private set

    fun cargar() {
        ui.value = ui.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                val resp = api.misSolicitudes()
                ui.value = ui.value.copy(cargando = false, solicitudes = resp.results)
            } catch (e: Exception) {
                ui.value = ui.value.copy(cargando = false, error = e.message ?: "Error")
            }
        }
    }
}

class MisSolicitudesFactory(private val token: String): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MisSolicitudesViewModel(token) as T
    }
}
