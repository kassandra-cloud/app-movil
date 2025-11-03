package com.example.proyecto.ui

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay // Importación necesaria para delay (si se usa en auto-refresh)
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.viewmodel.VotacionesViewModel

// 🔑 PALETA DE COLORES PRINCIPAL
val tuColorPrincipal = Color(0xFF33BACC) // Azul/Cian
val webColorSecundario = Color(0xFF66D9CE) // Menta
val fondoTarjetaVotacion = Color(0xFFF3E5F5) // Lila muy suave (para acento o fondo de opciones)
val grisOscuroTexto = Color(0xFF616161) // Texto gris oscuro

@Composable
fun VotacionesScreen(
    token: String,
    onBack: () -> Unit,
    vm: VotacionesViewModel = viewModel()
) {
    val ui = vm.ui.collectAsState().value
    val resultados by vm.resultados.collectAsState()

    // Gradiente para el encabezado
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

            if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
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
    val tuColorPrincipal = Color(0xFF33BACC)
    val webColorSecundario = Color(0xFF66D9CE)

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
                tint = tuColorPrincipal,
                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))
            Text(
                votacion.pregunta,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = tuColorPrincipal,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))

            // Ya votaste
            if (votacion.ya_vote) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Voto Realizado", tint = webColorSecundario)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ya votaste. Opción: ${votacion.opciones.first { it.id == votacion.opcion_votada_id }.texto}",
                        color = webColorSecundario
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Botones de Voto
            if (!votacion.ya_vote) {
                votacion.opciones.forEach { opcion ->
                    Button(
                        onClick = { onVote(opcion.id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = webColorSecundario)
                    ) { Text(opcion.texto, color = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Botón de Previsualización/Resultados
            OutlinedButton(
                onClick = onShowResults,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tuColorPrincipal)
            ) {
                Text(if (votacion.ya_vote) "Ver mi voto y resultados" else "Ver resultados")
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
    val tuColorPrincipal = Color(0xFF33BACC)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(votacion.pregunta, fontWeight = FontWeight.Bold, color = tuColorPrincipal) },
        text = {
            Column {
                if (votacion.ya_vote) {
                    Text(
                        "Tu voto: ${votacion.opciones.first { it.id == votacion.opcion_votada_id }.texto}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                }

                when (resultados) {
                    null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = tuColorPrincipal)
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
                colors = ButtonDefaults.buttonColors(containerColor = tuColorPrincipal)
            ) { Text("Actualizar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(50)) {
                Text("Cerrar", color = tuColorPrincipal)
            }
        }
    )
}

// ... (ResultadosCard y EmptyVotaciones se mantienen igual)

@Composable
private fun ResultadosCard(result: ResultadoVotacionDto) {
    val tuColorPrincipal = Color(0xFF33BACC)
    val grisOscuroTexto = Color(0xFF616161)

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            "Resultados (${result.total_votos} votos)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        result.opciones.forEach { op ->
            val pct = if (result.total_votos > 0) op.votos.toFloat() / result.total_votos.toFloat() else 0f
            val pctLabel = "${(pct * 100).toInt()}%"
            Text(op.texto, fontWeight = FontWeight.Medium)
            // Barra de progreso con color principal
            LinearProgressIndicator(
                progress = pct,
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = tuColorPrincipal
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
    val tuColorPrincipal = Color(0xFF33BACC)

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
                tint = tuColorPrincipal,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No hay votaciones abiertas",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = tuColorPrincipal,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(6.dp))
            Text("Cuando se publiquen, aparecerán aquí.", color = Color(0xFF616161))
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