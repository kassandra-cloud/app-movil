package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.data.reuniones.AsistenciaDto
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado

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

    // Carga inicial de reuniones + MIS asistencias
    LaunchedEffect(Unit) {
        reunionesVM.ensureLoaded(ReunionEstado.REALIZADA)
        reunionesVM.cargarMisAsistencias()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---------- CABECERA ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
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

            // ---------- CONTENIDO ----------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 20.dp)
            ) {
                when {
                    state.loading && state.items.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.error != null && state.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Ocurrió un error al cargar las reuniones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB91C1C)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.error ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    state.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No hay reuniones realizadas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(state.items) { reunion ->
                                val miAsistencia = misAsistencias[reunion.id]
                                ReunionRealizadaItem(
                                    reunion = reunion,
                                    miAsistencia = miAsistencia,
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

@Composable
private fun ReunionRealizadaItem(
    reunion: ReunionDto,
    miAsistencia: AsistenciaDto?,
    onClick: () -> Unit
) {
    val actaAprobada = reunion.actaAprobada == true
    val tieneActa = reunion.actaId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F2FF)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
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
                        color = Color(0xFF111827)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reunion.fechaInicio,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF)
                )
            }

            // Resumen de tabla
            if (!reunion.tabla.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reunion.tabla.take(100) +
                            if (reunion.tabla.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
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
                    val chipFg: Color

                    if (actaAprobada) {
                        chipText = "Acta aprobada"
                        chipBg = Color(0xFF16A34A)   // verde
                        chipFg = Color.White
                    } else {
                        chipText = "No aprobada"
                        chipBg = Color(0xFF9CA3AF)   // gris
                        chipFg = Color.White
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
                        TextButton(onClick = onClick) {
                            Text(
                                text = "Ver acta",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
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
                    color = Color(0xFF6B7280)
                )

                val (label, bg, fg) = when {
                    miAsistencia == null -> Triple(
                        "Sin registro",
                        Color(0xFF9CA3AF),
                        Color.White
                    )
                    miAsistencia.presente == true -> Triple(
                        "Presente",
                        Color(0xFF16A34A),
                        Color.White
                    )
                    else -> Triple(
                        "Ausente",
                        Color(0xFFDC2626),
                        Color.White
                    )
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
