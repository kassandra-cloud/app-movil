package com.example.proyecto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.votaciones.VotacionDto
import com.example.proyecto.viewmodel.VotacionesViewModel

@Composable
fun VotacionesScreen(token: String, vm: VotacionesViewModel = viewModel()) {
    val ui = vm.ui.collectAsState().value

    LaunchedEffect(token) { vm.cargarAbiertas(token) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Votaciones abiertas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (ui.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        LazyColumn(Modifier.fillMaxSize()) {
            items(ui.abiertas) { v ->
                VotacionItem(v) { opcionId -> vm.votar(token, v.id, opcionId) }
            }
        }
    }
}

@Composable
private fun VotacionItem(v: VotacionDto, onVote: (Int) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(v.pregunta, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (v.ya_vote) {
                Text("Ya votaste. Opción id: ${v.opcion_votada_id}")
            } else {
                v.opciones.forEach { op ->
                    Button(onClick = { onVote(op.id) }, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(op.texto)
                    }
                }
            }
        }
    }
}
