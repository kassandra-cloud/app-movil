package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun ReunionesEnCursoScreen(
    onBack: () -> Unit,
    onOpen: (ReunionDto) -> Unit,
    reunionesVM: ReunionesViewModel = viewModel()
) {
    val state by reunionesVM.enCurso.collectAsState(
        initial = ReunionesViewModel.SectionState()
    )

    // Carga / recarga al entrar
    LaunchedEffect(Unit) {
        reunionesVM.refresh(ReunionEstado.EN_CURSO)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // -------------- CABECERA --------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(AppColors.GradientePrincipal)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
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
                            text = "Transcripción automática en tiempo real",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // -------------- LISTA --------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 20.dp)
            ) {
                if (state.loading && state.items.isEmpty()) {
                    // Cargando inicial
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.items.isEmpty()) {
                    // Sin reuniones en curso
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay reuniones en curso.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.items) { reunion ->
                            ReunionEnCursoCard(
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

@Composable
private fun ReunionEnCursoCard(
    reunion: ReunionDto,
    onClick: () -> Unit
) {
    val estadoTranscripcion = formatEstadoTranscripcion(reunion.actaEstadoTranscripcion)
    val snippet = reunion.actaContenido
        ?.take(120)          // máximo 120 caracteres
        ?.ifBlank { null }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F2FF)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = AppColors.IconoReuniones,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "En curso",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reunion.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = estadoTranscripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2563EB)   // azul para destacar estado
                    )
                }
            }

            if (snippet != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = snippet + "…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

private fun formatEstadoTranscripcion(code: String?): String =
    when (code) {
        "NO_SUBIDO"   -> "Sin audio adjunto"
        "PENDIENTE"   -> "Audio listo para transcribir"
        "PROCESANDO"  -> "Transcripción en proceso…"
        "COMPLETADO"  -> "Transcripción completada"
        "ERROR"       -> "Error en la transcripción"
        null          -> "Estado de transcripción desconocido"
        else          -> code // por si llega un valor nuevo
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ReunionesEnCursoPreview() {
    ProyectoTheme {
        ReunionesEnCursoScreen(
            onBack = {},
            onOpen = {}
        )
    }
}
