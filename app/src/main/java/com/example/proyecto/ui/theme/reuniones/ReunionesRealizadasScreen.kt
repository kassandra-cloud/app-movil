package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.ui.theme.ProyectoTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// -------------------------------------------------------------------------
// COLORES UNIFICADOS
// -------------------------------------------------------------------------

private val PrimaryBlue = AppColors.Principal
private val TextPrimary = AppColors.TextPrimary
private val GrisOscuroTexto = AppColors.GrisOscuroTexto
private val HeaderBlue = PrimaryBlue.copy(alpha = 0.95f)

// -------------------------------------------------------------------------
// AUXILIARES
// -------------------------------------------------------------------------

private val CHILE_TZ_REALIZADAS: ZoneId = ZoneId.of("America/Santiago")

private fun parseChileRealizadas(iso: String): LocalDateTime = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(CHILE_TZ_REALIZADAS).toLocalDateTime()
}.getOrElse {
    LocalDateTime.parse(iso)
}

/** Encabezado del Día con Esquinas Redondeadas y Color Unificado */
@Composable
private fun DiaHeaderRealizadas(dia: LocalDate) {
    val formato = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es"))
    Surface(
        color = HeaderBlue,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
    ) {
        Text(
            text = dia.format(formato).replaceFirstChar { it.titlecase(Locale("es")) },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

/** Tarjeta de Reunión con Jerarquía y Fondo Blanco */
@Composable
private fun ReunionRealizadaCard(
    r: ReunionDto,
    onClick: () -> Unit
) {
    val inicio = parseChileRealizadas(r.fechaInicio)
    val horaFmt = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es"))

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
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
                color = GrisOscuroTexto
            )
            r.tabla?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = GrisOscuroTexto)
            }
        }
    }
}

// -------------------------------------------------------------------------
// FILTROS (Todas | Este mes | Este año)
// -------------------------------------------------------------------------

private enum class RealizadasFilter {
    ALL, THIS_MONTH, THIS_YEAR
}

@Composable
private fun RealizadasFilterRow(
    selected: RealizadasFilter,
    onSelect: (RealizadasFilter) -> Unit
) {
    val primary = PrimaryBlue
    val onPrimary = Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        @Composable
        fun BlueFilterChip(
            text: String,
            isSelected: Boolean,
            onClick: () -> Unit
        ) {
            FilterChip(
                selected = isSelected,
                onClick = onClick,
                label = { Text(text) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = primary,
                    selectedContainerColor = primary,
                    selectedLabelColor = onPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = primary.copy(alpha = 0.4f),
                    selectedBorderColor = primary
                )
            )
        }

        BlueFilterChip(
            text = "Todas",
            isSelected = selected == RealizadasFilter.ALL,
            onClick = { onSelect(RealizadasFilter.ALL) }
        )
        BlueFilterChip(
            text = "Este mes",
            isSelected = selected == RealizadasFilter.THIS_MONTH,
            onClick = { onSelect(RealizadasFilter.THIS_MONTH) }
        )
        BlueFilterChip(
            text = "Este año",
            isSelected = selected == RealizadasFilter.THIS_YEAR,
            onClick = { onSelect(RealizadasFilter.THIS_YEAR) }
        )
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
    val st by vm.realizadas.collectAsState(
        initial = ReunionesViewModel.SectionState()
    )
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (!st.initialized && !st.loading) {
            vm.refresh(ReunionEstado.REALIZADA)
        }
    }

    val today = remember { LocalDate.now(CHILE_TZ_REALIZADAS) }
    var activeFilter by remember { mutableStateOf(RealizadasFilter.ALL) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        "Reuniones realizadas",
                        color = TextPrimary // Texto Oscuro Unificado
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PrimaryBlue // Icono Azul Unificado
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.White,            // Fondo Blanco
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
                        onClick = { vm.refresh(ReunionEstado.REALIZADA) },
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
                Text("No hay reuniones realizadas.", color = GrisOscuroTexto)
            }

            else -> {
                val filteredItems = remember(st.items, activeFilter) {
                    st.items.filter { r ->
                        val date = parseChileRealizadas(r.fechaInicio).toLocalDate()
                        when (activeFilter) {
                            RealizadasFilter.ALL -> true
                            RealizadasFilter.THIS_MONTH ->
                                date.year == today.year && date.month == today.month
                            RealizadasFilter.THIS_YEAR ->
                                date.year == today.year
                        }
                    }
                }

                val grupos = remember(filteredItems) {
                    filteredItems
                        .sortedByDescending { parseChileRealizadas(it.fechaInicio) }
                        .groupBy { parseChileRealizadas(it.fechaInicio).toLocalDate() }
                        .toSortedMap(compareByDescending { it })
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    RealizadasFilterRow(
                        selected = activeFilter,
                        onSelect = { activeFilter = it }
                    )

                    Spacer(Modifier.height(4.dp))

                    if (grupos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay reuniones para este filtro.")
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
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

                            item {
                                if (st.hasNext || st.loading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (st.loading) {
                                            CircularProgressIndicator(color = PrimaryBlue)
                                        } else if (st.hasNext) {
                                            SideEffect {
                                                vm.nextPage(ReunionEstado.REALIZADA)
                                            }
                                        }
                                    }
                                }
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
fun PreviewReunionesRealizadasScreen() {
    ProyectoTheme {
        ReunionesRealizadasScreen(
            onBack = {},
            onOpen = {}
        )
    }
}