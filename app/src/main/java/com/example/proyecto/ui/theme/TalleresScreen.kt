package com.example.proyecto.ui.talleres

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.TallerDto
import com.example.proyecto.viewmodel.TalleresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalleresScreen(
    token: String,
    onBack: () -> Unit,
    vm: TalleresViewModel = viewModel()
) {
    val state = vm.uiState
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Talleres") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se pudo cargar.\n${state.error}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = vm::cargar) { Text("Reintentar") }
                    }
                }
                state.talleres.isEmpty() -> Text("Sin talleres disponibles", Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.talleres, key = { it.id }) { t ->
                        TallerCard(
                            t = t,
                            inscribiendo = (state.inscribiendoId == t.id),
                            onInscribir = { vm.inscribir(t.id, token) },
                            onDesinscribir = { vm.desinscribir(t.id, token) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TallerCard(
    t: TallerDto,
    inscribiendo: Boolean,
    onInscribir: () -> Unit,
    onDesinscribir: () -> Unit
) {
    val sinCupos = t.cuposDisponibles <= 0
    Card(shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp)) {
            Text(t.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(t.descripcion, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(
                "Totales: ${t.cuposTotales} · Inscritos: ${t.inscritosCount} · Disponibles: ${t.cuposDisponibles}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onDesinscribir, enabled = !inscribiendo) {
                    Text("Desinscribirme")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onInscribir, enabled = !sinCupos && !inscribiendo) {
                    if (inscribiendo) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Inscribiendo...")
                    } else {
                        Text(if (sinCupos) "Sin cupos" else "Inscribirme")
                    }
                }
            }
        }
    }
}
