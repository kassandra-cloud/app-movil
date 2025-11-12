package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReunionesRealizadasScreen(
    vm: ReunionesViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpen: (ReunionDto) -> Unit = {}
) {
    // Observa el slice de REALIZADA
    val st by vm.realizadas.collectAsState(initial = ReunionesViewModel.SectionState())

    LaunchedEffect(Unit) { vm.refresh(ReunionEstado.REALIZADA) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Reuniones realizadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            st.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            st.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${st.error}")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.refresh(ReunionEstado.REALIZADA) }) { Text("Reintentar") }
                }
            }

            st.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No hay reuniones realizadas.") }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(st.items) { r -> ReunionRealizadaCard(r) { onOpen(r) } }

                // Paginación: pide más al llegar al final
                item {
                    if (st.hasNext && !st.loading) {
                        LaunchedEffect(st.page) { vm.nextPage(ReunionEstado.REALIZADA) }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReunionRealizadaCard(
    r: ReunionDto,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF2F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(r.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("${r.tipo} · ${formateaFecha(r.fecha)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            r.tabla?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

private fun formateaFecha(iso: String): String = try {
    val dt = runCatching { OffsetDateTime.parse(iso) }.getOrNull()
        ?: LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toOffsetDateTime()
    val local = dt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    local.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es")))
} catch (_: Exception) {
    iso.replace('T', ' ').take(16)
}
