package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // 🔑 IMPORTACIÓN CORREGIDA
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
// COLORES UNIFICADOS
// -------------------------------------------------------------------------

private val PrimaryBlue = AppColors.Principal
private val TextSecondary = AppColors.GrisOscuroTexto
private val TextPrimary = AppColors.TextPrimary
private val HeaderBgLight = AppColors.GrisClaroFondo

// -------------------------------------------------------------------------
// AUXILIARES
// -------------------------------------------------------------------------

/** 🔑 MEJORA: Header de día con fondo claro unificado y esquinas redondeadas. */
@Composable
private fun DiaHeaderProgramadas(dia: LocalDate) {
    val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es"))
    Surface(
        color = HeaderBgLight,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)) // Corregido: RoundedCornerShape está disponible
    ) {
        Text(
            text = dia.format(formato).replaceFirstChar { it.titlecase(Locale("es")) },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = TextPrimary
        )
    }
}

private val CHILE_TZ_PROGRAMADAS: ZoneId = ZoneId.of("America/Santiago")

/** Acepta ISO con o sin offset. */
private fun parseChileProgramadas(iso: String): LocalDateTime = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(CHILE_TZ_PROGRAMADAS).toLocalDateTime()
}.getOrElse {
    LocalDateTime.parse(iso)
}

/** 🔑 MEJORA: Tarjeta con mayor elevación y jerarquía. */
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
        colors = CardDefaults.elevatedCardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                r.titulo,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                inicio.format(horaFmt),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            r.tabla?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// CALENDARIO + SELECTOR DE MES
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
    val primaryColor = PrimaryBlue
    val onPrimaryColor = Color.White

    val selectionBg = if (isSelected) primaryColor else Color.Transparent
    val textColor = if (isSelected) onPrimaryColor else TextPrimary

    Column(
        modifier = modifier
            .clickable { onDateSelected(day) }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Canvas(modifier = Modifier.size(30.dp)) {
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

        if (isMarked) {
            Canvas(modifier = Modifier.size(4.dp)) {
                drawCircle(
                    color = primaryColor,
                    radius = 4f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }
    }
}

/** Calendario con dropdown de los 12 meses del año actual. */
@Composable
private fun MonthlyCalendarViewProgramadas(
    currentMonth: YearMonth,
    uniqueDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "Mi", "J", "V", "S", "D")
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))
    }

    val meetingMonths = remember(uniqueDates) {
        uniqueDates
            .map { YearMonth.from(it) }
            .toSet()
    }

    val monthsOfYear = remember(currentMonth.year) {
        (1..12).map { monthNumber ->
            YearMonth.of(currentMonth.year, monthNumber)
        }
    }

    var monthMenuExpanded by remember { mutableStateOf(false) }

    val firstDayOfMonth = currentMonth.atDay(1)
    val startDayOffset =
        (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
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

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Fila de navegación + selector de mes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Mes anterior",
                    tint = PrimaryBlue
                )
            }

            Box {
                Text(
                    text = currentMonth
                        .format(monthFormatter)
                        .replaceFirstChar { it.titlecase(Locale("es")) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable { monthMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false }
                ) {
                    monthsOfYear.forEach { ym ->
                        val hasMeetings = ym in meetingMonths
                        DropdownMenuItem(
                            text = {
                                Text(
                                    ym.format(monthFormatter)
                                        .replaceFirstChar { it.titlecase(Locale("es")) },
                                    color = if (hasMeetings) TextPrimary
                                    else TextSecondary
                                )
                            },
                            onClick = {
                                monthMenuExpanded = false
                                onMonthChange(ym)
                            }
                        )
                    }
                }
            }

            IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Mes siguiente",
                    tint = PrimaryBlue
                )
            }
        }

        // Días de la semana
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Grilla de días
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day != null) {
                            CalendarDay(
                                day = day,
                                isMarked = day in uniqueDates,
                                isSelected = day == selectedDate,
                                onDateSelected = onDateSelected,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }

        Divider(
            Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
        )
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
        if (!st.initialized && !st.loading) {
            vm.refresh(ReunionEstado.PROGRAMADA)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Reuniones programadas", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = PrimaryBlue
                )
            )
        }
    ) { padding ->
        when {
            st.loading && st.items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
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
                            containerColor = PrimaryBlue
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
                Text("No hay reuniones programadas.", color = TextSecondary)
            }

            else -> {
                // Agrupamos reuniones por día
                val reunionesPorDia = remember(st.items) {
                    st.items.groupBy {
                        parseChileProgramadas(it.fechaInicio).toLocalDate()
                    }
                }

                val uniqueMeetingDates = remember(reunionesPorDia) {
                    reunionesPorDia.keys.toSet()
                }

                val initialDate = uniqueMeetingDates.minOrNull()
                    ?: LocalDate.now(CHILE_TZ_PROGRAMADAS)

                var selectedDate by remember(uniqueMeetingDates) {
                    mutableStateOf(initialDate)
                }
                var currentMonth by remember(uniqueMeetingDates) {
                    mutableStateOf(YearMonth.from(initialDate))
                }

                // Reuniones SOLO del día seleccionado
                val reunionesSeleccionadas = remember(selectedDate, reunionesPorDia) {
                    reunionesPorDia[selectedDate]
                        ?.sortedBy { parseChileProgramadas(it.fechaInicio) }
                        .orEmpty()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // --- CALENDARIO + SELECTOR DE MES ---
                    MonthlyCalendarViewProgramadas(
                        currentMonth = currentMonth,
                        uniqueDates = uniqueMeetingDates,
                        selectedDate = selectedDate,
                        onDateSelected = { newDate ->
                            selectedDate = newDate
                            val newMonth = YearMonth.from(newDate)
                            if (newMonth != currentMonth) {
                                currentMonth = newMonth
                            }
                        },
                        onMonthChange = { newMonth ->
                            currentMonth = newMonth
                            val dateInMonth = uniqueMeetingDates
                                .filter { YearMonth.from(it) == newMonth }
                                .minOrNull()
                            selectedDate = dateInMonth ?: newMonth.atDay(1)
                        }
                    )

                    // --- CONTENIDO DEL DÍA SELECCIONADO ---
                    if (reunionesSeleccionadas.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            DiaHeaderProgramadas(selectedDate)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No hay reuniones programadas para esta fecha.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item("header-$selectedDate") {
                                DiaHeaderProgramadas(selectedDate)
                            }
                            items(
                                items = reunionesSeleccionadas,
                                key = { it.id ?: "${it.titulo}-${it.fechaInicio}" }
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

// ====================== PREVIEW ======================
@Preview(showBackground = true)
@Composable
fun PreviewReunionesProgramadasScreen() {
    ProyectoTheme {
        ReunionesProgramadasScreen(
            onBack = {},
            onOpen = {}
        )
    }
}