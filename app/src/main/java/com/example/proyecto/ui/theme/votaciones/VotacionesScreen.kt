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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.viewmodel.VotacionesViewModel
// Importamos el Enum del filtro y la versión corregida del ViewModel
import com.example.proyecto.viewmodel.VotacionFilter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotacionesScreen(
    token: String,
    onBack: () -> Unit,
    vm: VotacionesViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val resultados by vm.resultados.collectAsState()

    // 🔥 NUEVOS ESTADOS PARA FILTRADO
    val filteredVotaciones by vm.filteredVotaciones.collectAsState()
    val currentFilter by vm.filtro.collectAsState()
    val tabs = listOf(VotacionFilter.EN_CURSO, VotacionFilter.CERRADAS)

    var votacionPreview by remember { mutableStateOf<VotacionDto?>(null) }

    // Estados para MFA
    var showVerificationDialog by remember { mutableStateOf(false) }
    var pendingVotacionId by remember { mutableStateOf<Int?>(null) }
    var pendingOpcionId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(token) { vm.cargarAbiertas(token) }

    LaunchedEffect(ui.mensaje) {
        // Retraso para mensajes de la pantalla principal (no de reenvío)
        if (ui.mensaje != null && ui.mensaje?.contains("reenviado") != true) {
            delay(3000)
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
            // BARRA SUPERIOR ESTÁNDAR CON GRADIENTE
            TopAppBar(
                title = { Text("Votaciones abiertas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.background(AppColors.GradientePrincipal)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Fondo Dinámico
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)

            ui.error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            // Muestra mensaje solo si no estamos en el diálogo de verificación (para evitar duplicados)
            if (!showVerificationDialog) {
                ui.mensaje?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // 🔥 AÑADIDO: TabRow para seleccionar el estado de la votación
            TabRow(
                selectedTabIndex = tabs.indexOf(currentFilter),
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { filter ->
                    Tab(
                        selected = currentFilter == filter,
                        onClick = { vm.setFilter(filter) }, // Llama a la función del ViewModel
                        text = {
                            Text(
                                when (filter) {
                                    VotacionFilter.EN_CURSO -> "Votaciones en curso"
                                    VotacionFilter.CERRADAS -> "Votaciones cerradas"
                                    VotacionFilter.TODAS -> "Todas" // Solo se deja para completar el enum, no se usa aquí
                                }
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 🔥 USAMOS la lista filtrada: 'filteredVotaciones'
            if (filteredVotaciones.isEmpty() && !ui.cargando) {
                EmptyVotaciones(onRecargar = { vm.cargarAbiertas(token) })
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(filteredVotaciones, key = { it.id }) { votacion ->
                        VotacionItem(
                            votacion = votacion,
                            onShowResults = {
                                votacionPreview = votacion
                                vm.cargarResultados(token, votacion.id)
                            },
                            onVote = { opcionId ->
                                pendingVotacionId = votacion.id
                                pendingOpcionId = opcionId
                                vm.solicitarCodigo(token)
                                showVerificationDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (votacionPreview != null) {
        VotacionResultadoDialog(
            votacion = votacionPreview!!,
            resultados = resultados[votacionPreview!!.id],
            onDismiss = { votacionPreview = null },
            onRefresh = { vm.cargarResultados(token, votacionPreview!!.id) }
        )
    }

    // Mantenemos la lógica de reenvío
    if (showVerificationDialog) {
        val resendMsg = if (ui.mensaje?.contains("reenviado") == true) ui.mensaje else null

        VerificationVoteDialog(
            onDismiss = {
                showVerificationDialog = false
                vm.clearMessages()
            },
            onConfirm = { codigo ->
                if (pendingVotacionId != null && pendingOpcionId != null) {
                    vm.votar(token, pendingVotacionId!!, pendingOpcionId!!, codigo)
                    showVerificationDialog = false
                    vm.clearMessages()
                }
            },
            onResendCode = { vm.solicitarCodigo(token) },
            resendMessage = resendMsg
        )
    }
}

// Mantenemos la lógica de reenvío
@Composable
fun VerificationVoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onResendCode: () -> Unit,
    resendMessage: String?
) {
    var code by remember { mutableStateOf("") }
    var showResendFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(resendMessage) {
        if (resendMessage != null) {
            showResendFeedback = true
            delay(3000)
            showResendFeedback = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Seguridad de Voto") },
        text = {
            Column {
                Text(
                    "Hemos enviado un código a tu correo. Ingrésalo para confirmar tu voto.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("Código (6 dígitos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = onResendCode,
                    modifier = Modifier.align(Alignment.End),
                    enabled = !showResendFeedback
                ) {
                    Text(
                        if (showResendFeedback) resendMessage ?: "Reenviando..." else "¿No te llegó? Vuelve a pedirlo",
                        color = if (showResendFeedback) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Validar y Votar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

// Mantenemos la lógica de visualización de ítems
@Composable
private fun VotacionItem(votacion: VotacionDto, onVote: (Int) -> Unit, onShowResults: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))

            Text(
                votacion.pregunta,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))

            if (votacion.yaVote) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.CheckCircle, "Voto Realizado", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    val opTexto = votacion.opciones.find { it.id == votacion.opcionVotadaId }?.texto ?: "Tu opción"
                    Text("Ya votaste: $opTexto", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
            }

            if (votacion.estaAbierta) { // Si está abierta, se muestran las opciones de voto (si no ha votado)
                if (!votacion.yaVote) {
                    votacion.opciones.forEach { opcion ->
                        Button(
                            onClick = { onVote(opcion.id) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(opcion.texto, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    // Muestra el mensaje de espera si ya votó y la votación está abierta
                    Text(
                        "Resultados disponibles al cierre de la votación.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Solo muestra el botón de resultados si NO está abierta
            if (!votacion.estaAbierta) {
                OutlinedButton(
                    onClick = onShowResults,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Ver resultados finales")
                }
            }
        }
    }
}

@Composable
private fun VotacionResultadoDialog(votacion: VotacionDto, resultados: ResultadoVotacionDto?, onDismiss: () -> Unit, onRefresh: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                votacion.pregunta,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            if (resultados == null) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            else ResultadosCard(resultados)
        },
        confirmButton = {
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Actualizar") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cerrar") } },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ResultadosCard(result: ResultadoVotacionDto) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Total votos: ${result.totalVotos}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        result.opciones.forEach { op ->
            val pct = if (result.totalVotos > 0) op.votos.toFloat() / result.totalVotos else 0f
            Text(op.texto, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)

            LinearProgressIndicator(
                progress = pct,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                "${(pct*100).toInt()}% (${op.votos})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyVotaciones(onRecargar: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Info,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                "No hay votaciones",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRecargar) { Text("Actualizar") }
        }
    }
}