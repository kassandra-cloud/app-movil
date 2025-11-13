// ui/reuniones/ReunionesProgramadasScreen.kt
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
import androidx.compose.ui.text.style.TextAlign
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
fun ReunionesProgramadasScreen(
    onBack: () -> Unit = {},
    onOpen: (ReunionDto) -> Unit = {},
    vm: ReunionesViewModel = viewModel()
) {
    val st by vm.programadas.collectAsState(initial = ReunionesViewModel.SectionState())

    // Semana visible (lunes a domingo) usando zona Chile
    var weekStart by remember { mutableStateOf(LocalDate.now(CHILE_TZ).with(DayOfWeek.MONDAY)) }

    LaunchedEffect(Unit) { vm.refresh(ReunionEstado.PROGRAMADA) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Reuniones programadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            WeekSwitcher(
                weekStart = weekStart,
                onPrev = { weekStart = weekStart.minusWeeks(1) },
                onNext = { weekStart = weekStart.plusWeeks(1) }
            )

            when {
                st.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                st.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${st.error}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.refresh(ReunionEstado.PROGRAMADA) }) { Text("Reintentar") }
                    }
                }
                else -> {
                    val weekEnd = weekStart.plusDays(6)

                    val filtered: List<ReunionDto> = remember(st.items, weekStart) {
                        st.items
                            .filter { dto ->
                                val d = parseChile(dto.fecha).toLocalDate()
                                !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                            }
                            .sortedBy { parseChile(it.fecha) }
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay reuniones esta semana.")
                        }
                    } else {
                        val grupos: Map<LocalDate, List<ReunionDto>> =
                            filtered.groupBy { parseChile(it.fecha).toLocalDate() }
                                .toSortedMap(compareBy { it })

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            grupos.forEach { (dia, lista) ->
                                item("header-$dia") { DiaHeader(dia) }
                                items(
                                    items = lista,
                                    key = { it.id ?: "${it.titulo}-${it.fecha}" }
                                ) { r ->
                                    ReunionProgramadaCard(r) { onOpen(r) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Selector de semana ---------- */

@Composable
private fun WeekSwitcher(
    weekStart: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrev) { Text("◀ Semana") }
        Spacer(Modifier.weight(1f))
        Text(
            "${weekStart.format(fmt)} – ${weekStart.plusDays(6).format(fmt)}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onNext) { Text("Semana ▶") }
    }
}

/* ---------- Tarjeta (sin usar 'fin') ---------- */

@Composable
private fun ReunionProgramadaCard(r: ReunionDto, onClick: () -> Unit) {
    val inicio = parseChile(r.fecha)
    val hora = DateTimeFormatter.ofPattern("HH:mm", Locale("es"))

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.padding(16.dp)) {
            // Bloque de hora
            Surface(
                color = Color(0xFFF0ECFF),
                modifier = Modifier.width(96.dp).heightIn(min = 64.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(inicio.format(hora), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(text = "Programada", bg = Color(0xFF2962FF), fg = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Badge(text = r.tipo ?: "—", bg = Color(0xFFFFF3CD), fg = Color(0xFF7A5B00))
                }
                Spacer(Modifier.height(8.dp))
                Text(r.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                r.tabla?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/* ---------- Utilidades compartidas ---------- */

private val CHILE_TZ: ZoneId = ZoneId.of("America/Santiago")

/** ISO con offset → se convierte a Chile; sin offset → se interpreta como local (Chile). */
private fun parseChile(iso: String): LocalDateTime = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(CHILE_TZ).toLocalDateTime()
}.getOrElse {
    LocalDateTime.parse(iso)
}

@Composable
private fun DiaHeader(dia: LocalDate) {
    val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es"))
    Surface(color = Color(0xFFF3F6FF), tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dia.format(formato).replaceFirstChar { it.titlecase(Locale("es")) },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
