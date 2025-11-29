package com.example.proyecto.ui.theme.recursos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.api.ApiClient
import com.example.proyecto.api.RecursosApi
import com.example.proyecto.data.recursos.CrearSolicitudReq
import com.example.proyecto.data.recursos.RecursoDto
import com.example.proyecto.ui.theme.AppColors // 👈 Importamos tus colores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// --- COLORES DE ESTADO (Estos se mantienen fijos por semántica, pero se usan con cuidado) ---
val ColorAprobada = Color(0xFF4CAF50)
val ColorRechazada = Color(0xFFF44336)
val ColorPendiente = Color(0xFFFFC107)
val ColorNoDisponible = Color(0xFF9E9E9E)

enum class RecursosFilter(val label: String) {
    TODOS("Todos"),
    DISPONIBLES("Disponibles"),
    PENDIENTES("Pendientes"),
    APROBADOS("Aprobados"),
    RECHAZADOS("Rechazados")
}

// =============================================================================
// 1. PANTALLA PRINCIPAL
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecursosScreen(
    token: String,
    onBack: () -> Unit
) {
    val recursosApi: RecursosApi = remember(token) {
        ApiClient.createAuthorized(token, RecursosApi::class.java)
    }

    val vm: RecursosViewModel = viewModel(
        key = token,
        factory = RecursosViewModelFactory(recursosApi)
    )

    val recursos by vm.recursos.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val reservaMessage by vm.reservaMessage.collectAsState()

    var selectedRecursoId by remember { mutableStateOf<Int?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf(RecursosFilter.TODOS) }

    LaunchedEffect(Unit) {
        vm.cargarRecursos()
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            // ✅ BARRA CON GRADIENTE DE MARCA
            TopAppBar(
                title = { Text("Recursos Comunitarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Transparente para ver el gradiente
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.background(AppColors.GradientePrincipal)
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // ✅ Fondo Dinámico (Gris claro en día, Oscuro en noche)
                .background(MaterialTheme.colorScheme.background)
        ) {

            // Barra de progreso y mensajes
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }
            errorMessage?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            reservaMessage?.let {
                Text(
                    it,
                    color = if (it.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Barra de Filtros
            RecursosFilterBar(selected = selectedFilter, onSelectedChange = { selectedFilter = it })

            val recursosFiltrados = when (selectedFilter) {
                RecursosFilter.TODOS -> recursos
                RecursosFilter.DISPONIBLES -> recursos.filter { it.disponible && (it.estadoUltimaSolicitud == null || it.estadoUltimaSolicitud == "RECHAZADA") }
                RecursosFilter.PENDIENTES -> recursos.filter { it.estadoUltimaSolicitud == "PENDIENTE" }
                RecursosFilter.APROBADOS -> recursos.filter { it.estadoUltimaSolicitud == "APROBADA" }
                RecursosFilter.RECHAZADOS -> recursos.filter { it.estadoUltimaSolicitud == "RECHAZADA" }
            }

            // Lista de recursos
            when {
                !isLoading && recursos.isEmpty() && errorMessage == null -> {
                    EmptyRecursos(onRecargar = { vm.cargarRecursos() })
                }
                !isLoading && recursosFiltrados.isEmpty() && errorMessage == null -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No se encontraron recursos para este filtro.",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(items = recursosFiltrados, key = { it.id }) { recurso ->
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
    }

    selectedRecursoId?.let { id ->
        val recurso = recursos.firstOrNull { it.id == id }
        if (recurso != null) {
            SolicitudDialog(
                recurso = recurso,
                onDismiss = { selectedRecursoId = null },
                onConfirm = { fechaInicio, fechaFin, motivo ->
                    selectedRecursoId = null
                    vm.crearSolicitud(
                        CrearSolicitudReq(recurso = id, fechaInicio = fechaInicio, fechaFin = fechaFin, motivo = motivo)
                    )
                }
            )
        } else {
            selectedRecursoId = null
        }
    }
}

// =============================================================================
// 2. BARRA DE FILTROS (DROPDOWN)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecursosFilterBar(selected: RecursosFilter, onSelectedChange: (RecursosFilter) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selected.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filtrar por estado") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                // ✅ Colores adaptativos para el input
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
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
}

// =============================================================================
// 3. TARJETA DE RECURSO
// =============================================================================

@Composable
fun RecursoItem(recurso: RecursoDto, onReservarClick: (Int) -> Unit, modifier: Modifier) {
    // Definimos visuales según estado, pero usando colores del tema donde sea posible
    val visuals = when (recurso.estadoUltimaSolicitud) {
        "APROBADA" -> RecursoVisuals("RESERVA APROBADA", "APROBADA", ColorAprobada, MaterialTheme.colorScheme.surface, ColorAprobada, Icons.Filled.CheckCircle)
        "RECHAZADA" -> RecursoVisuals("SOLICITUD RECHAZADA", "RECHAZADA", ColorRechazada, MaterialTheme.colorScheme.surface, ColorRechazada, Icons.Filled.Cancel)
        "PENDIENTE" -> RecursoVisuals("SOLICITUD PENDIENTE", "PENDIENTE", ColorPendiente, MaterialTheme.colorScheme.surface, ColorPendiente, Icons.Filled.Schedule)
        else -> RecursoVisuals(
            if (recurso.disponible) "RESERVAR RECURSO" else "NO DISPONIBLE HOY",
            if (recurso.disponible) "DISPONIBLE" else "NO DISPONIBLE",
            if (recurso.disponible) MaterialTheme.colorScheme.primary else ColorNoDisponible,
            MaterialTheme.colorScheme.surface,
            if (recurso.disponible) MaterialTheme.colorScheme.primary else ColorNoDisponible,
            if (recurso.disponible) Icons.Filled.Info else Icons.Filled.Block
        )
    }

    val actualIsEnabled = recurso.disponible && (recurso.estadoUltimaSolicitud == null || recurso.estadoUltimaSolicitud == "RECHAZADA")
    val isButtonDisabledByStatus = recurso.estadoUltimaSolicitud == "APROBADA" || recurso.estadoUltimaSolicitud == "PENDIENTE"
    val finalEnabled = actualIsEnabled && !isButtonDisabledByStatus

    val colorBorde = visuals.colorEstadoTexto
    val anchoBorde = if (visuals.estadoIcon != null) 2.dp else 1.dp

    Card(
        modifier = modifier.border(anchoBorde, colorBorde, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = visuals.colorFondoCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    recurso.nombre,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = visuals.colorEstadoTexto),
                    modifier = Modifier.weight(1f)
                )
                visuals.estadoIcon?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(it, null, tint = visuals.colorEstadoTexto, modifier = Modifier.size(32.dp))
                        Text(visuals.textoEstado, style = MaterialTheme.typography.labelSmall, color = visuals.colorEstadoTexto, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                recurso.descripcion ?: "Sin descripción",
                style = MaterialTheme.typography.bodyMedium,
                // ✅ Texto secundario adaptable
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onReservarClick(recurso.id) },
                enabled = finalEnabled,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = visuals.colorBoton,
                    contentColor = Color.White // Texto botón siempre blanco para contraste con colores fuertes
                )
            ) {
                Text(visuals.textoBoton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class RecursoVisuals(
    val textoBoton: String, val textoEstado: String, val colorBoton: Color,
    val colorFondoCard: Color, val colorEstadoTexto: Color, val estadoIcon: ImageVector?
)

// =============================================================================
// 4. DIÁLOGO DE SOLICITUD
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolicitudDialog(
    recurso: RecursoDto,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var showDatePickerInicio by rememberSaveable { mutableStateOf(false) }
    var showDatePickerFin by rememberSaveable { mutableStateOf(false) }
    var motivo by rememberSaveable { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val dateValidator = remember {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                return utcTimeMillis >= today
            }
            override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now().year
        }
    }

    val datePickerStateInicio = rememberDatePickerState(selectableDates = dateValidator)
    val datePickerStateFin = rememberDatePickerState(selectableDates = dateValidator)

    val formatterUI = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formatterAPI = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val fechaInicioStringUI = datePickerStateInicio.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterUI) } ?: "Seleccionar"
    val fechaFinStringUI = datePickerStateFin.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatterUI) } ?: "Seleccionar"

    val isFormValid = datePickerStateInicio.selectedDateMillis != null &&
            datePickerStateFin.selectedDateMillis != null &&
            datePickerStateInicio.selectedDateMillis!! <= datePickerStateFin.selectedDateMillis!! &&
            motivo.isNotBlank()

    // --- DIÁLOGO FECHAS ---
    if (showDatePickerInicio) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerInicio = false },
            confirmButton = { TextButton(onClick = { showDatePickerInicio = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePickerInicio = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerStateInicio, title = { Text("Fecha inicio") }) }
    }

    if (showDatePickerFin) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFin = false },
            confirmButton = { TextButton(onClick = { showDatePickerFin = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePickerFin = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerStateFin, title = { Text("Fecha fin") }) }
    }

    // --- DIÁLOGO PRINCIPAL ---
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                // ✅ Fondo Dialogo Adaptable
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Text(
                    text = "Reservar: ${recurso.nombre}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.weight(weight = 1f, fill = false).verticalScroll(scrollState)
                ) {
                    Text("Define fechas y motivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))

                    // Inicio
                    OutlinedTextField(
                        value = fechaInicioStringUI, onValueChange = {}, readOnly = true,
                        label = { Text("Desde") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.clickable { showDatePickerInicio = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Fin
                    OutlinedTextField(
                        value = fechaFinStringUI, onValueChange = {}, readOnly = true,
                        label = { Text("Hasta") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.clickable { showDatePickerFin = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Motivo
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo") },
                        placeholder = { Text("Ej: Cumpleaños, Reunión...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    if (datePickerStateInicio.selectedDateMillis != null &&
                        datePickerStateFin.selectedDateMillis != null &&
                        datePickerStateInicio.selectedDateMillis!! > datePickerStateFin.selectedDateMillis!!) {
                        Text(
                            "La fecha fin debe ser posterior a inicio",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val apiInicio = Instant.ofEpochMilli(datePickerStateInicio.selectedDateMillis!!)
                                    .atZone(ZoneId.systemDefault()).toLocalDate().format(formatterAPI)
                                val apiFin = Instant.ofEpochMilli(datePickerStateFin.selectedDateMillis!!)
                                    .atZone(ZoneId.systemDefault()).toLocalDate().format(formatterAPI)
                                onConfirm(apiInicio, apiFin, motivo)
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecursos(onRecargar: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.LibraryBooks, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
            Text("No hay recursos disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRecargar, modifier = Modifier.padding(top = 8.dp)) { Text("Recargar") }
        }
    }
}