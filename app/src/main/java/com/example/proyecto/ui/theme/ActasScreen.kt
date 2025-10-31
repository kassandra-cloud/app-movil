package com.example.proyecto.ui.actas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    onVerActa: (ActaDto) -> Unit = {}
) {
    val actas by vm.actas.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val asistenciasMap by vm.asistencias.collectAsState()

    val loginUi by loginVm.uiState.collectAsState()
    val cu = remember(loginUi.currentUser) { norm(loginUi.currentUser) }

    LaunchedEffect(Unit) { vm.cargarActas() }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> ErrorBox(
            message = error ?: "Error",
            onDismiss = { vm.limpiarError() },
            onRetry = { vm.refrescar() }
        )

        actas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay reuniones/actas disponibles.")
        }

        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(actas, key = { it.reunion }) { a ->
                Card {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            a.reunion_titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(a.reunion_fecha, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        StatusPill(
                            text = if (a.aprobada) "Aprobada" else "No aprobada",
                            positive = a.aprobada
                        )

                        Spacer(Modifier.height(12.dp))

                        // --- Solo tu asistencia ---
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

                        Spacer(Modifier.height(10.dp))

                        // Botón "Ver acta" solo habilitado si está aprobada
                        val puedeVerActa = a.aprobada
                        TextButton(
                            onClick = { if (puedeVerActa) onVerActa(a) },
                            enabled = puedeVerActa
                        ) {
                            Text("Ver acta")
                        }

                        if (!puedeVerActa) {
                            Text(
                                text = "Disponible cuando el acta esté aprobada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
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
        shape = MaterialTheme.shapes.small
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

/** Normaliza: quita acentos, espacios y símbolos; minúsculas. */
private fun norm(s: String?): String =
    Normalizer.normalize(s?.trim()?.lowercase() ?: "", Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]"), "")
