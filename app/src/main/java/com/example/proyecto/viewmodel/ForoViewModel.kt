package com.example.proyecto.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.ComentarioCrearRequest
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ForoViewModel : ViewModel() {

    data class UiState(
        val cargando: Boolean = true,
        val publicaciones: List<PublicacionDto> = emptyList(),
        val error: String? = null
    )

    var uiState by mutableStateOf(UiState())
        private set

    // Estados por publicación
    val comentarios = mutableStateMapOf<Int, List<ComentarioDto>>() // pubId -> lista
    val posting     = mutableStateMapOf<Int, Boolean>()             // pubId -> enviando
    val postError   = mutableStateMapOf<Int, String?>()             // pubId -> último error

    // Polling por publicación (para “tiempo real” simple)
    private val refreshJobs = mutableMapOf<Int, Job>()

    fun cargar(token: String) {
        uiState = uiState.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                val data = ApiClient.foroApi.listar("Token $token")
                uiState = UiState(cargando = false, publicaciones = data)
                // Auto-refresh por cada publicación
                data.forEach { startAutoRefresh(token, it.id) }
            } catch (e: Exception) {
                Log.e("FORO", "listar()", e)
                uiState = uiState.copy(cargando = false, error = e.message ?: "Error al cargar")
            }
        }
    }

    fun cargarComentarios(token: String, pubId: Int, force: Boolean = false) {
        if (!force && comentarios.containsKey(pubId)) return
        viewModelScope.launch {
            try {
                comentarios[pubId] = ApiClient.foroApi.comentarios("Token $token", pubId)
            } catch (e: Exception) {
                Log.w("FORO", "comentarios($pubId)", e)
            }
        }
    }

    fun comentar(token: String, pubId: Int, texto: String, parentId: Int? = null) {
        val body = texto.trim()
        if (body.isBlank()) return

        posting[pubId] = true
        postError[pubId] = null

        viewModelScope.launch {
            try {
                val nuevo = ApiClient.foroApi.comentar(
                    "Token $token",
                    pubId,
                    ComentarioCrearRequest(texto = body, parent = parentId)
                )
                // Agrega localmente para respuesta inmediata
                val prev = comentarios[pubId] ?: emptyList()
                comentarios[pubId] = prev + nuevo
                // Refresca desde servidor para mantener hilos/orden
                cargarComentarios(token, pubId, force = true)
            } catch (e: Exception) {
                Log.e("FORO", "comentar($pubId)", e)
                postError[pubId] = e.message ?: "No se pudo enviar"
            } finally {
                posting[pubId] = false
            }
        }
    }

    private fun startAutoRefresh(token: String, pubId: Int) {
        if (refreshJobs[pubId]?.isActive == true) return
        refreshJobs[pubId] = viewModelScope.launch {
            while (isActive) {
                try {
                    val remotos = ApiClient.foroApi.comentarios("Token $token", pubId)
                    comentarios[pubId] = remotos
                } catch (_: Exception) {
                    // silencioso
                }
                delay(3000) // cada 3 s
            }
        }
    }

    override fun onCleared() {
        refreshJobs.values.forEach { it.cancel() }
        super.onCleared()
    }
}
