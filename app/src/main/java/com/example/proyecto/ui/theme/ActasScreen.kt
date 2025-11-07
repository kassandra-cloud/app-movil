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
// 🔑 IMPORTACIONES CLAVE (Asegúrate que estas rutas coincidan con tu proyecto)
import com.example.proyecto.data.ActaDto
import com.example.proyecto.viewmodel.ActasViewModel
import com.example.proyecto.viewmodel.LoginViewModel
import com.example.proyecto.data.AsistenciaDto
import java.text.Normalizer


// 🔑 PALETA DE COLORES PRINCIPAL
val tuColorPrincipal = Color(0xFF33BACC) // Azul/Cian
val webColorSecundario = Color(0xFF66D9CE) // Menta (aunque ya casi no la usaremos directamente)
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
    LaunchedEffect(Unit) { vm.cargarActas() }

    // ✅ CORRECCIÓN DEL AUTO-REFRESH (usa LaunchedEffect y delay)
    LaunchedEffect("auto-refresh-actas") {
        while (true) {
            delay(10_000)
            vm.cargarActas()
        }
    }

    BackHandler { onBack() }

    // Fondo degradado para el encabezado
    val gradientBrush = remember {
        Brush.verticalGradient(listOf(tuColorPrincipal, webColorSecundario))
    }

    Scaffold(
        // 1. BARRA SUPERIOR CON COLOR DE FONDO DEGRADADO
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Aplicamos el degradado solo a esta Columna
                    .background(gradientBrush)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        // Icono en BLANCO para contraste
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Actas de Reuniones",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            // Título en BLANCO
                            color = Color.White
                        )
                    )
                }
            }
        },
        // 2. BOTÓN INFERIOR FIJO
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
        // 3. CONTENIDO PRINCIPAL (Fondo Blanco)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            when {
                // Loader
                loading && actas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tuColorPrincipal)
                }
                // Error
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorBox(
                        message = error ?: "Error",
                        onDismiss = { vm.limpiarError() },
                        onRetry   = { vm.cargarActas() }
                    )
                }
                // Lista Vacía
                actas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay reuniones/actas disponibles.", color = grisOscuroTexto)
                }
                // Contenido
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // ✅ OPTIMIZACIÓN: Aumentamos el espacio entre tarjetas a 20.dp
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
                                // ✅ OPTIMIZACIÓN: Ajustamos el padding interno a 16.dp
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            a.reunion_titulo,
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
                                    Text(a.reunion_fecha, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(10.dp))

                                    if (!a.resumen.isNullOrBlank()) {
                                        Text(
                                            a.resumen.take(120) + if (a.resumen.length > 120) "..." else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    // Lógica de Asistencia
                                    val asistentes = asistenciasMap[a.reunion]
                                    if (asistentes != null) {
                                        val tuRegistro = asistentes.firstOrNull { asis ->
                                            val nu = norm(asis.nombre_usuario)
                                            val nc = norm(asis.nombre_completo)
                                            val rut = norm(asis.rut)
                                            cu.isNotEmpty() && (nu == cu || nc.contains(cu) || cu.contains(nc) || rut == cu)
                                        }
                                        when {
                                            tuRegistro == null ->
                                                Text("Tu asistencia: — sin registro", style = MaterialTheme.typography.bodySmall)
                                            tuRegistro.presente ->
                                                TuAsistenciaRow("Presente", true)
                                            else ->
                                                TuAsistenciaRow("Ausente", false)
                                        }
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

// FUNCIONES AUXILIARES (Se mantienen igual)

@Composable
private fun ActaPreviewDialog(acta: ActaDto, onDismiss: () -> Unit, onVerActa: (ActaDto) -> Unit) {
    val tuColorPrincipal = Color(0xFF33BACC)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(acta.reunion_titulo, fontWeight = FontWeight.Bold, color = tuColorPrincipal) },
        text = {
            Column {
                Text("Fecha: ${acta.reunion_fecha}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(acta.resumen ?: "Sin resumen disponible.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerActa(acta) },
                enabled = acta.aprobada,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
            ) { Text("Ver acta completa") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(50)) {
                Text("Cerrar", color = tuColorPrincipal)
            }
        }
    )
}

@Composable
private fun TuAsistenciaRow(texto: String, presente: Boolean) {
    val tuColorPrincipal = Color(0xFF33BACC)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (presente) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Presente", tint = tuColorPrincipal)
        } else {
            Icon(Icons.Filled.Close, contentDescription = "Ausente", tint = MaterialTheme.colorScheme.error)
        }
        Text("Tu asistencia: $texto", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean) {
    // Eliminamos la referencia a webColorSecundario aquí para asegurar que siempre use tuColorPrincipal para 'positive'
    // val webColorSecundario = Color(0xFF66D9CE) // Comentado o eliminado
    val grisClaroFondo = Color(0xFFEEEEEE)
    val grisOscuroTexto = Color(0xFF616161)

    Surface(
        // Aseguramos que si es positivo (aprobada), use tuColorPrincipal (cian)
        color = if (positive) tuColorPrincipal else grisClaroFondo,
        contentColor = if (positive) Color.White else grisOscuroTexto,
        shape = RoundedCornerShape(50)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ErrorBox(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(50)) { Text("Cerrar") }
                Button(onClick = onRetry, shape = RoundedCornerShape(50)) { Text("Reintentar") }
            }
        }
    }
}

private fun norm(s: String?): String =
    Normalizer.normalize(s?.trim()?.lowercase() ?: "", Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]"), "")

@Preview(showBackground = true)
@Composable
fun PreviewActasScreen() {
    val actasEjemplo = listOf(
        ActaDto(1, "Contenido", true,  "Reunión de aprobación de cuentas", "2025-11-01", "Ordinaria", "Resumen demo"),
        ActaDto(2, "Contenido", false, "Reunión de planificación",  "2025-10-15", "Extraordinaria", "Resumen breve")
    )
    val tuColorPrincipal = Color(0xFF33BACC)
    // val webColorSecundario = Color(0xFF66D9CE) // Comentado también en el Preview
    val gradientBrush = Brush.verticalGradient(listOf(tuColorPrincipal, Color(0xFF66D9CE))) // Puedes mantener el degradado con el secundario si te gusta

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
                        colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Volver")
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White),
                verticalArrangement = Arrangement.spacedBy(20.dp), // Espacio optimizado
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(actasEjemplo, key = { it.reunion }) { a ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { },
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) { // Padding optimizado
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(a.reunion_titulo, style = MaterialTheme.typography.titleLarge.copy(color = tuColorPrincipal), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                // Esta llamada a StatusPill ahora usará la versión modificada
                                StatusPill(if (a.aprobada) "Aprobada" else "No aprobada", a.aprobada)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(a.reunion_fecha, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))

                            TuAsistenciaRow(if (a.aprobada) "Presente" else "Ausente", a.aprobada)

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { },
                                enabled = a.aprobada,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
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