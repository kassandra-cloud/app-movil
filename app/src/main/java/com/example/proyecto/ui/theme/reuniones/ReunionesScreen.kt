package com.example.proyecto.ui.theme.reuniones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History // Icono para Realizadas
import androidx.compose.material.icons.filled.CalendarMonth // Icono para Programadas
import androidx.compose.material.icons.filled.PlayCircle // Icono para En Curso
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview // 🔑 Importación necesaria
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.ui.theme.ProyectoTheme // 🔑 Importación necesaria para el Preview

/* ===== Paleta (Unificada usando AppColors) ===== */
private val iconBgSoft = AppColors.Principal.copy(alpha = 0.12f)
private val iconBorder = AppColors.Principal.copy(alpha = 0.35f)

/* ===== Pantalla ===== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReunionesScreen(
    realizadasCount: Int? = null,
    programadasCount: Int? = null,
    enCursoCount: Int? = null,
    onVerRealizadas: () -> Unit = {},
    onVerProgramadas: () -> Unit = {},
    onVerEnCurso: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(AppColors.GradientePrincipal)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Reuniones", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Historial, agenda y sesiones en curso",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Espacio grande para empujar la lista hacia abajo (efecto MainMenu)
            Spacer(Modifier.height(56.dp))

            // LISTA DE MÓDULOS
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MenuCard(
                    title = "Reuniones realizadas",
                    subtitle = realizadasCount?.let { "$it registradas" } ?: "Ver historial",
                    icon = Icons.Filled.History,
                    onClick = onVerRealizadas
                )
                MenuCard(
                    title = "Reuniones programadas",
                    subtitle = programadasCount?.let { "$it próximas" } ?: "Ver calendario",
                    icon = Icons.Filled.CalendarMonth,
                    onClick = onVerProgramadas
                )
                MenuCard(
                    title = "Reuniones en curso",
                    subtitle = enCursoCount?.let { "$it activas" } ?: "Ver detalles",
                    icon = Icons.Filled.PlayCircle,
                    onClick = onVerEnCurso
                )
            }
        }
    }
}

/* ===== Tarjeta tipo módulo con ícono azul (SIN CAMBIOS FUNCIONALES) ===== */
@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cápsula clara + borde con azul
            Surface(
                color = iconBgSoft,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = if (pressed) 0.dp else 2.dp,
                border = BorderStroke(1.dp, iconBorder)
            ) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = AppColors.Principal)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.GrisOscuroTexto.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.Principal
            )
        }
    }
}

// ====================== PREVIEW ======================
@Preview(showBackground = true)
@Composable
fun PreviewReunionesScreen() {
    ProyectoTheme {
        ReunionesScreen(
            realizadasCount = 5,
            programadasCount = 2,
            enCursoCount = null,
            onBack = {}
        )
    }
}