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
// AUXILIARES
// -------------------------------------------------------------------------

/* ---------- Encabezado de día ---------- */

@Composable
private fun DiaHeaderProgramadas(dia: LocalDate) {
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

private val CHILE_TZ_PROGRAMADAS: ZoneId = ZoneId.of("America/Santiago")

/** Acepta ISO con o sin offset. Con offset → convierte a Chile; sin offset → interpreta como hora local Chile. */
private fun parseChileProgramadas(iso: String): LocalDateTime = runCatching {
    // Intenta parsear como ISO 8601 con Offset (esperado desde Django sin format)
    OffsetDateTime.parse(iso).atZoneSameInstant(CHILE_TZ_PROGRAMADAS).toLocalDateTime()
}.getOrElse {
    // Falla si no tiene offset, intenta como Local
    LocalDateTime.parse(iso)
}

/* ---------- Tarjeta ---------- */

@Composable
private fun ReunionProgramadaCard(
    r: ReunionDto,
    onClick: () -> Unit
) {
    // Usa 'r.fechaInicio' (sincronizado con DTO)
    val inicio = parseChileProgramadas(r.fechaInicio)
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
            // Manejo seguro del campo opcional 'tabla'
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
fun ReunionesProgramadasScreen(
    onBack: () -> Unit = {},
    onOpen: (ReunionDto) -> Unit = {},
    vm: ReunionesViewModel = viewModel()
) {
    val st by vm.programadas.collectAsState(initial = ReunionesViewModel.SectionState())

    // Carga inicial
    LaunchedEffect(Unit) {
        // Solo recarga si no ha cargado y no está cargando
        if (!st.initialized && !st.loading) {
            vm.refresh(ReunionEstado.PROGRAMADA)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Reuniones programadas") },
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
            st.loading && st.items.isEmpty() -> Box( // Muestra progreso solo si no hay datos previos
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
                        onClick = { vm.refresh(ReunionEstado.PROGRAMADA) },
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
                Text("No hay reuniones programadas.")
            }

            else -> {
                val grupos = remember(st.items) {
                    st.items
                        // Usa 'it.fechaInicio'
                        .sortedByDescending { parseChileProgramadas(it.fechaInicio) }
                        // Agrupa por fecha (día)
                        .groupBy { parseChileProgramadas(it.fechaInicio).toLocalDate() }
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
                            DiaHeaderProgramadas(dia)
                        }
                        items(
                            items = lista,
                            // Usa 'it.fechaInicio'
                            key = { it.id ?: "${it.titulo}-${it.fechaInicio}" }
                        ) { r ->
                            ReunionProgramadaCard(r) { onOpen(r) }
                        }
                    }
                    // Implementación de paginación infinita (si aplica)
                    item {
                        if (st.hasNext || st.loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (st.loading) {
                                    CircularProgressIndicator()
                                } else if (st.hasNext) {
                                    // Trigger para cargar más cuando se llega al final
                                    SideEffect { vm.nextPage(ReunionEstado.PROGRAMADA) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}