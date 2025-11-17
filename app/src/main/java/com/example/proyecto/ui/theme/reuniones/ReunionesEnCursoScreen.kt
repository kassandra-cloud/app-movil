package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ReunionDto
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.ui.theme.ProyectoTheme
import com.example.proyecto.viewmodel.ReunionesViewModel
import com.example.proyecto.viewmodel.ReunionesViewModel.ReunionEstado
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/* =========== LISTA: REUNIONES EN CURSO =========== */

@Composable
fun ReunionesEnCursoScreen(
    onBack: () -> Unit,
    onOpen: (ReunionDto) -> Unit,
    reunionesVM: ReunionesViewModel = viewModel()
) {
    val state by reunionesVM.enCurso.collectAsState(
        initial = ReunionesViewModel.SectionState()
    )

    // Cargar datos al entrar
    LaunchedEffect(Unit) {
        reunionesVM.ensureLoaded(ReunionEstado.EN_CURSO)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // CABECERA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(AppColors.GradientePrincipal)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Reuniones en curso",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sesiones activas y estado de transcripción",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // CONTENIDO
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
                                text = "No hay reuniones en curso.",
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
                                ReunionEnCursoItem(
                                    reunion = reunion,
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
fun ReunionEnCursoItem(
    reunion: ReunionDto,
    onClick: () -> Unit
) {
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

            Spacer(Modifier.height(8.dp))

            if (!reunion.tabla.isNullOrBlank()) {
                Text(
                    text = reunion.tabla.take(100) +
                            if (reunion.tabla.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
                Spacer(Modifier.height(8.dp))
            }

            TranscripcionChip(
                estado = reunion.actaEstadoTranscripcion,
                hasContenido = !reunion.actaContenido.isNullOrBlank()
            )
        }
    }
}

/* =========== DETALLE: REUNIÓN EN CURSO =========== */

@Composable
fun ReunionEnCursoDetalleScreen(
    reunion: ReunionDto,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    // 🔁 Refresco automático cada 5 segundos mientras estés en esta pantalla
    LaunchedEffect(reunion.id) {
        while (isActive) {
            onRefresh()
            delay(1_000)
        }
    }

    // 🔹 Limpieza básica de los "\n" para que se vean como saltos de línea
    val textoTranscripcion = (reunion.actaContenido ?: "")
        .replace("\\n", "\n")
        .replace("\\r", "")
        .replace("\\t", "    ")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // CABECERA (la dejamos igual, pero puedes simplificar si quieres)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(AppColors.GradientePrincipal)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Transcripción en curso",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = reunion.titulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // 🌟 SOLO TRANSCRIPCIÓN 🌟
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Transcripción (borrador)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))

                        if (textoTranscripcion.isBlank()) {
                            Text(
                                text = "Aún no hay texto de transcripción disponible para esta reunión.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        } else {
                            Text(
                                text = textoTranscripcion,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF111827)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
/* =========== CHIP DE ESTADO =========== */

@Composable
fun TranscripcionChip(
    estado: String?,
    hasContenido: Boolean
) {
    val (texto, bg, fg) = when (estado) {
        "NO_SUBIDO" -> Triple("Sin audio subido", Color(0xFFE5E7EB), Color(0xFF374151))
        "PENDIENTE" -> Triple("Pendiente de procesar", Color(0xFFF97316), Color.White)
        "PROCESANDO" -> Triple("Procesando transcripción...", Color(0xFF0EA5E9), Color.White)
        "COMPLETADO" -> {
            if (hasContenido) {
                Triple("Transcripción disponible", Color(0xFF16A34A), Color.White)
            } else {
                Triple("Completado (sin contenido)", Color(0xFF16A34A), Color.White)
            }
        }
        "ERROR" -> Triple("Error en transcripción", Color(0xFFDC2626), Color.White)
        else -> Triple("Estado no disponible", Color(0xFFE5E7EB), Color(0xFF374151))
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ReunionesEnCursoPreview() {
    ProyectoTheme {
        // Solo para ver la lista vacía en preview
        ReunionesEnCursoScreen(
            onBack = {},
            onOpen = {}
        )
    }
}
