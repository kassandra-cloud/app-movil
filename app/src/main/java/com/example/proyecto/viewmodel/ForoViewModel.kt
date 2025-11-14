package com.example.proyecto.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.ForoApi
import com.example.proyecto.data.ComentarioCrearRequest
import com.example.proyecto.data.PublicacionDto
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

// Define el estado de la interfaz de usuario del foro
data class ForoUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val publicaciones: List<PublicacionDto> = emptyList(),
    // para saber si se está posteando comentario en una publicación concreta
    val posting: Map<Int, Boolean> = emptyMap(),
    // errores por publicación al comentar
    val postError: Map<Int, String?> = emptyMap()
)

// 💡 CORRECCIÓN: La API no se inyecta por defecto; se crea autorizada en cada función.
class ForoViewModel : ViewModel() {
    var uiState by mutableStateOf(ForoUiState())
        private set

    /**
     * Carga las publicaciones del foro.
     * Utiliza un cliente autorizado para resolver el error 401.
     */
    fun cargar(token: String) {
        viewModelScope.launch {
            if (token.isBlank()) {
                uiState = uiState.copy(error = "Token de sesión no disponible")
                return@launch
            }

            uiState = uiState.copy(cargando = true, error = null)

            try {
                // 💡 Crea la instancia de API AUTORIZADA con el token
                val authorizedApi: ForoApi = ApiClient.createAuthorized(token, ForoApi::class.java)

                // La llamada es limpia; el token se maneja en el cliente HTTP
                val publicaciones: List<PublicacionDto> = authorizedApi.listar()

                uiState = uiState.copy(
                    cargando = false,
                    publicaciones = publicaciones
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    cargando = false,
                    error = "Error al cargar el foro. Asegúrese de tener conexión o inicie sesión de nuevo."
                )
            }
        }
    }

    /**
     * Publica un comentario de texto en una publicación.
     */
    fun comentar(token: String, publicacionId: Int, texto: String) {
        if (texto.isBlank() || token.isBlank()) return

        viewModelScope.launch {
            uiState = uiState.copy(
                posting = uiState.posting + (publicacionId to true),
                postError = uiState.postError + (publicacionId to null)
            )

            val authorizedApi: ForoApi = ApiClient.createAuthorized(token, ForoApi::class.java)

            try {
                authorizedApi.comentar(
                    publicacionId = publicacionId,
                    body = ComentarioCrearRequest(texto = texto)
                )

                // Recargar el foro para ver el nuevo comentario
                val publicacionesActualizadas: List<PublicacionDto> = authorizedApi.listar()

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

    /**
     * Sube un archivo (audio o imagen) a la publicación (Lógica de WhatsApp/Instagram).
     */
    fun enviarArchivo(token: String, publicacionId: Int, file: File, mimeType: String) {
        viewModelScope.launch {
            if (token.isBlank()) return@launch

            uiState = uiState.copy(
                posting = uiState.posting + (publicacionId to true),
                postError = uiState.postError + (publicacionId to null)
            )

            val authorizedApi: ForoApi = ApiClient.createAuthorized(token, ForoApi::class.java)

            try {
                // 1. Crear el RequestBody y la parte Multipart para el archivo
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val archivoPart = MultipartBody.Part.createFormData(
                    "archivo", // Nombre de campo esperado por Django
                    file.name,
                    requestFile
                )

                // 2. Llamar a la API
                authorizedApi.subirAdjunto(
                    publicacionId = publicacionId,
                    archivo = archivoPart,
                    // Se envía el string "true" para la bandera es_mensaje del backend
                    esMensaje = "true"
                )

                // Recargar el foro para ver el nuevo adjunto
                val publicacionesActualizadas: List<PublicacionDto> =
                    authorizedApi.listar()

                uiState = uiState.copy(
                    publicaciones = publicacionesActualizadas,
                    postError = uiState.postError + (publicacionId to null)
                )

            } catch (e: Exception) {
                uiState = uiState.copy(
                    postError = uiState.postError + (publicacionId to "Error al subir archivo: ${e.message}")
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