package com.example.proyecto.viewmodel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import kotlinx.coroutines.launch
import com.example.proyecto.api.ForoApi
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.ComentarioCrearRequest
data class ForoUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val publicaciones: List<PublicacionDto> = emptyList(),
    // para saber si se está posteando comentario en una publicación concreta
    val posting: Map<Int, Boolean> = emptyMap(),
    // errores por publicación al comentar
    val postError: Map<Int, String?> = emptyMap()
)


class ForoViewModel(
    private val api: ForoApi = ApiClient.foroApi
) : ViewModel() {
    var uiState by mutableStateOf(ForoUiState())
        private set

    fun cargar(token: String) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(cargando = true, error = null)

                // La API devuelve una lista de publicaciones con comentarios embebidos
                val publicaciones: List<PublicacionDto> =
                    api.listar("Token $token")

                uiState = uiState.copy(
                    cargando = false,
                    publicaciones = publicaciones
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    cargando = false,
                    error = "Error al cargar el foro: ${e.message}"
                )
            }
        }
    }

    fun comentar(token: String, publicacionId: Int, texto: String) {
        if (texto.isBlank()) return

        viewModelScope.launch {
            // marcar que estamos publicando en esa publicación
            uiState = uiState.copy(
                posting = uiState.posting + (publicacionId to true),
                postError = uiState.postError + (publicacionId to null)
            )

            try {
                api.comentar(
                    auth = "Token $token",
                    publicacionId = publicacionId,
                    body = ComentarioCrearRequest(texto = texto)   // <- usamos la data class verdadera
                )
                // opción simple: recargar todo el foro (1 llamada)
                val publicacionesActualizadas: List<PublicacionDto> =
                    api.listar("Token $token")

                uiState = uiState.copy(
                    publicaciones = publicacionesActualizadas
                )

            } catch (e: Exception) {
                uiState = uiState.copy(
                    postError = uiState.postError + (publicacionId to "No se pudo enviar el comentario")
                )
            } finally {
                uiState = uiState.copy(
                    posting = uiState.posting + (publicacionId to false)
                )
            }
        }
    }

    fun limpiarErrorGlobal() {
        uiState = uiState.copy(error = null)
    }
}