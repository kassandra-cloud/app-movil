// ui/reuniones/ReunionesRealizadasScreen.kt
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// -------------------------------------------------------------------------
// AUXILIARES (con nombres únicos para evitar conflictos de sobrecarga)
// -------------------------------------------------------------------------

/* ---------- Encabezado de día ---------- */

@Composable
private fun DiaHeaderRealizadas(dia: LocalDate) {
    val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es"))
    Surface(
        color = Color(0xFFF3F6FF),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = dia.format(formato).replaceFirstChar { it.titlecase(Locale("es")) },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/* ---------- Zona horaria Chile y Parser ---------- */

private val CHILE_TZ_REALIZADAS: ZoneId = ZoneId.of("America/Santiago")

/** Acepta ISO con o sin offset. Con offset → convierte a Chile; sin offset → interpreta como hora local Chile. */
private fun parseChileRealizadas(iso: String): LocalDateTime = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(CHILE_TZ_REALIZADAS).toLocalDateTime()
}.getOrElse {
    LocalDateTime.parse(iso)
}

/* ---------- Tarjeta ---------- */

@Composable
private fun ReunionRealizadaCard(
    r: ReunionDto,
    onClick: () -> Unit
) {
    val inicio = parseChileRealizadas(r.fechaInicio)
    val horaFmt = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es"))

    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF2F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                r.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                inicio.format(horaFmt),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            r.tabla?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// PANTALLA PRINCIPAL
// -------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReunionesRealizadasScreen(
    onBack: () -> Unit = {},
    onOpen: (ReunionDto) -> Unit = {},
    vm: ReunionesViewModel = viewModel()
) {
    val st by vm.realizadas.collectAsState(initial = ReunionesViewModel.SectionState())

    // Carga inicial
    LaunchedEffect(Unit) {
        vm.refresh(ReunionEstado.REALIZADA)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Reuniones realizadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF287BFF) // azul
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            st.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            st.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${st.error}")
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.refresh(ReunionEstado.REALIZADA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF287BFF)
                        )
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            st.items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay reuniones realizadas.")
            }

            else -> {
                val grupos = remember(st.items) {
                    st.items
                        .sortedByDescending { parseChileRealizadas(it.fechaInicio) }
                        .groupBy { parseChileRealizadas(it.fechaInicio).toLocalDate() }
                        .toSortedMap(compareByDescending { it })
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grupos.forEach { (dia, lista) ->
                        item("header-$dia") {
                            DiaHeaderRealizadas(dia)
                        }
                        items(
                            items = lista,
                            key = { it.id ?: "${it.titulo}-${it.fechaInicio}" }
                        ) { r ->
                            ReunionRealizadaCard(r) { onOpen(r) }
                        }
                    }
                }
            }
        }
    }
}
