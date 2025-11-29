package com.example.proyecto.ui.actas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ActaDto
import com.example.proyecto.ui.theme.AppColors // 👈 Importamos tus colores
import com.example.proyecto.viewmodel.ActasViewModel
import com.example.proyecto.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import java.text.Normalizer

// --- COLORES SEMÁNTICOS (Fijos por significado) ---
val ColorAprobado = Color(0xFF16A34A) // Verde
val ColorNoAprobado = Color(0xFF9CA3AF) // Gris

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActasScreen(
    vm: ActasViewModel = viewModel(),
    loginVm: LoginViewModel = viewModel(),
    onVerActa: (ActaDto) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val actas by vm.actas.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val asistenciasMap by vm.asistencias.collectAsState()
    val loginUi by loginVm.uiState.collectAsState()

    var actaPreview by remember { mutableStateOf<ActaDto?>(null) }

    // Lógica del ViewModel y Auto-refresh
    LaunchedEffect(Unit) { vm.cargarActas(loginUi.token ?: "") }

    LaunchedEffect("auto-refresh-actas") {
        while (true) {
            delay(10_000)
            vm.cargarActas(loginUi.token ?: "")
        }
    }

    BackHandler { onBack() }

    Scaffold(
        // 1. BARRA SUPERIOR CON GRADIENTE (Igual que Anuncios)
        topBar = {
            TopAppBar(
                title = { Text("Actas de Reuniones") },
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
        }
        // Nota: Eliminé el bottomBar "Volver" porque ya existe la flecha arriba (estándar Android)
    ) { paddingValues ->

        // 2. CONTENIDO PRINCIPAL
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // ✅ Fondo Dinámico
                .padding(paddingValues)
        ) {
            when {
                loading && actas.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                error != null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorBox(
                        message = error ?: "Error",
                        onDismiss = { vm.limpiarError() },
                        onRetry = { vm.cargarActas(loginUi.token ?: "") }
                    )
                }

                actas.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay actas disponibles.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(actas, key = { it.reunion }) { a ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actaPreview = a },
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(20.dp),
                                // ✅ Tarjeta Dinámica (Blanco/Gris)
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            a.reunionTitulo,
                                            // ✅ Título adaptativo
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        StatusPill(
                                            text = if (a.aprobada) "Aprobada" else "No aprobada",
                                            positive = a.aprobada
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        a.reunionFecha,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    if (!a.resumen.isNullOrBlank()) {
                                        Text(
                                            a.resumen.take(120) +
                                                    if (a.resumen.length > 120) "..." else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    // Lógica de asistencia
                                    val asistentes = asistenciasMap[a.reunion]
                                    if (asistentes != null && asistentes.isNotEmpty()) {
                                        val tuRegistro = asistentes.first()
                                        val presente = tuRegistro.presente == true
                                        TuAsistenciaRow(if (presente) "Presente" else "Ausente", presente)
                                    } else {
                                        Text(
                                            "Tu asistencia: — sin registro",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    val puedeVerActa = a.aprobada
                                    Button(
                                        onClick = { if (puedeVerActa) onVerActa(a) },
                                        enabled = puedeVerActa,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) { Text("Ver acta") }

                                    if (!puedeVerActa) {
                                        Text(
                                            text = "Disponible cuando el acta esté aprobada",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (actaPreview != null) {
        ActaPreviewDialog(
            acta = actaPreview!!,
            onDismiss = { actaPreview = null },
            onVerActa = {
                onVerActa(it)
                actaPreview = null
            }
        )
    }
}

// --------- AUXILIARES ---------

@Composable
private fun ActaPreviewDialog(acta: ActaDto, onDismiss: () -> Unit, onVerActa: (ActaDto) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                acta.reunionTitulo,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    "Fecha: ${acta.reunionFecha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    acta.resumen ?: "Sin resumen disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerActa(acta) },
                enabled = acta.aprobada,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Ver acta completa") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun TuAsistenciaRow(texto: String, presente: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (presente) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Presente", tint = ColorAprobado)
        } else {
            Icon(Icons.Filled.Close, contentDescription = "Ausente", tint = MaterialTheme.colorScheme.error)
        }
        Text(
            "Tu asistencia: $texto",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean) {
    Surface(
        color = if (positive) ColorAprobado else ColorNoAprobado, // Colores semánticos
        contentColor = Color.White,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorBox(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text("Cerrar")
                }
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}