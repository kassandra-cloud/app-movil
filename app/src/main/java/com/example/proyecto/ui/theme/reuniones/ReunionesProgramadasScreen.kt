package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.ui.theme.ProyectoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// -------------------------------------------------------------------------
// AUXILIARES
// -------------------------------------------------------------------------

@Composable
private fun DiaHeaderProgramadas(dia: LocalDate) {
    val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es"))
    Surface(
        // ✅ Color adaptable: SurfaceVariant (gris suave en día, gris medio en noche)
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        Text(
            text = dia.format(formato).replaceFirstChar { it.titlecase(Locale("es")) },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            // ✅ Color de texto sobre variante
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val CHILE_TZ_PROGRAMADAS: ZoneId = ZoneId.of("America/Santiago")

private fun parseChileProgramadas(iso: String): LocalDateTime = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(CHILE_TZ_PROGRAMADAS).toLocalDateTime()
}.getOrElse {
    LocalDateTime.parse(iso)
}

@Composable
private fun ReunionProgramadaCard(
    r: ReunionDto,
    onClick: () -> Unit
) {
    val inicio = parseChileProgramadas(r.fechaInicio)
    val horaFmt = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es"))

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            // ✅ Tarjeta dinámica (Blanca / Gris Oscuro)
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                r.titulo,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                // ✅ Color principal del tema
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                inicio.format(horaFmt),
                style = MaterialTheme.typography.bodySmall,
                // ✅ Color secundario
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            r.tabla?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// CALENDARIO
// -------------------------------------------------------------------------

@Composable
private fun CalendarDay(
    day: LocalDate,
    isMarked: Boolean,
    isSelected: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayOfMonth = day.dayOfMonth.toString()

    // ✅ Colores del tema
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val textColor = if (isSelected) onPrimaryColor else MaterialTheme.colorScheme.onSurface

    val selectionBg = if (isSelected) primaryColor else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(40)) // Ripple circular
            .clickable { onDateSelected(day) }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Canvas(modifier = Modifier.size(32.dp)) {
                    drawCircle(
                        color = selectionBg,
                        radius = size.minDimension / 2,
                        style = Fill
                    )
                }
            }

            Text(
                text = dayOfMonth,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Puntito indicador de reunión
        if (isMarked) {
            Canvas(modifier = Modifier.size(4.dp)) {
                drawCircle(
                    color = if (isSelected) onPrimaryColor else primaryColor,
                    radius = 4f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }
    }
}

@Composable
private fun MonthlyCalendarViewProgramadas(
    currentMonth: YearMonth,
    uniqueDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "Mi", "J", "V", "S", "D")
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es")) }

    // ... (Cálculos de calendario idénticos) ...
    val firstDayOfMonth = currentMonth.atDay(1)
    val startDayOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    val days = remember(currentMonth) {
        val list = mutableListOf<LocalDate?>()
        repeat(startDayOffset) { list.add(null) }
        repeat(daysInMonth) { dayIndex -> list.add(currentMonth.atDay(dayIndex + 1)) }
        val totalCells = list.size
        val remaining = (7 - totalCells % 7) % 7
        repeat(remaining) { list.add(null) }
        list
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Navegación Mes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Anterior", tint = MaterialTheme.colorScheme.primary)
            }

            Text(
                text = currentMonth.format(monthFormatter).replaceFirstChar { it.titlecase(Locale("es")) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Siguiente", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Cabecera Días
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Grilla
        Column(modifier = Modifier.fillMaxWidth()) {
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day != null) {
                            CalendarDay(
                                day = day,
                                isMarked = day in uniqueDates,
                                isSelected = day == selectedDate,
                                onDateSelected = onDateSelected,
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
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

    LaunchedEffect(Unit) {
        if (!st.initialized && !st.loading) {
            vm.refresh(ReunionEstado.PROGRAMADA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // ✅ Fondo Dinámico
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. CABECERA AZUL CON GRADIENTE (Estilo Unificado)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp) // Un poco más alto para dar espacio al calendario flotante
                    .background(AppColors.GradientePrincipal)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Reuniones Programadas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Consulta la agenda por día",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Espacio vacío para que el contenido suba
        }

        // 2. CONTENIDO SUPERPUESTO (Calendario + Lista)
        // Usamos una columna que empieza desplazada hacia arriba
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp) // Ajustamos para que la tarjeta tape parte del azul
                .padding(horizontal = 20.dp)
        ) {

            // Lógica de fechas
            val reunionesPorDia = remember(st.items) {
                st.items.groupBy { parseChileProgramadas(it.fechaInicio).toLocalDate() }
            }
            val uniqueMeetingDates = remember(reunionesPorDia) { reunionesPorDia.keys.toSet() }
            val initialDate = uniqueMeetingDates.minOrNull() ?: LocalDate.now(CHILE_TZ_PROGRAMADAS)
            var selectedDate by remember(uniqueMeetingDates) { mutableStateOf(initialDate) }
            var currentMonth by remember(uniqueMeetingDates) { mutableStateOf(YearMonth.from(initialDate)) }
            val reunionesSeleccionadas = remember(selectedDate, reunionesPorDia) {
                reunionesPorDia[selectedDate]?.sortedBy { parseChileProgramadas(it.fechaInicio) }.orEmpty()
            }

            // --- TARJETA DE CALENDARIO FLOTANTE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    MonthlyCalendarViewProgramadas(
                        currentMonth = currentMonth,
                        uniqueDates = uniqueMeetingDates,
                        selectedDate = selectedDate,
                        onDateSelected = { newDate ->
                            selectedDate = newDate
                            val newMonth = YearMonth.from(newDate)
                            if (newMonth != currentMonth) currentMonth = newMonth
                        },
                        onMonthChange = { newMonth ->
                            currentMonth = newMonth
                            val dateInMonth = uniqueMeetingDates.filter { YearMonth.from(it) == newMonth }.minOrNull()
                            selectedDate = dateInMonth ?: newMonth.atDay(1)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- LISTA DE REUNIONES ---
            // Usamos Box con weight para que ocupe el resto de la pantalla
            Box(modifier = Modifier.weight(1f)) {
                when {
                    st.loading && st.items.isEmpty() -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )

                    st.error != null -> Text(
                        "Error: ${st.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )

                    reunionesSeleccionadas.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Sin reuniones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No hay nada programado para el\n${selectedDate.format(DateTimeFormatter.ofPattern("dd 'de' MMMM"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DiaHeaderProgramadas(selectedDate)
                        }
                        items(items = reunionesSeleccionadas) { r ->
                            ReunionProgramadaCard(r) { onOpen(r) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReunionesProgramadasScreen() {
    ProyectoTheme {
        ReunionesProgramadasScreen()
    }
}