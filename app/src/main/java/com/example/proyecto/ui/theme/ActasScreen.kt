package com.example.proyecto.ui.actas

// Imports añadidos
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
// ---

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.proyecto.data.ActaDto
import com.example.proyecto.viewmodel.ActasViewModel
import com.example.proyecto.viewmodel.LoginViewModel
import java.text.Normalizer

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

    LaunchedEffect(Unit) { vm.cargarActas() }
    BackHandler { onBack() }

    // Definimos el gradiente inspirado en tu imagen
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDCD8FF), // Un lila muy claro
            Color(0xFFC0B8FF)  // Un lila un poco más oscuro
        )
    )

    // El Box aplica el fondo de gradiente a toda la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                ErrorBox(
                    message = error ?: "Error",
                    onDismiss = { vm.limpiarError() },
                    onRetry = { vm.refrescar() }
                )
            }
            actas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay reuniones/actas disponibles.")
            }
            // 3. Estructura cambiada a Column
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // <-- Padding general
            ) {
                // 4. LazyColumn ahora tiene weight(1f)
                LazyColumn(
                    // Se quita el padding de aquí
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(actas, key = { it.reunion }) { a ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actaPreview = a },
                            elevation = CardDefaults.cardElevation(6.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        a.reunion_titulo,
                                        style = MaterialTheme.typography.titleLarge,
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

                                val asistentes = asistenciasMap[a.reunion]
                                when {
                                    asistentes == null ->
                                        Text("Tu asistencia: cargando…", style = MaterialTheme.typography.bodySmall)
                                    else -> {
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
                                }

                                Spacer(Modifier.height(12.dp))

                                val puedeVerActa = a.aprobada
                                Button(
                                    onClick = { if (puedeVerActa) onVerActa(a) },
                                    enabled = puedeVerActa,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50) // Botón "Ver acta" sigue siendo píldora
                                ) {
                                    Text("Ver acta")
                                }
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
                } // Fin de LazyColumn

                // 5. Botón "Volver" añadido (estilo corregido)
                Spacer(Modifier.height(8.dp)) // Spacer añadido
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(), // Padding individual quitado
                    shape = RoundedCornerShape(16.dp), // Forma corregida
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)) // Color de VotacionesScreen
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver")
                }

            } // Fin de Column
        }
    }

    // Dialog de previsualización
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

// ... (Las funciones ActaPreviewDialog, TuAsistenciaRow, StatusPill, ErrorBox y norm NO CAMBIAN) ...
@Composable
private fun ActaPreviewDialog(
    acta: ActaDto,
    onDismiss: () -> Unit,
    onVerActa: (ActaDto) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // Forma redondeada para el diálogo
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(acta.reunion_titulo, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Fecha: ${acta.reunion_fecha}", style = MaterialTheme.typography.bodySmall)
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
                // Forma de píldora
                shape = RoundedCornerShape(50)
            ) {
                Text("Ver acta completa")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                // Forma de píldora
                shape = RoundedCornerShape(50)
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
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Presente",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Ausente",
                tint = MaterialTheme.colorScheme.error
            )
        }
        Text("Tu asistencia: $texto", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean) {
    Surface(
        color = if (positive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (positive) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
        // Forma de píldora
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
        // Forma redondeada
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
                    // Forma de píldora
                    shape = RoundedCornerShape(50)
                ) { Text("Cerrar") }
                Button(
                    onClick = onRetry,
                    // Forma de píldora
                    shape = RoundedCornerShape(50)
                ) { Text("Reintentar") }
            }
        }
    }
}

/** Normaliza: quita acentos, espacios y símbolos; minúsculas. */
private fun norm(s: String?): String =
    Normalizer.normalize(s?.trim()?.lowercase() ?: "", Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]"), "")

// ----------- PREVIEW PARA ANDROID STUDIO -----------
// 6. Preview actualizado para reflejar la nueva estructura
@Preview(showBackground = true)
@Composable
fun PreviewActasScreen() {

    data class FakeAsistencia(
        val nombre_usuario: String,
        val nombre_completo: String,
        val rut: String,
        val presente: Boolean
    )

    val actasEjemplo = listOf(
        ActaDto(
            reunion = 1,
            contenido = "Contenido completo del acta de ejemplo.",
            aprobada = true,
            reunion_titulo = "Reunión de ejemplo",
            reunion_fecha = "2025-11-01",
            reunion_tipo = "Ordinaria",
            resumen = "Este es un resumen de ejemplo para la previsualización de la acta. Aquí puedes ver cómo se muestra el texto resumido en la tarjeta y el diálogo."
        ),
        ActaDto(
            reunion = 2,
            contenido = "Otro contenido de acta.",
            aprobada = false,
            reunion_titulo = "Reunión pendiente",
            reunion_fecha = "2025-10-15",
            reunion_tipo = "Extraordinaria",
            resumen = "Resumen breve de una acta no aprobada."
        )
    )

    val asistenciasFake = mapOf(
        1 to listOf(
            FakeAsistencia(
                nombre_usuario = "usuario",
                nombre_completo = "Usuario Ejemplo",
                rut = "12345678-9",
                presente = true
            )
        ),
        2 to listOf(
            FakeAsistencia(
                nombre_usuario = "usuario",
                nombre_completo = "Usuario Ejemplo",
                rut = "12345678-9",
                presente = false
            )
        )
    )

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDCD8FF), // Un lila muy claro
            Color(0xFFC0B8FF)  // Un lila un poco más oscuro
        )
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Padding general
            ) { // Columna principal
                LazyColumn(
                    // Se quita el padding de aquí
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Con weight
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(actasEjemplo, key = { it.reunion }) { a ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(6.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        a.reunion_titulo,
                                        style = MaterialTheme.typography.titleLarge,
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

                                val asistencia = asistenciasFake[a.reunion]?.first()
                                if (asistencia != null) {
                                    if (asistencia.presente) {
                                        TuAsistenciaRow("Presente", true)
                                    } else {
                                        TuAsistenciaRow("Ausente", false)
                                    }
                                } else {
                                    Text("Tu asistencia: — sin registro", style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = { },
                                    enabled = a.aprobada,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50) // Botón píldora
                                ) {
                                    Text("Ver acta")
                                }
                                if (!a.aprobada) {
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
                } // Fin de LazyColumn (preview)

                // Botón "Volver" (preview)
                Spacer(Modifier.height(8.dp)) // Spacer añadido
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(), // Padding individual quitado
                    shape = RoundedCornerShape(16.dp), // Forma corregida
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver")
                }
            } // Fin de Column (preview)
        }
    }
}