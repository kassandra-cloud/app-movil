package com.example.proyecto.ui.theme.votaciones

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security // Icono de seguridad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.viewmodel.VotacionesViewModel

val tuColorPrincipal = Color(0xFF42A5F5)
val webColorSecundario = Color(0xFF1E88E5)

@Composable
fun VotacionesScreen(
    token: String,
    onBack: () -> Unit,
    vm: VotacionesViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState() // Usar 'by' para acceso directo
    val resultados by vm.resultados.collectAsState()

    val gradientBrush = remember { Brush.verticalGradient(listOf(tuColorPrincipal, webColorSecundario)) }

    var votacionPreview by remember { mutableStateOf<VotacionDto?>(null) }

    // 🔥 ESTADOS PARA EL DIÁLOGO DE VERIFICACIÓN
    var showVerificationDialog by remember { mutableStateOf(false) }
    var pendingVotacionId by remember { mutableStateOf<Int?>(null) }
    var pendingOpcionId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(token) { vm.cargarAbiertas(token) }

    // Limpiar mensajes (ej: "Voto registrado") después de un tiempo
    LaunchedEffect(ui.mensaje) {
        if (ui.mensaje != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearMessages()
        }
    }

    DisposableEffect(token) {
        vm.startAutoRefresh(token)
        onDispose { vm.stopAutoRefresh() }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(gradientBrush).padding(12.dp, 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                    Spacer(Modifier.width(8.dp))
                    Text("Votaciones abiertas", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(paddingValues).padding(horizontal = 16.dp)) {

            if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = tuColorPrincipal)
            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
            ui.mensaje?.let { Text(it, color = tuColorPrincipal, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))

            if (ui.abiertas.isEmpty() && !ui.cargando) {
                EmptyVotaciones(onRecargar = { vm.cargarAbiertas(token) })
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(ui.abiertas, key = { it.id }) { votacion ->
                        VotacionItem(
                            votacion = votacion,
                            onShowResults = {
                                votacionPreview = votacion
                                vm.cargarResultados(token, votacion.id)
                            },
                            onVote = { opcionId ->
                                // 🔥 AL TOCAR UNA OPCIÓN:
                                // 1. Guardamos qué quería votar
                                pendingVotacionId = votacion.id
                                pendingOpcionId = opcionId
                                // 2. Pedimos al backend que mande el código
                                vm.solicitarCodigo(token)
                                // 3. Mostramos el diálogo
                                showVerificationDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Resultados
    if (votacionPreview != null) {
        VotacionResultadoDialog(
            votacion = votacionPreview!!,
            resultados = resultados[votacionPreview!!.id],
            onDismiss = { votacionPreview = null },
            onRefresh = { vm.cargarResultados(token, votacionPreview!!.id) }
        )
    }

    // 🔥 DIÁLOGO DE SEGURIDAD (MFA)
    if (showVerificationDialog) {
        VerificationVoteDialog(
            onDismiss = { showVerificationDialog = false },
            onConfirm = { codigo ->
                if (pendingVotacionId != null && pendingOpcionId != null) {
                    // 4. ENVIAMOS EL VOTO CON EL CÓDIGO
                    vm.votar(token, pendingVotacionId!!, pendingOpcionId!!, codigo)
                    showVerificationDialog = false
                }
            }
        )
    }
}

// 🔥 COMPONENTE DIÁLOGO DE VERIFICACIÓN
@Composable
fun VerificationVoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Security, null, tint = tuColorPrincipal) },
        title = { Text("Seguridad de Voto") },
        text = {
            Column {
                Text("Hemos enviado un código a tu correo. Ingrésalo para confirmar tu voto.", fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("Código (6 dígitos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
            ) {
                Text("Validar y Votar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ... (Tus componentes VotacionItem, VotacionResultadoDialog, EmptyVotaciones se mantienen IGUAL) ...
@Composable
private fun VotacionItem(votacion: VotacionDto, onVote: (Int) -> Unit, onShowResults: () -> Unit) {
    // Copia aquí tu VotacionItem original, no ha cambiado lógica interna,
    // solo la llamada externa onVote ahora abre el diálogo.
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Icon(Icons.Filled.CheckCircle, null, tint = tuColorPrincipal, modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Text(votacion.pregunta, style = MaterialTheme.typography.titleMedium.copy(color = tuColorPrincipal, fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))

            if (votacion.yaVote) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.CheckCircle, "Voto Realizado", tint = webColorSecundario)
                    Spacer(Modifier.width(8.dp))
                    val opTexto = votacion.opciones.find { it.id == votacion.opcionVotadaId }?.texto ?: "Tu opción"
                    Text("Ya votaste: $opTexto", color = webColorSecundario)
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!votacion.yaVote) {
                votacion.opciones.forEach { opcion ->
                    Button(
                        onClick = { onVote(opcion.id) }, // <-- Esto dispara el diálogo en el padre
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = webColorSecundario)
                    ) { Text(opcion.texto, color = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onShowResults,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tuColorPrincipal)
            ) {
                Text(if (votacion.yaVote) "Ver resultados" else "Ver resultados parciales")
            }
        }
    }
}

// ... (Copiar VotacionResultadoDialog, ResultadosCard y EmptyVotaciones igual que antes)
@Composable
private fun VotacionResultadoDialog(votacion: VotacionDto, resultados: ResultadoVotacionDto?, onDismiss: () -> Unit, onRefresh: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(votacion.pregunta, fontWeight = FontWeight.Bold, color = tuColorPrincipal) },
        text = {
            if (resultados == null) CircularProgressIndicator(color = tuColorPrincipal)
            else ResultadosCard(resultados)
        },
        confirmButton = { Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)) { Text("Actualizar") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun ResultadosCard(result: ResultadoVotacionDto) {
    Column(Modifier.fillMaxWidth()) {
        Text("Total votos: ${result.totalVotos}", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        result.opciones.forEach { op ->
            val pct = if (result.totalVotos > 0) op.votos.toFloat() / result.totalVotos else 0f
            Text(op.texto)
            LinearProgressIndicator(progress = pct, modifier = Modifier.fillMaxWidth().height(8.dp), color = tuColorPrincipal)
            Text("${(pct*100).toInt()}% (${op.votos})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyVotaciones(onRecargar: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Info, null, tint = tuColorPrincipal, modifier = Modifier.size(40.dp))
            Text("No hay votaciones", color = tuColorPrincipal)
            Button(onClick = onRecargar) { Text("Actualizar") }
        }
    }
}