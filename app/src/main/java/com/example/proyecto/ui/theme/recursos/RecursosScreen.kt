package com.example.proyecto.ui.theme.recursos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.RecursosApi
import com.example.proyecto.data.recursos.CrearSolicitudReq
import com.example.proyecto.data.recursos.RecursoDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Colores consistentes con tu MainActivity
val tuColorPrincipal = Color(0xFF42A5F5)

// =================================================================================
// 1. PANTALLA PRINCIPAL
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar y DatePicker
@Composable
fun RecursosScreen(
    token: String,
    onBack: () -> Unit
) {
    // 1. Inicializar el ViewModel usando el Factory
    // Creamos el cliente de API autorizado usando el token
    val recursosApi: RecursosApi = remember(token) {
        ApiClient.createAuthorized(token, RecursosApi::class.java)
    }

    val vm: RecursosViewModel = viewModel(
        factory = RecursosViewModelFactory(recursosApi)
    )

    // 2. Obtener los estados del ViewModel
    val recursos by vm.recursos.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val reservaMessage by vm.reservaMessage.collectAsState()

    // 3. Estado local para manejar la reserva (abre el diálogo)
    var selectedRecursoId by remember { mutableStateOf<Int?>(null) }

    // Auto-refresh o carga inicial al entrar con el token
    LaunchedEffect(token) {
        vm.cargarRecursos()
    }

    // Mostrar mensaje de éxito/error de reserva
    LaunchedEffect(reservaMessage) {
        if (reservaMessage != null) {
            // Lógica para mostrar Snackbar o Toast
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recursos Comunitarios", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tuColorPrincipal)
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {

            // Indicadores de estado
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = tuColorPrincipal)
            }
            errorMessage?.let {
                Text(
                    "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            reservaMessage?.let {
                Text(
                    it,
                    color = if (it.contains("Error")) MaterialTheme.colorScheme.error else tuColorPrincipal,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Contenido principal
            if (!isLoading && recursos.isEmpty() && errorMessage == null) {
                EmptyRecursos(onRecargar = { vm.cargarRecursos() })
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(
                        items = recursos,
                        key = { it.id },
                        contentType = { "recurso" }
                    ) { recurso ->
                        RecursoItem(
                            recurso = recurso,
                            onReservarClick = { selectedRecursoId = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // 4. Diálogo de Solicitud de Reserva
    selectedRecursoId?.let { id ->
        val recurso = recursos.firstOrNull { it.id == id }
        if (recurso != null) {
            SolicitudDialog(
                recurso = recurso,
                onDismiss = { selectedRecursoId = null },
                onConfirm = { fechaInicio, fechaFin ->
                    selectedRecursoId = null
                    vm.crearSolicitud(
                        CrearSolicitudReq(
                            recurso = id,
                            fechaInicio = fechaInicio,
                            fechaFin = fechaFin,
                            motivo = null
                        )
                    )
                }
            )
        } else {
            selectedRecursoId = null // Cierra si no encuentra el recurso
        }
    }
}

// =================================================================================
// 2. ITEM DE RECURSO
// =================================================================================

@Composable
fun RecursoItem(recurso: RecursoDto, onReservarClick: (Int) -> Unit, modifier: Modifier) {

    // Definir la acción de reserva
    val onButtonClick = { onReservarClick(recurso.id) }

    // ✅ LÓGICA DE HABILITACIÓN CLAVE:
    // El botón está habilitado si:
    // 1. El recurso está disponible.
    // 2. Y (No hay solicitud anterior O la última solicitud fue RECHAZADA).
    val actualIsEnabled = recurso.disponible && (
            recurso.estadoUltimaSolicitud == null ||
                    recurso.estadoUltimaSolicitud == "RECHAZADA"
            )

    val colorBoton = if (actualIsEnabled) tuColorPrincipal else Color.Gray

    // ✅ LÓGICA DE TEXTO DEL BOTÓN (Muestra el estado final)
    val textoBoton = when (recurso.estadoUltimaSolicitud) {
        "APROBADA" -> "Reserva Aprobada"
        "RECHAZADA" -> "Solicitud Rechazada"
        "PENDIENTE" -> "Solicitud Pendiente"
        else -> if (recurso.disponible) "Reservar Recurso" else "No Disponible Hoy"
    }

    // Si la solicitud es APROBADA o PENDIENTE, se debe deshabilitar el botón
    val isButtonDisabledByStatus = recurso.estadoUltimaSolicitud == "APROBADA" || recurso.estadoUltimaSolicitud == "PENDIENTE"
    val finalEnabled = actualIsEnabled && !isButtonDisabledByStatus

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = recurso.nombre,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = tuColorPrincipal
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = recurso.descripcion ?: "Sin descripción",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            // Botón de Reserva
            Button(
                onClick = onButtonClick,
                enabled = finalEnabled, // 🔄 Usa la lógica de habilitación final
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorBoton)
            ) {
                Text(
                    text = textoBoton, // 🔄 Usa el texto de estado
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// =================================================================================
// 3. DIÁLOGO DE SOLICITUD DE RESERVA (CON SELECTOR DE FECHAS)
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolicitudDialog(
    recurso: RecursoDto,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    // Estados para los selectores de fecha
    var showDatePickerInicio by rememberSaveable { mutableStateOf(false) }
    var showDatePickerFin by rememberSaveable { mutableStateOf(false) }

    // Estados para almacenar las fechas (Long es milisegundos desde época)
    val datePickerStateInicio = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val datePickerStateFin = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Variables para el texto y la validación
    val selectedDateInicio = datePickerStateInicio.selectedDateMillis
    val selectedDateFin = datePickerStateFin.selectedDateMillis

    // Formateador para mostrar en la UI (dd/MM/yyyy) y para el API (yyyy-MM-dd)
    val formatterUI = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formatterAPI = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val fechaInicioStringUI = if (selectedDateInicio != null) {
        Instant.ofEpochMilli(selectedDateInicio).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterUI)
    } else "Seleccionar Fecha de Inicio"

    val fechaFinStringUI = if (selectedDateFin != null) {
        Instant.ofEpochMilli(selectedDateFin).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterUI)
    } else "Seleccionar Fecha de Fin"

    // Función de validación
    val isFormValid = selectedDateInicio != null && selectedDateFin != null &&
            selectedDateInicio <= selectedDateFin // Inicio no debe ser después de fin

    // --- DIÁLOGOS DE CALENDARIO ---

    if (showDatePickerInicio) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerInicio = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerInicio = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerInicio = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerStateInicio)
        }
    }

    if (showDatePickerFin) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFin = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerFin = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFin = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerStateFin)
        }
    }

    // --- DIÁLOGO PRINCIPAL DE SOLICITUD ---

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Reserva: ${recurso.nombre}", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Selecciona las fechas de solicitud.")
                Spacer(Modifier.height(16.dp))

                // Campo Fecha de Inicio
                OutlinedTextField(
                    value = fechaInicioStringUI,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de Inicio") },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, null, Modifier.clickable { showDatePickerInicio = true })
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Campo Fecha de Fin
                OutlinedTextField(
                    value = fechaFinStringUI,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de Fin") },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, null, Modifier.clickable { showDatePickerFin = true })
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Mensaje de validación si las fechas son inválidas
                if (selectedDateInicio != null && selectedDateFin != null && selectedDateInicio > selectedDateFin) {
                    Text(
                        "⚠️ La fecha de fin debe ser posterior a la de inicio.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        // Convertir a formato API YYYY-MM-DD antes de enviar
                        val apiInicio = Instant.ofEpochMilli(selectedDateInicio!!).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterAPI)
                        val apiFin = Instant.ofEpochMilli(selectedDateFin!!).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterAPI)
                        onConfirm(apiInicio, apiFin)
                    }
                },
                enabled = isFormValid, // Deshabilitar si no es válido
                colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
            ) { Text("Confirmar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}


// =================================================================================
// 4. VISTA DE LISTA VACÍA
// =================================================================================

@Composable
private fun EmptyRecursos(onRecargar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.LibraryBooks,
                contentDescription = null,
                tint = tuColorPrincipal,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No hay recursos disponibles",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = tuColorPrincipal,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onRecargar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tuColorPrincipal)
            ) {
                Text("Actualizar")
            }
        }
    }
}