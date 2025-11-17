package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.ui.theme.ProyectoTheme

@Composable
fun ReunionesScreen(
    realizadasCount: Int,
    programadasCount: Int,
    enCursoCount: Int,
    onVerRealizadas: () -> Unit,
    onVerProgramadas: () -> Unit,
    onVerEnCurso: () -> Unit,
    onBack: () -> Unit
) {
    val hasEnCurso = enCursoCount > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ----------------- CABECERA AZUL -----------------
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
                                text = "Reuniones",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Historial, agenda y sesiones en curso",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // ----------------- TARJETAS -----------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-40).dp) // flota sobre el header
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReunionCategoryCard(
                    title = "Reuniones realizadas",
                    subtitle = "$realizadasCount registradas",
                    iconTint = Color.White,
                    iconBg = AppColors.IconoReuniones,
                    enabled = true,
                    onClick = onVerRealizadas,
                    icon = Icons.Default.History
                )

                ReunionCategoryCard(
                    title = "Reuniones programadas",
                    subtitle = "$programadasCount próximas",
                    iconTint = Color.White,
                    iconBg = AppColors.IconoReuniones,
                    enabled = true,
                    onClick = onVerProgramadas,
                    icon = Icons.Default.Today
                )

                // 🔹 Se desactiva cuando no hay reuniones en curso
                ReunionCategoryCard(
                    title = "Reuniones en curso",
                    subtitle = if (hasEnCurso)
                        "$enCursoCount en curso"
                    else
                        "Sin reuniones en curso",
                    iconTint = Color.White,
                    iconBg = AppColors.IconoReuniones,
                    enabled = hasEnCurso,
                    onClick = onVerEnCurso,
                    icon = Icons.Default.PlayArrow
                )
            }
        }
    }
}

@Composable
fun ReunionCategoryCard(
    title: String,
    subtitle: String,
    iconTint: Color,
    iconBg: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val alpha = if (enabled) 1f else 0.4f
    val elevation = if (enabled) 6.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },   // más opaco si está deshabilitada
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F2FF)
        ),
        elevation = CardDefaults.cardElevation(elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    onClick()
                }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = iconBg),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
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
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ReunionesScreenPreview() {
    ProyectoTheme {
        ReunionesScreen(
            realizadasCount = 3,
            programadasCount = 1,
            enCursoCount = 0, // aquí se verá opaca y sin click
            onVerRealizadas = {},
            onVerProgramadas = {},
            onVerEnCurso = {},
            onBack = {}
        )
    }
}
