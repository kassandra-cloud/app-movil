package com.example.proyecto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.ResultadoVotacionDto
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.viewmodel.VotacionesViewModel

@Composable
fun VotacionesScreen(
    token: String,
    onBack: () -> Unit,
    vm: VotacionesViewModel = viewModel()
) {
    val ui = vm.ui.collectAsState().value
    val resultados by vm.resultados.collectAsState()

    LaunchedEffect(token) { vm.cargarAbiertas(token) }
    BackHandler { onBack() }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(brush = Brush.linearGradient(listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                Spacer(Modifier.width(8.dp))
                Text("Votaciones abiertas",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF6C63FF), fontWeight = FontWeight.Bold))
            }

            if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
            ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.height(8.dp))

            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(ui.abiertas) { votacion ->
                    VotacionItem(
                        votacion = votacion,
                        onVote = { opcionId -> vm.votar(token, votacion.id, opcionId) },
                        getResultados = { id -> resultados[id] },
                        onCargarResultados = { id -> vm.cargarResultados(token, id) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Volver")
            }
        }
    }
}

@Composable
private fun VotacionItem(
    votacion: VotacionDto,
    onVote: (Int) -> Unit,
    getResultados: (Int) -> ResultadoVotacionDto?,
    onCargarResultados: (Int) -> Unit
) {
    var mostrarResultados by rememberSaveable { mutableStateOf(false) }
    var yaPidio by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {

            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF6C63FF),
                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(8.dp))
            Text(votacion.pregunta,
                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF6C63FF), fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))

            if (votacion.ya_vote) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Voto Realizado", tint = Color(0xFF8EC5FC))
                    Spacer(Modifier.width(8.dp))
                    Text("Ya votaste. Opción id: ${votacion.opcion_votada_id}", color = Color(0xFF8EC5FC))
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!votacion.ya_vote && !mostrarResultados) {
                votacion.opciones.forEach { opcion ->
                    Button(
                        onClick = { onVote(opcion.id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8EC5FC))
                    ) { Text(opcion.texto, color = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!mostrarResultados) {
                OutlinedButton(
                    onClick = {
                        mostrarResultados = true
                        yaPidio = true
                        onCargarResultados(votacion.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Ver resultados") }
            } else {
                val res = getResultados(votacion.id)
                when {
                    res == null && yaPidio -> {
                        Text("Cargando resultados…", color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onCargarResultados(votacion.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Reintentar") }
                    }
                    res != null -> {
                        ResultadosCard(res)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onCargarResultados(votacion.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Actualizar") }
                            OutlinedButton(
                                onClick = { mostrarResultados = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Ocultar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultadosCard(result: ResultadoVotacionDto) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Resultados (${result.total_votos} votos)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        result.opciones.forEach { op ->
            val pct = if (result.total_votos > 0) op.votos.toFloat() / result.total_votos.toFloat() else 0f
            val pctLabel = "${(pct * 100).toInt()}%"
            Text(op.texto, fontWeight = FontWeight.Medium)
            LinearProgressIndicator(progress = pct, modifier = Modifier.fillMaxWidth().height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${op.votos} votos", color = Color(0xFF616161))
                Text(pctLabel, color = Color(0xFF616161))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
