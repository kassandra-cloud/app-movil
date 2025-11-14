package com.example.proyecto.ui.theme.votaciones

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.votaciones.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.viewmodel.VotacionesViewModel

// 🔑 PALETA DE COLORES PRINCIPAL ACTUALIZADA AL AZUL VIBRANTE
val tuColorPrincipal = Color(0xFF42A5F5) // ⬅️ ¡NUEVO AZUL PRINCIPAL!
val webColorSecundario = Color(0xFF1E88E5) // ⬅️ ¡NUEVO AZUL SECUNDARIO para el degradado y botones!
val fondoTarjetaVotacion = Color(0xFFE3F2FD) // Un azul muy claro (Blue 50) para acento si se necesita
val grisOscuroTexto = Color(0xFF616161) // Texto gris oscuro

@Composable
fun VotacionesScreen(
    token: String,
    onBack: () -> Unit,
    vm: VotacionesViewModel = viewModel()
) {
    val ui = vm.ui.collectAsState().value
    val resultados by vm.resultados.collectAsState()

    // Gradiente para el encabezado (Ahora usa los nuevos azules)
    val gradientBrush = remember {
        Brush.verticalGradient(listOf(tuColorPrincipal, webColorSecundario))
    }

    // ✅ ESTADO DE PREVISUALIZACIÓN: Controla si el diálogo de resultados está abierto
    var votacionPreview by remember { mutableStateOf<VotacionDto?>(null) }

    // Primer fetch y auto-refresh
    LaunchedEffect(token) { vm.cargarAbiertas(token) }

    DisposableEffect(token) {
        vm.startAutoRefresh(token, periodMs = 10_000L) // 10 s
        onDispose { vm.stopAutoRefresh() }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            // ENCABEZADO DEGRADADO (Consistencia visual)
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
                        "Votaciones abiertas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        bottomBar = {
            // Botón Volver fijo
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    // Usando el nuevo azul principal
                    colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver")
                }
            }
        }
    ) { paddingValues ->
        // CUERPO PRINCIPAL (Fondo Blanco)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            // Usando el nuevo azul principal
            if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = tuColorPrincipal)
            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
            ui.mensaje?.let { Text(it, color = tuColorPrincipal) }

            Spacer(Modifier.height(8.dp))

            if (ui.abiertas.isEmpty() && !ui.cargando && ui.error == null) {
                EmptyVotaciones(onRecargar = { vm.cargarAbiertas(token) })
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(
                        items = ui.abiertas,
                        key = { it.id },
                        contentType = { "votacion" }
                    ) { votacion ->
                        VotacionItem(
                            votacion = votacion,
                            // ✅ Lógica de Previsualización: Abre el diálogo y dispara la carga de resultados
                            onShowResults = {
                                votacionPreview = votacion
                                vm.cargarResultados(token, votacion.id)
                            },
                            onVote = { opcionId ->
                                vm.votar(token, votacion.id, opcionId)
                                // Opcional: Podrías añadir lógica aquí para abrir el diálogo si el voto es exitoso
                            }
                        )
                    }
                }
            }
        }
    }

    // ✅ DIÁLOGO DE RESULTADOS
    if (votacionPreview != null) {
        VotacionResultadoDialog(
            votacion = votacionPreview!!,
            resultados = resultados[votacionPreview!!.id],
            onDismiss = { votacionPreview = null },
            onRefresh = { vm.cargarResultados(token, votacionPreview!!.id) }
        )
    }
}

