package com.example.proyecto.ui.actas

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ActaDto
import com.example.proyecto.viewmodel.ActasViewModel
import com.example.proyecto.viewmodel.LoginViewModel
import java.text.Normalizer

// 🔑 PALETA DE COLORES PRINCIPAL MODIFICADA AL AZUL VIBRANTE
val tuColorPrincipal = Color(0xFF42A5F5) // ⬅️ ¡NUEVO AZUL PRINCIPAL!
val webColorSecundario = Color(0xFF1E88E5) // ⬅️ ¡NUEVO AZUL SECUNDARIO para el degradado!
val grisClaroFondo = Color(0xFFEEEEEE) // Fondo gris muy claro para "No Aprobada"
val grisOscuroTexto = Color(0xFF616161) // Texto gris oscuro para "No Aprobada"

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
    val cu = remember(loginUi.currentUser) { norm(loginUi.currentUser) }

    var actaPreview by remember { mutableStateOf<ActaDto?>(null) }

    // Lógica del ViewModel y Auto-refresh
    LaunchedEffect(Unit) { vm.cargarActas(loginUi.token ?: "") } // Pasar token

    // ✅ AUTO-REFRESH
    LaunchedEffect("auto-refresh-actas") {
        while (true) {
            delay(10_000)
            vm.cargarActas(loginUi.token ?: "") // Pasar token
        }
    }

    BackHandler { onBack() }

    // Fondo degradado para el encabezado
    val gradientBrush = remember {
        Brush.verticalGradient(listOf(tuColorPrincipal, webColorSecundario))
    }

    Scaffold(
        // 1. BARRA SUPERIOR
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Actas de Reuniones",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        },
        // 2. BOTÓN INFERIOR
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver")
                }
            }
        }
    ) { paddingValues ->
        // 3. CONTENIDO PRINCIPAL
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            when {
                loading && actas.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = tuColorPrincipal)
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
                    Text("No hay reuniones/actas disponibles.", color = grisOscuroTexto)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(actas, key = { it.reunion }) { a ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actaPreview = a },
                                elevation = CardDefaults.cardElevation(6.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            a.reunionTitulo,
                                            style = MaterialTheme.typography.titleLarge.copy(color = tuColorPrincipal),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatusPill(
                                            text = if (a.aprobada) "Aprobada" else "No aprobada",
                                            positive = a.aprobada
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(a.reunionFecha, style = MaterialTheme.typography.bodySmall)
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

                                    // 🔹 LÓGICA DE ASISTENCIA SIMPLIFICADA
                                    val asistentes = asistenciasMap[a.reunion]
                                    if (asistentes != null && asistentes.isNotEmpty()) {
                                        val tuRegistro = asistentes.first()
                                        val presente = tuRegistro.presente == true

                                        if (presente) {
                                            TuAsistenciaRow("Presente", true)
                                        } else {
                                            TuAsistenciaRow("Ausente", false)
                                        }
                                    } else {
                                        Text(
                                            "Tu asistencia: — sin registro",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    val puedeVerActa = a.aprobada
                                    Button(
                                        onClick = { if (puedeVerActa) onVerActa(a) },
                                        enabled = puedeVerActa,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
                                    ) { Text("Ver acta") }

                                    if (!puedeVerActa) {
                                        Text(
                                            text = "Disponible cuando el acta esté aprobada",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
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
    val principalColor = tuColorPrincipal
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                acta.reunionTitulo,
                fontWeight = FontWeight.Bold,
                color = principalColor
            )
        },
        text = {
            Column {
                Text("Fecha: ${acta.reunionFecha}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    acta.resumen ?: "Sin resumen disponible.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerActa(acta) },
                enabled = acta.aprobada,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = principalColor)
            ) { Text("Ver acta completa") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(50)) {
                Text("Cerrar", color = principalColor)
            }
        }
    )
}

@Composable
private fun TuAsistenciaRow(texto: String, presente: Boolean) {
    val principalColor = tuColorPrincipal
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (presente) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Presente", tint = principalColor)
        } else {
            Icon(Icons.Filled.Close, contentDescription = "Ausente", tint = MaterialTheme.colorScheme.error)
        }
        Text("Tu asistencia: $texto", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean) {
    val principalColor = tuColorPrincipal
    val grisClaroFondo = grisClaroFondo
    val grisOscuroTexto = grisOscuroTexto

    Surface(
        color = if (positive) principalColor else grisClaroFondo,
        contentColor = if (positive) Color.White else grisOscuroTexto,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge
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
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(50)) {
                    Text("Cerrar")
                }
                Button(onClick = onRetry, shape = RoundedCornerShape(50)) {
                    Text("Reintentar")
                }
            }
        }
    }
}

private fun norm(s: String?): String =
    Normalizer.normalize(s?.trim()?.lowercase() ?: "", Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]"), "")

// PREVIEW SOLO PARA DISEÑO (usa datos falsos)
@Preview(showBackground = true)
@Composable
fun PreviewActasScreen() {
    val actasEjemplo = listOf(
        ActaDto(
            reunion = 1,
            contenido = "Contenido",
            aprobada = true,
            reunionTitulo = "Reunión de aprobación de cuentas",
            reunionFecha = "2025-11-01",
            reunionTipo = "Ordinaria",
            autorUsername = "admin_user",
            resumen = "Resumen demo"
        ),
        ActaDto(
            reunion = 2,
            contenido = "Contenido",
            aprobada = false,
            reunionTitulo = "Reunión de planificación",
            reunionFecha = "2025-10-15",
            reunionTipo = "Extraordinaria",
            autorUsername = "kassandra_user",
            resumen = "Resumen breve"
        )
    )

    val principalColor = tuColorPrincipal
    val secondaryColor = webColorSecundario
    val gradientBrush = Brush.verticalGradient(listOf(principalColor, secondaryColor))

    MaterialTheme {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradientBrush)
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Actas de Reuniones",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = principalColor)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Volver")
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(actasEjemplo, key = { it.reunion }) { a ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { },
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    a.reunionTitulo,
                                    style = MaterialTheme.typography.titleLarge.copy(color = principalColor),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(
                                    if (a.aprobada) "Aprobada" else "No aprobada",
                                    a.aprobada
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(a.reunionFecha, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))

                            TuAsistenciaRow(
                                if (a.aprobada) "Presente" else "Ausente",
                                a.aprobada
                            )

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { },
                                enabled = a.aprobada,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = principalColor)
                            ) {
                                Text("Ver acta")
                            }
                        }
                    }
                }
            }
        }
    }
}
