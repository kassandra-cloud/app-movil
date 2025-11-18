package com.example.proyecto.ui.theme.recursos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.RecursosApi
import com.example.proyecto.data.recursos.CrearSolicitudReq
import com.example.proyecto.data.recursos.RecursoDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Colores consistentes con tu MainActivity
val tuColorPrincipal = Color(0xFF42A5F5)

// 🌟 COLORES DE ESTADO (MEJORADOS)
val ColorAprobada = Color(0xFF4CAF50) // Verde
val ColorRechazada = Color(0xFFF44336) // Rojo
val ColorPendiente = Color(0xFFFFC107) // Amarillo/Ámbar
val ColorNoDisponible = Color(0xFF9E9E9E) // Gris para no disponible

// -------------------------------------------------------
// FILTROS DE ESTADO
// -------------------------------------------------------
enum class RecursosFilter(val label: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    APROBADOS("Aprobados"),
    RECHAZADOS("Rechazados")
}

// =================================================================================
// 1. PANTALLA PRINCIPAL
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecursosScreen(
    token: String,
    onBack: () -> Unit
) {
    // 1. Inicializar el ViewModel usando el Factory
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

    // 4. Estado local para el filtro
    var selectedFilter by rememberSaveable { mutableStateOf(RecursosFilter.TODOS) }

    // Auto-refresh o carga inicial al entrar con el token
    LaunchedEffect(token) {
        vm.cargarRecursos()
    }

    // Mostrar mensaje de éxito/error de reserva
    LaunchedEffect(reservaMessage) {
        if (reservaMessage != null) {
            // Aquí podrías usar Snackbar si quieres
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

            // 👉 Barra de filtros por estado
            RecursosFilterBar(
                selected = selectedFilter,
                onSelectedChange = { selectedFilter = it }
            )

            // Lista filtrada según el chip seleccionado
            val recursosFiltrados = when (selectedFilter) {
                RecursosFilter.TODOS -> recursos
                RecursosFilter.PENDIENTES ->
                    recursos.filter { it.estadoUltimaSolicitud == "PENDIENTE" }
                RecursosFilter.APROBADOS ->
                    recursos.filter { it.estadoUltimaSolicitud == "APROBADA" }
                RecursosFilter.RECHAZADOS ->
                    recursos.filter { it.estadoUltimaSolicitud == "RECHAZADA" }
            }

            // Contenido principal
            when {
                !isLoading && recursos.isEmpty() && errorMessage == null -> {
                    // No hay recursos en el sistema
                    EmptyRecursos(onRecargar = { vm.cargarRecursos() })
                }
                !isLoading && recursosFiltrados.isEmpty() && errorMessage == null -> {
                    // Hay recursos, pero ninguno coincide con el filtro
                    Text(
                        text = "No hay recursos para este filtro.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(
                            items = recursosFiltrados,
                            key = { it.id },
                            contentType = { "recurso" }
                        ) { recurso ->
                            RecursoItem(
                                recurso = recurso,
                                onReservarClick = { selectedRecursoId = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
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
// BARRA DE FILTROS
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecursosFilterBar(
    selected: RecursosFilter,
    onSelectedChange: (RecursosFilter) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Campo “tipo spinner”
        OutlinedTextField(
            value = selected.label,
            onValueChange = { },
            readOnly = true,
            label = { Text("Filtrar por estado") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        // Lista desplegable
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RecursosFilter.values().forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onSelectedChange(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

// =================================================================================
// 2. ITEM DE RECURSO (OPTIMIZADO PARA PERSONAS MAYORES)
// =================================================================================

@Composable
fun RecursoItem(recurso: RecursoDto, onReservarClick: (Int) -> Unit, modifier: Modifier) {

    // 1. Lógica de Colores, Texto y Icono: Usa la data class RecursoVisuals
    val visuals = when (recurso.estadoUltimaSolicitud) {
        "APROBADA" -> RecursoVisuals(
            textoBoton = "RESERVA APROBADA",
            textoEstado = "APROBADA",
            colorBoton = ColorAprobada,
            colorFondoCard = Color.White,
            colorEstadoTexto = ColorAprobada,
            estadoIcon = Icons.Filled.CheckCircle
        )
        "RECHAZADA" -> RecursoVisuals(
            textoBoton = "SOLICITUD RECHAZADA",
            textoEstado = "RECHAZADA",
            colorBoton = ColorRechazada,
            colorFondoCard = Color.White,
            colorEstadoTexto = ColorRechazada,
            estadoIcon = Icons.Filled.Cancel
        )
        "PENDIENTE" -> RecursoVisuals(
            textoBoton = "SOLICITUD PENDIENTE",
            textoEstado = "PENDIENTE",
            colorBoton = ColorPendiente,
            colorFondoCard = Color.White,
            colorEstadoTexto = ColorPendiente,
            estadoIcon = Icons.Filled.Schedule
        )
        else -> RecursoVisuals(
            textoBoton = if (recurso.disponible) "RESERVAR RECURSO" else "NO DISPONIBLE HOY",
            textoEstado = if (recurso.disponible) "DISPONIBLE" else "NO DISPONIBLE",
            colorBoton = if (recurso.disponible) tuColorPrincipal else ColorNoDisponible,
            colorFondoCard = Color.White,
            colorEstadoTexto = if (recurso.disponible) tuColorPrincipal else ColorNoDisponible,
            estadoIcon = if (recurso.disponible) Icons.Filled.Info else Icons.Filled.Block
        )
    }

    // 2. Lógica de Habilitación del Botón
    val actualIsEnabled = recurso.disponible && (
            recurso.estadoUltimaSolicitud == null ||
                    recurso.estadoUltimaSolicitud == "RECHAZADA"
            )

    val isButtonDisabledByStatus = recurso.estadoUltimaSolicitud == "APROBADA" || recurso.estadoUltimaSolicitud == "PENDIENTE"
    val finalEnabled = actualIsEnabled && !isButtonDisabledByStatus

    val onButtonClick = { onReservarClick(recurso.id) }

    val colorBorde = visuals.colorEstadoTexto
    val anchoBorde = if (visuals.estadoIcon != null) 2.dp else 1.dp

    Card(
        modifier = modifier.border(anchoBorde, colorBorde, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = visuals.colorFondoCard)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recurso.nombre,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = visuals.colorEstadoTexto
                    ),
                    modifier = Modifier.weight(1f)
                )

                visuals.estadoIcon?.let { icon ->
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            imageVector = icon,
                            contentDescription = visuals.textoEstado,
                            tint = visuals.colorEstadoTexto,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = visuals.textoEstado,
                            style = MaterialTheme.typography.titleSmall,
                            color = visuals.colorEstadoTexto,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = recurso.descripcion ?: "Sin descripción",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onButtonClick,
                enabled = finalEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = visuals.colorBoton)
            ) {
                Text(
                    text = visuals.textoBoton,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Clase de datos auxiliar para organizar los valores de visualización
private data class RecursoVisuals(
    val textoBoton: String,
    val textoEstado: String,
    val colorBoton: Color,
    val colorFondoCard: Color,
    val colorEstadoTexto: Color,
    val estadoIcon: ImageVector?
)


// =================================================================================
// 3. DIÁLOGO DE SOLICITUD DE RESERVA
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolicitudDialog(
    recurso: RecursoDto,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var showDatePickerInicio by rememberSaveable { mutableStateOf(false) }
    var showDatePickerFin by rememberSaveable { mutableStateOf(false) }

    val datePickerStateInicio = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val datePickerStateFin = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val selectedDateInicio = datePickerStateInicio.selectedDateMillis
    val selectedDateFin = datePickerStateFin.selectedDateMillis

    val formatterUI = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formatterAPI = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val fechaInicioStringUI = if (selectedDateInicio != null) {
        Instant.ofEpochMilli(selectedDateInicio).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterUI)
    } else "Seleccionar Fecha de Inicio"

    val fechaFinStringUI = if (selectedDateFin != null) {
        Instant.ofEpochMilli(selectedDateFin).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterUI)
    } else "Seleccionar Fecha de Fin"

    val isFormValid = selectedDateInicio != null && selectedDateFin != null &&
            selectedDateInicio <= selectedDateFin

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Reserva: ${recurso.nombre}", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Selecciona las fechas de solicitud.")
                Spacer(Modifier.height(16.dp))

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
                        val apiInicio = Instant.ofEpochMilli(selectedDateInicio!!).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterAPI)
                        val apiFin = Instant.ofEpochMilli(selectedDateFin!!).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterAPI)
                        onConfirm(apiInicio, apiFin)
                    }
                },
                enabled = isFormValid,
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
        elevation = CardDefaults.cardElevation(4.dp)
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
