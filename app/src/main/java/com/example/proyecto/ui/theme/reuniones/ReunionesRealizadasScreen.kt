package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.data.reuniones.AsistenciaDto
import com.example.proyecto.ui.theme.AppColors // 👈 Importamos tus colores
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado

// ---------------- ENUMS DE FILTRO ----------------

private enum class FiltroActa { TODAS, APROBADAS, NO_APROBADAS }
private enum class FiltroAsistencia { TODAS, PRESENTE, AUSENTE, SIN_REGISTRO }

// --- COLORES SEMÁNTICOS (Fijos por significado) ---
val ColorPositivo = Color(0xFF16A34A) // Verde
val ColorNegativo = Color(0xFFDC2626) // Rojo
val ColorNeutral = Color(0xFF9CA3AF)  // Gris

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReunionesRealizadasScreen(
    onBack: () -> Unit,
    onOpen: (ReunionDto) -> Unit,
    reunionesVM: ReunionesViewModel = viewModel()
) {
    val state by reunionesVM.realizadas.collectAsState(
        initial = ReunionesViewModel.SectionState()
    )
    val misAsistencias by reunionesVM.miAsistenciaPorReunion.collectAsState()

    var filtroActa by remember { mutableStateOf(FiltroActa.TODAS) }
    var filtroAsistencia by remember { mutableStateOf(FiltroAsistencia.TODAS) }

    LaunchedEffect(Unit) {
        reunionesVM.ensureLoaded(ReunionEstado.REALIZADA)
        reunionesVM.cargarMisAsistencias()
    }

    val opcionesActa = mapOf(
        FiltroActa.TODAS        to "Todas",
        FiltroActa.APROBADAS    to "Solo aprobadas",
        FiltroActa.NO_APROBADAS to "Solo no aprobadas"
    )

    val opcionesAsistencia = mapOf(
        FiltroAsistencia.TODAS        to "Todas",
        FiltroAsistencia.PRESENTE     to "Solo presente",
        FiltroAsistencia.AUSENTE      to "Solo ausente",
        FiltroAsistencia.SIN_REGISTRO to "Solo sin registro"
    )

    val reunionesFiltradas = remember(state.items, misAsistencias, filtroActa, filtroAsistencia) {
        state.items.filter { reunion ->
            val actaAprobada = reunion.actaAprobada == true
            val miAsistencia = misAsistencias[reunion.id]

            val pasaFiltroActa = when (filtroActa) {
                FiltroActa.TODAS        -> true
                FiltroActa.APROBADAS    -> actaAprobada
                FiltroActa.NO_APROBADAS -> !actaAprobada
            }

            val pasaFiltroAsistencia = when (filtroAsistencia) {
                FiltroAsistencia.TODAS        -> true
                FiltroAsistencia.PRESENTE     -> miAsistencia?.presente == true
                FiltroAsistencia.AUSENTE      -> miAsistencia?.presente == false
                FiltroAsistencia.SIN_REGISTRO -> miAsistencia == null
            }

            pasaFiltroActa && pasaFiltroAsistencia
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // ✅ Fondo Dinámico
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---------- CABECERA AZUL CON GRADIENTE ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    // ✅ Usamos el gradiente de la marca
                    .background(AppColors.GradientePrincipal)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Reuniones realizadas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Historial, actas y tu asistencia",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // ---------- CONTENIDO + FILTROS ----------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-50).dp) // Subimos el contenido para que flote sobre el azul
                    .padding(horizontal = 20.dp)
            ) {

                // --- TARJETA DE FILTROS ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    // ✅ Color de Tarjeta Dinámico
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        // Filtro 1: ACTA
                        Box(modifier = Modifier.weight(1f)) {
                            FiltroDropdownMinimalista(
                                label = "Acta",
                                selectedValue = filtroActa,
                                options = opcionesActa,
                                onValueChange = { filtroActa = it },
                                icon = Icons.Default.Description
                            )
                        }

                        // Separador Vertical
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 10.dp)
                                // ✅ Color de separador adaptable
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Filtro 2: ASISTENCIA
                        Box(modifier = Modifier.weight(1f)) {
                            FiltroDropdownMinimalista(
                                label = "Asistencia",
                                selectedValue = filtroAsistencia,
                                options = opcionesAsistencia,
                                onValueChange = { filtroAsistencia = it },
                                icon = Icons.Default.Person
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // LÓGICA DE LISTA / ESTADOS
                when {
                    state.loading && state.items.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    state.error != null && state.items.isEmpty() -> {
                        EstadoError(state.error ?: "")
                    }

                    reunionesFiltradas.isEmpty() -> {
                        EstadoVacio(
                            filtroActivo = (filtroActa != FiltroActa.TODAS || filtroAsistencia != FiltroAsistencia.TODAS),
                            onLimpiar = {
                                filtroActa = FiltroActa.TODAS
                                filtroAsistencia = FiltroAsistencia.TODAS
                            }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(reunionesFiltradas) { reunion ->
                                val miAsistencia = misAsistencias[reunion.id]
                                ReunionRealizadaItem(
                                    reunion = reunion,
                                    miAsistencia = miAsistencia,
                                    // 🚀 Esta es la acción que lleva a ActaDetalleScreen
                                    onClick = { onOpen(reunion) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- COMPONENTES UI AUXILIARES ----------------

@Composable
fun <T> FiltroDropdownMinimalista(
    label: String,
    selectedValue: T,
    options: Map<T, String>,
    onValueChange: (T) -> Unit,
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val text = options[selectedValue] ?: ""

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // ✅ Tinte dinámico
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Color secundario
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, // ✅ Color principal
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // ✅ Fondo del menú adaptable
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { (key, value) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = value,
                            fontWeight = if(key == selectedValue) FontWeight.Bold else FontWeight.Normal,
                            color = if(key == selectedValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onValueChange(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EstadoVacio(filtroActivo: Boolean, onLimpiar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(70.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No se encontraron reuniones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Intenta cambiar los criterios de búsqueda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filtroActivo) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onLimpiar,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Limpiar filtros")
            }
        }
    }
}

@Composable
private fun EstadoError(error: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ocurrió un error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReunionRealizadaItem(
    reunion: ReunionDto,
    miAsistencia: AsistenciaDto?,
    onClick: () -> Unit // 👈 Esta función es la que llama a onOpen(reunion) en el padre
) {
    val actaAprobada = reunion.actaAprobada == true
    val tieneActa = reunion.actaId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 🚀 Toda la tarjeta es clicable
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            // ✅ Tarjeta adaptable (Blanco en día, Gris en noche)
            // Eliminamos el azul claro fijo (0xFFE7F2FF)
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título + fecha
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = reunion.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        // ✅ Color primario del tema
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reunion.fechaInicio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Resumen de tabla
            if (!reunion.tabla.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reunion.tabla.take(100) +
                            if (reunion.tabla.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Estado del acta
            if (tieneActa) {
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chipText: String
                    val chipBg: Color
                    // Mantenemos texto blanco en los chips de estado para contraste
                    val chipFg: Color = Color.White

                    if (actaAprobada) {
                        chipText = "Acta aprobada"
                        chipBg = ColorPositivo
                    } else {
                        chipText = "No aprobada"
                        chipBg = ColorNeutral
                    }

                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.bodySmall,
                            color = chipFg,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (actaAprobada) {
                        // 🚀 El botón "Ver acta" también llama a la función onClick (onOpen)
                        TextButton(onClick = onClick) {
                            Text(
                                text = "Ver acta",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ---- Tu asistencia ----
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tu asistencia: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val (label, bg, fg) = when {
                    miAsistencia == null -> Triple("Sin registro", ColorNeutral, Color.White)
                    miAsistencia?.presente == true -> Triple("Presente", ColorPositivo, Color.White)
                    miAsistencia?.presente == false -> Triple("Ausente", ColorNegativo, Color.White)
                    else -> Triple("Sin registro", ColorNeutral, Color.White)
                }

                Box(
                    modifier = Modifier
                        .background(bg, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = fg,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}