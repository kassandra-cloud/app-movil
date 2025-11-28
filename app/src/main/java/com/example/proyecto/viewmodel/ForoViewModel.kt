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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.math.max
import android.content.Context
import android.net.Uri
import com.example.proyecto.utils.uriToFile
import com.example.proyecto.utils.getMimeType
import android.util.Log


// Estado de la UI
data class ForoUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val publicaciones: List<PublicacionDto> = emptyList(),
    val posting: Map<Int, Boolean> = emptyMap(),
    val postError: Map<Int, String?> = emptyMap()
)

class ForoViewModel : ViewModel() {
    var uiState by mutableStateOf(ForoUiState())
        private set

    fun cargar(token: String) {
        viewModelScope.launch {
            if (token.isBlank()) {
                uiState = uiState.copy(error = "Token no disponible")
                return@launch
            }
            uiState = uiState.copy(cargando = true, error = null)
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                val pubs = api.listar()
                uiState = uiState.copy(cargando = false, publicaciones = pubs)
            } catch (e: Exception) {
                uiState = uiState.copy(cargando = false, error = "Error al cargar: ${e.message}")
            }
        }
    }

    fun comentar(token: String, publicacionId: Int, texto: String, parentId: Int? = null) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            setPostingState(publicacionId, true)
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                api.comentar(publicacionId, ComentarioCrearRequest(texto, parentId))
                recargarLista(api)
            } catch (e: Exception) {
                setErrorState(publicacionId, "Error: ${e.message}")
            } finally {
                setPostingState(publicacionId, false)
            }
        }
    }

    fun enviarArchivo(
        token: String,
        publicacionId: Int,
        file: File,
        mimeType: String,
        textoCaption: String? = null
    ) {
        viewModelScope.launch {
            setPostingState(publicacionId, true)
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)

                val requestFile = file.asRequestBody(mimeType.toMediaType())
                val archivoPart =
                    MultipartBody.Part.createFormData("archivo", file.name, requestFile)
                val esMensajePart = "true".toRequestBody("text/plain".toMediaType())
                val descripcionPart = textoCaption?.toRequestBody("text/plain".toMediaType())

                api.subirAdjunto(
                    publicacionId = publicacionId,
                    archivo = archivoPart,
                    esMensaje = esMensajePart,
                    descripcion = descripcionPart
                )
                recargarLista(api)
            } catch (e: Exception) {
                setErrorState(publicacionId, "Error subir: ${e.message}")
            } finally {
                setPostingState(publicacionId, false)
            }
        }
    }

    fun eliminarComentario(token: String, comentarioId: Int, publicacionId: Int) {
        viewModelScope.launch {
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                api.eliminarComentario(comentarioId)
                recargarLista(api)
            } catch (e: Exception) { /* Manejo error */
            }
        }
    }

    fun eliminarAdjunto(token: String, adjuntoId: Int, publicacionId: Int) {
        viewModelScope.launch {
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                api.eliminarAdjunto(adjuntoId)
                recargarLista(api)
            } catch (e: Exception) { /* Manejo error */
            }
        }
    }

    fun toggleLike(token: String, comentarioId: Int, publicacionId: Int) {
        viewModelScope.launch {
            actualizarLikeLocalmente(publicacionId, comentarioId, esAdjunto = false)
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                val res = api.toggleLike(comentarioId)
                if (!res.isSuccessful) actualizarLikeLocalmente(
                    publicacionId,
                    comentarioId,
                    esAdjunto = false
                )
            } catch (e: Exception) {
                actualizarLikeLocalmente(publicacionId, comentarioId, esAdjunto = false)
            }
        }
    }

    fun toggleLikeAdjunto(token: String, adjuntoId: Int, publicacionId: Int) {
        viewModelScope.launch {
            actualizarLikeLocalmente(publicacionId, adjuntoId, esAdjunto = true)
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)
                val res = api.toggleLikeAdjunto(adjuntoId)
                if (!res.isSuccessful) actualizarLikeLocalmente(
                    publicacionId,
                    adjuntoId,
                    esAdjunto = true
                )
            } catch (e: Exception) {
                actualizarLikeLocalmente(publicacionId, adjuntoId, esAdjunto = true)
            }
        }
    }

    private suspend fun recargarLista(api: ForoApi) {
        val pubs = api.listar()
        uiState = uiState.copy(publicaciones = pubs)
    }

    private fun setPostingState(pubId: Int, loading: Boolean) {
        uiState = uiState.copy(posting = uiState.posting + (pubId to loading))
        if (loading) uiState = uiState.copy(postError = uiState.postError + (pubId to null))
    }

    private fun setErrorState(pubId: Int, msg: String) {
        uiState = uiState.copy(postError = uiState.postError + (pubId to msg))
    }

    // Manejo seguro de listas nulas
    private fun actualizarLikeLocalmente(pubId: Int, itemId: Int, esAdjunto: Boolean) {
        val listaPubs = uiState.publicaciones.toMutableList()
        val indexPub = listaPubs.indexOfFirst { it.id == pubId }
        if (indexPub == -1) return

        val pub = listaPubs[indexPub]

        if (esAdjunto) {
            // Usamos (pub.adjuntos ?: emptyList()) para evitar el error
            val lista = (pub.adjuntos ?: emptyList()).toMutableList()
            val index = lista.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val item = lista[index]
                val nuevoLike = !item.meGustaUsuario
                val nuevoTotal = if (nuevoLike) item.totalLikes + 1 else max(0, item.totalLikes - 1)
                lista[index] = item.copy(meGustaUsuario = nuevoLike, totalLikes = nuevoTotal)
                listaPubs[indexPub] = pub.copy(adjuntos = lista)
            }
        } else {
            // Usamos (pub.comentarios ?: emptyList()) para evitar el error
            val lista = (pub.comentarios ?: emptyList()).toMutableList()
            val index = lista.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val item = lista[index]
                val nuevoLike = !item.meGustaUsuario
                val nuevoTotal = if (nuevoLike) item.totalLikes + 1 else max(0, item.totalLikes - 1)
                lista[index] = item.copy(meGustaUsuario = nuevoLike, totalLikes = nuevoTotal)
                listaPubs[indexPub] = pub.copy(comentarios = lista)
            }
        }
        uiState = uiState.copy(publicaciones = listaPubs)
    }

    // --- FUNCIÓN PARA ENVIAR TEXTO, FOTOS, AUDIOS Y DOCUMENTOS ---
    fun enviarMensaje(
        token: String,
        publicacionId: Int,
        texto: String?,
        uri: Uri?,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val api = ApiClient.createAuthorized(token, ForoApi::class.java)

                // Texto
                val textoPart = texto
                    ?.takeIf { it.isNotBlank() }
                    ?.toRequestBody("text/plain".toMediaType())

                // Archivo (opcional)
                val archivoPart = uri?.let {
                    val file = uriToFile(context, uri) ?: return@launch onError("No se pudo leer archivo")

                    val mime = getMimeType(file)
                    val requestBody = file.asRequestBody(mime.toMediaType())

                    MultipartBody.Part.createFormData(
                        "archivo",
                        file.name,
                        requestBody
                    )
                }

                api.enviarMensaje(
                    publicacionId = publicacionId,
                    texto = textoPart,
                    archivo = archivoPart
                )

                onSuccess()
                cargar(token)

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Error desconocido")
            }
        }
    }
}