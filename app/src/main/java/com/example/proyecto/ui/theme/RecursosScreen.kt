// app/src/main/java/com/example/proyecto/ui/recursos/RecursosScreen.kt
package com.example.proyecto.ui.recursos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.recursos.RecursoDto
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecursosScreen(
    token: String,
    onBack: () -> Unit
) {
    val vm: RecursosViewModel = viewModel(factory = RecursosViewModelFactory(token))
    val ui by vm.ui

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.refresh(disponiblesSolo = true)
        vm.cargarMisSolicitudes()           // ← carga estados para mostrar el chip y deshabilitar botón
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recursos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.cargando && ui.recursos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ui.error != null && ui.recursos.isEmpty() -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Error: ${ui.error}")
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            vm.refresh()
                            vm.cargarMisSolicitudes()
                        }) { Text("Reintentar") }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ui.recursos) { r ->
                            var showDialog by remember { mutableStateOf(false) }

                            if (showDialog) {
                                SolicitudDialog(
                                    onDismiss = { showDialog = false },
                                    onSubmit = { ini, fin, mot ->
                                        vm.solicitar(
                                            recursoId = r.id,
                                            inicio = ini,
                                            fin = fin,
                                            motivo = mot,
                                            onOk = {
                                                showDialog = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Solicitud enviada para '${r.nombre}'"
                                                    )
                                                }
                                            },
                                            onErr = { msg ->
                                                showDialog = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Error al solicitar: $msg"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                )
                            }

                            RecursoCard(
                                recurso = r,
                                estadoActual = ui.misSolicitudes[r.id],  // ← estado del usuario
                                onSolicitar = { showDialog = true }
                            )
                        }

                        if (!ui.fin) {
                            item {
                                OutlinedButton(
                                    onClick = { vm.loadMore() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Cargar más") }
                            }
                        }
                    }

                    if (ui.cargando && ui.recursos.isNotEmpty()) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

/** Chip de estado PENDIENTE/APROBADA/RECHAZADA */
@Composable
private fun EstadoChip(estado: String) {
    val texto = when (estado) {
        "APROBADA" -> "Aprobada"
        "RECHAZADA" -> "Rechazada"
        else -> "Pendiente"
    }
    AssistChip(onClick = {}, label = { Text(texto) })
}

/** Tarjeta de recurso con chips y botón Solicitar (deshabilitado si ya tiene activa) */
@Composable
private fun RecursoCard(
    recurso: RecursoDto,
    estadoActual: String?,              // null | "PENDIENTE" | "APROBADA" | "RECHAZADA"
    onSolicitar: () -> Unit
) {
    val yaTieneActiva = estadoActual == "PENDIENTE" || estadoActual == "APROBADA"

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(recurso.nombre, style = MaterialTheme.typography.titleMedium)
            if (!recurso.descripcion.isNullOrBlank()) {
                Text(recurso.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = {}, label = {
                    Text(if (recurso.disponible) "Disponible" else "No disponible")
                })
                Spacer(Modifier.width(8.dp))
                if (estadoActual != null) {
                    EstadoChip(estadoActual)
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedButton(
                    enabled = recurso.disponible && !yaTieneActiva,
                    onClick = onSolicitar
                ) { Text(if (yaTieneActiva) "Ya solicitada" else "Solicitar") }
            }
        }
    }
}

/** Diálogo con DatePicker (YYYY-MM-DD) y motivo opcional. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?) -> Unit
) {
    val iso = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val tz = remember { ZoneId.systemDefault() }

    var inicio by remember { mutableStateOf<String?>(null) }
    var fin by remember { mutableStateOf<String?>(null) }
    var motivo by remember { mutableStateOf("") }

    var showInicio by remember { mutableStateOf(false) }
    var showFin by remember { mutableStateOf(false) }

    fun millisToIso(millis: Long?): String? =
        millis?.let { Instant.ofEpochMilli(it).atZone(tz).toLocalDate().format(iso) }

    if (showInicio) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showInicio = false },
            confirmButton = {
                TextButton(onClick = {
                    inicio = millisToIso(dpState.selectedDateMillis)
                    if (fin != null && inicio != null && fin!! < inicio!!) fin = null
                    showInicio = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showInicio = false }) { Text("Cancelar") } }
        ) { DatePicker(state = dpState) }
    }

    if (showFin) {
        val minMillis = remember(inicio) {
            inicio?.let {
                val ldt = java.time.LocalDate.parse(it, iso).atStartOfDay(tz)
                ldt.toInstant().toEpochMilli()
            }
        }
        val selectable = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                minMillis == null || utcTimeMillis >= minMillis
        }
        val dpState = rememberDatePickerState(selectableDates = selectable)
        DatePickerDialog(
            onDismissRequest = { showFin = false },
            confirmButton = {
                TextButton(onClick = {
                    fin = millisToIso(dpState.selectedDateMillis)
                    showFin = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFin = false }) { Text("Cancelar") } }
        ) { DatePicker(state = dpState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar recurso") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inicio.orEmpty(),
                    onValueChange = {},
                    label = { Text("Fecha inicio") },
                    readOnly = true,
                    trailingIcon = { TextButton(onClick = { showInicio = true }) { Text("Elegir") } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fin.orEmpty(),
                    onValueChange = {},
                    label = { Text("Fecha fin") },
                    readOnly = true,
                    trailingIcon = { TextButton(onClick = { showFin = true }) { Text("Elegir") } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!inicio.isNullOrBlank() && !fin.isNullOrBlank() && fin!! < inicio!!) {
                    Text(
                        "La fecha fin no puede ser menor que la fecha inicio.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            val enabled = !inicio.isNullOrBlank() && !fin.isNullOrBlank() && fin!! >= inicio!!
            TextButton(
                enabled = enabled,
                onClick = { onSubmit(inicio!!, fin!!, motivo.ifBlank { null }) }
            ) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