@Composable
private fun VotacionItem(
    votacion: VotacionDto,
    onVote: (Int) -> Unit,
    onShowResults: () -> Unit
) {
    val principal = Color(0xFF42A5F5) // Nuevo Azul Principal
    val secundario = Color(0xFF1E88E5) // Nuevo Azul Secundario

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {

            // Icono de la votación
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = principal, // Usando el nuevo azul
                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                votacion.pregunta,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = principal, // Usando el nuevo azul
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))

            // Ya votaste
            if (votacion.yaVote) { // ⬅️ CORRECCIÓN: ya_vote -> yaVote
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Voto Realizado", tint = secundario) // Usando el nuevo azul secundario
                    Spacer(Modifier.width(8.dp))
                    Text(
                        // ⬅️ CORRECCIÓN: opcion_votada_id -> opcionVotadaId
                        "Ya votaste. Opción: ${votacion.opciones.first { it.id == votacion.opcionVotadaId }.texto}",
                        color = secundario // Usando el nuevo azul secundario
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Botones de Voto
            if (!votacion.yaVote) { // ⬅️ CORRECCIÓN: ya_vote -> yaVote
                votacion.opciones.forEach { opcion ->
                    Button(
                        onClick = { onVote(opcion.id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        // Usando el nuevo azul secundario para las opciones
                        colors = ButtonDefaults.buttonColors(containerColor = secundario)
                    ) { Text(opcion.texto, color = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Botón de Previsualización/Resultados
            OutlinedButton(
                onClick = onShowResults,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                // Usando el nuevo azul principal
                colors = ButtonDefaults.outlinedButtonColors(contentColor = principal)
            ) {
                Text(if (votacion.yaVote) "Ver mi voto y resultados" else "Ver resultados") // ⬅️ CORRECCIÓN: ya_vote -> yaVote
            }
        }
    }
}

// ✅ NUEVO COMPONENTE: Diálogo de Resultados de Votación
@Composable
private fun VotacionResultadoDialog(
    votacion: VotacionDto,
    resultados: ResultadoVotacionDto?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val principal = Color(0xFF42A5F5) // Nuevo Azul Principal

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(votacion.pregunta, fontWeight = FontWeight.Bold, color = principal) }, // Usando el nuevo azul
        text = {
            Column {
                if (votacion.yaVote) { // ⬅️ CORRECCIÓN: ya_vote -> yaVote
                    Text(
                        // ⬅️ CORRECCIÓN: opcion_votada_id -> opcionVotadaId
                        "Tu voto: ${votacion.opciones.first { it.id == votacion.opcionVotadaId }.texto}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                }

                when (resultados) {
                    null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = principal) // Usando el nuevo azul
                            Spacer(Modifier.width(8.dp))
                            Text("Cargando resultados...")
                        }
                    }
                    else -> ResultadosCard(resultados) // Reutilizamos la card de resultados
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = principal) // Usando el nuevo azul
            ) { Text("Actualizar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(50)) {
                Text("Cerrar", color = principal) // Usando el nuevo azul
            }
        }
    )
}

@Composable
private fun ResultadosCard(result: ResultadoVotacionDto) {
    val principal = Color(0xFF42A5F5) // Nuevo Azul Principal
    val grisOscuroTexto = Color(0xFF616161)

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // ⬅️ CORRECCIÓN: total_votos -> totalVotos
        Text(
            "Resultados (${result.totalVotos} votos)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        result.opciones.forEach { op ->
            // ⬅️ CORRECCIÓN: total_votos -> totalVotos
            val pct = if (result.totalVotos > 0) op.votos.toFloat() / result.totalVotos.toFloat() else 0f
            val pctLabel = "${(pct * 100).toInt()}%"
            Text(op.texto, fontWeight = FontWeight.Medium)
            // Barra de progreso con color principal
            LinearProgressIndicator(
                progress = pct,
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = principal // Usando el nuevo azul
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${op.votos} votos", color = grisOscuroTexto)
                Text(pctLabel, color = grisOscuroTexto)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun EmptyVotaciones(onRecargar: () -> Unit) {
    val principal = Color(0xFF42A5F5) // Nuevo Azul Principal

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = principal, // Usando el nuevo azul
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No hay votaciones abiertas",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = principal, // Usando el nuevo azul
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(6.dp))
            Text("Cuando se publiquen, aparecerán aquí.", color = Color(0xFF616161))
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onRecargar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = principal) // Usando el nuevo azul
            ) {
                Text("Actualizar")
            }
        }
    }
}