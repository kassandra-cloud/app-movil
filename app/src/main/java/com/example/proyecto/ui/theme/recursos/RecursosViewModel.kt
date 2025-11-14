// app/src/main/java/com/example/proyecto/ui/recursos/RecursosViewModel.kt
package com.example.proyecto.ui.theme.recursos

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.RecursosApi
import com.example.proyecto.data.recursos.CrearSolicitudReq
import com.example.proyecto.data.recursos.RecursoDto
import com.example.proyecto.data.recursos.SolicitudDto
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RecursosState(
    val cargando: Boolean = false,
    val error: String? = null,
    val recursos: List<RecursoDto> = emptyList(),
    val page: Int = 0,
    val fin: Boolean = false,
    // recursoId -> estado ("PENDIENTE" | "APROBADA" | "RECHAZADA")
    val misSolicitudes: Map<Int, String> = emptyMap(),
    // 💡 AÑADIDO: Estado para controlar el envío de una solicitud
    val enviando: Boolean = false
)

class RecursosViewModel(private val token: String) : ViewModel() {

    private val api by lazy { ApiClient.getClient(token).create(RecursosApi::class.java) }
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    var ui: MutableState<RecursosState> = mutableStateOf(RecursosState())
        private set

    fun refresh(disponiblesSolo: Boolean = true) = cargar(disponiblesSolo, reset = true)
    fun loadMore(disponiblesSolo: Boolean = true) = cargar(disponiblesSolo, reset = false)

    fun cargar(disponiblesSolo: Boolean = true, reset: Boolean = true) {
        val nextPage = if (reset) 1 else ui.value.page + 1
        if (!reset && ui.value.fin) return

        ui.value = ui.value.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                val resp = api.listarRecursos(
                    disponible = if (disponiblesSolo) true else null,
                    page = nextPage
                )
                val nuevos = if (reset) resp.results else ui.value.recursos + resp.results
                ui.value = ui.value.copy(
                    cargando = false,
                    recursos = nuevos,
                    page = nextPage,
                    fin = resp.next == null
                )
            } catch (e: Exception) {
                ui.value = ui.value.copy(cargando = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    /** Carga /recursos/api/v1/solicitudes/?mine=true y mapea recursoId -> estado */
    fun cargarMisSolicitudes(estado: String? = null) {
        viewModelScope.launch {
            try {
                val page = api.misSolicitudes(mine = true, estado = estado)
                val map = page.results.associate { it.recurso to it.estado }
                ui.value = ui.value.copy(misSolicitudes = map)
            } catch (e: Exception) {
                // No rompas la UI si falla solo el estado
                ui.value = ui.value.copy(error = ui.value.error ?: e.message)
            }
        }
    }

    fun solicitar(
        recursoId: Int,
        inicio: String,
        fin: String,
        motivo: String?,
        onOk: (SolicitudDto) -> Unit,
        onErr: (String) -> Unit
    ) {
        // 💡 1. Inicia el estado de envío
        ui.value = ui.value.copy(enviando = true)

        viewModelScope.launch {
            try {
                val di = LocalDate.parse(inicio, ISO)
                val df = LocalDate.parse(fin, ISO)
                if (df.isBefore(di)) {
                    onErr("La fecha fin no puede ser menor que la fecha inicio.")
                    return@launch
                }

                val req = CrearSolicitudReq(
                    recurso = recursoId,
                    fechaInicio = inicio.trim(),
                    fechaFin = fin.trim(),
                    motivo = motivo?.ifBlank { null }
                )
                val resp = api.crearSolicitud(req)
                onOk(resp)

                // Refresca estado para deshabilitar el botón inmediatamente
                cargarMisSolicitudes()

            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                val msg = try {
                    val jo = JSONObject(body ?: "{}")
                    buildString {
                        jo.keys().forEach { key ->
                            val arr = jo.optJSONArray(key)
                            val errorMsg = if (arr != null && arr.length() > 0)
                                arr.getString(0)
                            else "Error desconocido."

                            val formattedKey = if (key == "non_field_errors") "Error General" else key.capitalize()

                            append("$formattedKey: $errorMsg\n")
                        }
                    }.ifBlank { "Solicitud inválida (${e.code()})." }
                } catch (_: Exception) {
                    body ?: "Solicitud inválida (${e.code()})."
                }
                onErr(msg.trim())
            } catch (e: Exception) {
                val moshiError = e.message

                val finalMsg = if (moshiError != null && moshiError.contains("Required value 'id' missing")) {
                    "Error de validación: La solicitud fue rechazada por el servidor. Revisa si ya tienes una solicitud activa para este recurso o si los datos son incorrectos."
                } else {
                    e.message ?: "Error al crear solicitud"
                }

                onErr(finalMsg)
            } finally {
                // 💡 2. Finaliza el estado de envío, sin importar el resultado
                ui.value = ui.value.copy(enviando = false)
            }
        }
    }
}