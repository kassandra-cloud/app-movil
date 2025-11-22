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
import kotlin.math.max

// Estado de la UI
data class ForoUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val publicaciones: List<PublicacionDto> = emptyList(),
    val posting: Map<Int, Boolean> = emptyMap(), // Para spinners de carga individuales
    val postError: Map<Int, String?> = emptyMap() // Errores individuales
)

class ForoViewModel : ViewModel() {
    var uiState by mutableStateOf(ForoUiState())
        private set

    /**
     * Carga la lista de publicaciones del foro.
     */
    fun cargar(token: String) {
        viewModelScope.launch {
            if (token.isBlank()) {
                uiState = uiState.copy(error = "Token de sesión no disponible")
                return@launch
            }

            uiState = uiState.copy(cargando = true, error = null)

            try {
                val authorizedApi = ApiClient.createAuthorized(token, ForoApi::class.java)
                val publicaciones = authorizedApi.listar()

                uiState = uiState.copy(
                    cargando = false,
                    publicaciones = publicaciones
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    cargando = false,
                    error = "Error al cargar el foro. Asegúrese de tener conexión."
                )
            }
        }
    }

    /**
     * Envía un comentario o una respuesta a una publicación.
     * @param parentId Opcional. Si se envía, el comentario será una respuesta.
     */
    fun comentar(token: String, publicacionId: Int, texto: String, parentId: Int? = null) {
        if (texto.isBlank() || token.isBlank()) return

        viewModelScope.launch {
            // Marcamos que se está posteando en ESTA publicación específica
            uiState = uiState.copy(
                posting = uiState.posting + (publicacionId to true),
                postError = uiState.postError + (publicacionId to null)
            )

            val authorizedApi = ApiClient.createAuthorized(token, ForoApi::class.java)

            try {
                authorizedApi.comentar(
                    publicacionId = publicacionId,
                    body = ComentarioCrearRequest(texto = texto, parent = parentId)
                )

                // Recargamos la lista para ver el nuevo comentario
                val publicacionesActualizadas = authorizedApi.listar()
                uiState = uiState.copy(publicaciones = publicacionesActualizadas)

            } catch (e: Exception) {
                uiState = uiState.copy(
                    postError = uiState.postError + (publicacionId to "No se pudo enviar: ${e.message}")
                )
            } finally {
                uiState = uiState.copy(
                    posting = uiState.posting + (publicacionId to false)
                )
            }
        }
    }

    /**
     * Elimina un comentario propio.
     */
    fun eliminarComentario(token: String, comentarioId: Int, publicacionId: Int) {
        if (token.isBlank()) return

        viewModelScope.launch {
            try {
                val authorizedApi = ApiClient.createAuthorized(token, ForoApi::class.java)
                authorizedApi.eliminarComentario(comentarioId)

                // Recargamos para reflejar la eliminación
                val publicacionesActualizadas = authorizedApi.listar()
                uiState = uiState.copy(publicaciones = publicacionesActualizadas)

            } catch (e: Exception) {
                e.printStackTrace()
                val mensajeError = if (e.message?.contains("403") == true) {
                    "No tienes permiso para eliminar esto."
                } else {
                    "Error al eliminar: ${e.message}"
                }
                uiState = uiState.copy(error = mensajeError)
            }
        }
    }

    /**
     * Da o quita "Me gusta" a un comentario.
     * Usa actualización optimista (cambia visualmente antes de confirmar con el servidor).
     */
    fun toggleLike(token: String, comentarioId: Int, publicacionId: Int) {
        if (token.isBlank()) return

        viewModelScope.launch {
            // 1. Actualización visual inmediata
            actualizarLikeLocalmente(publicacionId, comentarioId)

            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                val response = api.toggleLike(comentarioId)

                // Si falla el servidor, revertimos el cambio visual
                if (!response.isSuccessful) {
                    actualizarLikeLocalmente(publicacionId, comentarioId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Si hay error de red, revertimos
                actualizarLikeLocalmente(publicacionId, comentarioId)
            }
        }
    }

    // Función auxiliar para cambiar el like en la lista local sin recargar todo
    private fun actualizarLikeLocalmente(pubId: Int, comId: Int) {
        val listaPubs = uiState.publicaciones.toMutableList()
        val indexPub = listaPubs.indexOfFirst { it.id == pubId }

        if (indexPub != -1) {
            val pub = listaPubs[indexPub]
            val listaComs = pub.comentarios.toMutableList()
            val indexCom = listaComs.indexOfFirst { it.id == comId }

            if (indexCom != -1) {
                val c = listaComs[indexCom]

                // Invertimos el estado actual
                val nuevoLike = !c.meGustaUsuario
                // Ajustamos el contador (+1 o -1)
                val nuevoTotal = if (nuevoLike) c.totalLikes + 1 else max(0, c.totalLikes - 1)

                listaComs[indexCom] = c.copy(
                    meGustaUsuario = nuevoLike,
                    totalLikes = nuevoTotal
                )

                listaPubs[indexPub] = pub.copy(comentarios = listaComs)
                uiState = uiState.copy(publicaciones = listaPubs)
            }
        }
    }

    /**
     * Sube un archivo (audio o imagen) a la publicación.
     */
    fun enviarArchivo(token: String, publicacionId: Int, file: File, mimeType: String) {
        viewModelScope.launch {
            if (token.isBlank()) return@launch

            uiState = uiState.copy(
                posting = uiState.posting + (publicacionId to true),
                postError = uiState.postError + (publicacionId to null)
            )

            val authorizedApi = ApiClient.createAuthorized(token, ForoApi::class.java)

            try {
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val archivoPart = MultipartBody.Part.createFormData(
                    "archivo",
                    file.name,
                    requestFile
                )

                authorizedApi.subirAdjunto(
                    publicacionId = publicacionId,
                    archivo = archivoPart,
                    esMensaje = "true"
                )

                val publicacionesActualizadas = authorizedApi.listar()
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