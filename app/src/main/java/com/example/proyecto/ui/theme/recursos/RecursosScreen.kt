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
    // (Asumiendo que tienes definidos RecursosViewModel y RecursosViewModelFactory)
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
// 2. ITEM DE RECURSO (OPTIMIZADO PARA PERSONAS MAYORES)
// =================================================================================

@Composable
fun RecursoItem(recurso: RecursoDto, onReservarClick: (Int) -> Unit, modifier: Modifier) {

    // 1. Lógica de Colores, Texto y Icono: Usa la data class RecursoVisuals
    val visuals = when (recurso.estadoUltimaSolicitud) {
        "APROBADA" -> RecursoVisuals(
            textoBoton = "RESERVA APROBADA", // Texto del botón en mayúsculas
            textoEstado = "APROBADA",
            colorBoton = ColorAprobada,
            colorFondoCard = Color.White, // Fondo BLANCO para máximo contraste
            colorEstadoTexto = ColorAprobada,
            estadoIcon = Icons.Filled.CheckCircle
        )
        "RECHAZADA" -> RecursoVisuals(
            textoBoton = "SOLICITUD RECHAZADA", // Texto del botón en mayúsculas
            textoEstado = "RECHAZADA",
            colorBoton = ColorRechazada,
            colorFondoCard = Color.White, // Fondo BLANCO para máximo contraste
            colorEstadoTexto = ColorRechazada,
            estadoIcon = Icons.Filled.Cancel
        )
        "PENDIENTE" -> RecursoVisuals(
            textoBoton = "SOLICITUD PENDIENTE", // Texto del botón en mayúsculas
            textoEstado = "PENDIENTE",
            colorBoton = ColorPendiente,
            colorFondoCard = Color.White, // Fondo BLANCO para máximo contraste
            colorEstadoTexto = ColorPendiente,
            estadoIcon = Icons.Filled.Schedule
        )
        else -> RecursoVisuals(
            textoBoton = if (recurso.disponible) "RESERVAR RECURSO" else "NO DISPONIBLE HOY",
            textoEstado = if (recurso.disponible) "DISPONIBLE" else "NO DISPONIBLE",
            colorBoton = if (recurso.disponible) tuColorPrincipal else ColorNoDisponible,
            colorFondoCard = Color.White, // Fondo BLANCO
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

    // Usamos el color de estado como color del borde para alto contraste
    val colorBorde = visuals.colorEstadoTexto
    val anchoBorde = if (visuals.estadoIcon != null) 2.dp else 1.dp // Borde más grueso si hay estado definido

    Card(
        modifier = modifier.border(anchoBorde, colorBorde, RoundedCornerShape(16.dp)), // 🚨 Borde de color de estado
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp), // Sin sombra para un diseño plano
        colors = CardDefaults.cardColors(containerColor = visuals.colorFondoCard) // Fondo blanco
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nombre del Recurso: FUENTE MÁS GRANDE y NEGRITA
                Text(
                    text = recurso.nombre,
                    style = MaterialTheme.typography.headlineSmall.copy( // 🚨 headlineSmall para mayor tamaño
                        fontWeight = FontWeight.ExtraBold, // Mayor peso de fuente
                        color = visuals.colorEstadoTexto // Color del estado
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Icono y Texto de Estado
                visuals.estadoIcon?.let { icon ->
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            imageVector = icon,
                            contentDescription = visuals.textoEstado,
                            tint = visuals.colorEstadoTexto,
                            modifier = Modifier.size(36.dp) // 🚨 Icono más grande
                        )
                        Text(
                            text = visuals.textoEstado,
                            style = MaterialTheme.typography.titleSmall, // 🚨 titleSmall para mayor legibilidad del estado
                            color = visuals.colorEstadoTexto,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp)) // Espacio aumentado

            // Descripción: Mantener legible, pero no tan prominente
            Text(
                text = recurso.descripcion ?: "Sin descripción",
                style = MaterialTheme.typography.bodyLarge, // 🚨 bodyLarge para mejor lectura
                color = Color.DarkGray
            )

            Spacer(Modifier.height(20.dp)) // Espacio aumentado

            // Botón de Reserva/Estado: FUENTE MÁS GRANDE
            Button(
                onClick = onButtonClick,
                enabled = finalEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp), // 🚨 Altura de botón fija y más grande
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = visuals.colorBoton)
            ) {
                Text(
                    text = visuals.textoBoton,
                    style = MaterialTheme.typography.titleMedium, // 🚨 titleMedium para texto de botón más grande
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