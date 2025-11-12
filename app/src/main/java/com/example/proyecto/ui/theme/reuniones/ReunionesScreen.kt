package com.example.proyecto.ui.theme.reuniones
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Colores y gradiente (tomados de la imagen) */
private val azulClaroHeader = Color(0xFF42A5F5) // Similar al inicio del gradiente en la imagen
private val azulOscuroHeader = Color(0xFF1E88E5) // Similar al final del gradiente en la imagen
private val gradienteHeader = Brush.linearGradient(listOf(azulClaroHeader, azulOscuroHeader))

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
    Box( // Usamos Box para superponer el fondo y la lista de tarjetas
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FF)) // Fondo gris claro
    ) {
        // 🔹 1. Encabezado superior con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Altura suficiente para el TopBar y el corte
                .background(gradienteHeader)
                // Usamos un clip para simular el borde curvado de la tarjeta principal
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        )

        // 🔹 2. TopBar (sobre el gradiente)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Flecha de retroceso
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        // Usamos Icons.AutoMirrored.Filled.ArrowBack para compatibilidad RTL
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }

                // Título
                Text(
                    text = "Reuniones",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // 🔹 3. Cuerpo principal (tarjetas flotantes)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp), // Separación del header
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReunionCard(
                    text = "Reuniones Realizadas",
                    subtext = realizadasCount?.let { "$it registradas" } ?: "Ver historial",
                    onClick = onVerRealizadas
                )
                ReunionCard(
                    text = "Reuniones Programadas",
                    subtext = programadasCount?.let { "$it próximas" } ?: "Ver calendario",
                    onClick = onVerProgramadas
                )
                ReunionCard(
                    text = "Reuniones en Curso",
                    subtext = enCursoCount?.let { "$it activas" } ?: "Ver detalles",
                    onClick = onVerEnCurso
                )
            }
        }
    }
}

/** Componente de tarjeta de reunión con estilo similar al de la imagen principal. */
@Composable
private fun ReunionCard(
    text: String,
    subtext: String,
    onClick: () -> Unit
) {
    // Usamos ElevatedCard para replicar el efecto de sombra y el fondo blanco
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp), // Bordes redondeados
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White // Fondo blanco
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Sombra suave
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Contenido de la tarjeta
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = text,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtext,
                    color = Color.Gray,
                    fontSize = 14.sp,
                )
            }

            // Icono de flecha
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Usamos la misma flecha rotada 180°
                contentDescription = "Ir a detalles",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp).background(Color.White)
            )
        }
    }
}

// --- PREVISUALIZACIÓN ---
@Preview(showSystemUi = true)
@Composable
private fun ReunionesScreenPreview() {
    ReunionesScreen(
        realizadasCount = 5,
        programadasCount = 2,
        enCursoCount = 1,
        onBack = {} // Añadimos la función onBack para que se muestre la flecha
    )
}