package com.example.proyecto.ui.actas

// Imports añadidos
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
// ---
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.proyecto.data.ActaDto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActaDetalleScreen(
    acta: ActaDto,
    onBack: () -> Unit
) {
    // 1. Manejador para el botón "atrás" del sistema
    BackHandler { onBack() }

    // 2. Fondo de gradiente
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDCD8FF), // Un lila muy claro
            Color(0xFFC0B8FF)  // Un lila un poco más oscuro
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // 3. Barra de título personalizada
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    acta.reunion_titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333366) // Un color oscuro para el título
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4. Tarjeta principal con el contenido
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Ocupa el espacio disponible
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    // 5. Hacemos que el contenido sea deslizable
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = acta.reunion_fecha, style = MaterialTheme.typography.bodySmall)

                    // 6. Chip de estado con estilo "pill"
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(if (acta.aprobada) "Aprobada" else "No aprobada")
                        },
                        shape = RoundedCornerShape(50.dp),
                        leadingIcon = {
                            if (acta.aprobada) {
                                Icon(Icons.Filled.CheckCircle, "Aprobada")
                            } else {
                                Icon(Icons.Filled.Close, "No aprobada")
                            }
                        }
                    )

                    Divider()

                    Text(
                        text = acta.contenido.ifBlank { "Sin contenido." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 7. Botón "Volver" al final
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)) // Color de VotacionesScreen
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Volver")
            }
        }
    }
}


// ----------- PREVIEW PARA ANDROID STUDIO -----------
@Preview(showBackground = true)
@Composable
fun PreviewActaDetalleScreen() {
    val actaEjemplo = ActaDto(
        reunion = 1,
        contenido = "Este es el contenido completo del acta.\n\nAquí se detallan todos los puntos discutidos durante la reunión.\n\nPunto 1: Discusión sobre el presupuesto.\nPunto 2: Avances del proyecto.\nPunto 3: Varios.\n\nEl contenido puede ser bastante largo, por lo que la capacidad de scroll es importante para asegurar que el usuario pueda leer todo el texto sin problemas de diseño, incluso en pantallas más pequeñas. El texto sigue fluyendo hacia abajo.",
        aprobada = true,
        reunion_titulo = "Reunión de ejemplo",
        reunion_fecha = "2025-11-01",
        reunion_tipo = "Ordinaria",
        resumen = "Resumen de ejemplo"
    )

    MaterialTheme {
        ActaDetalleScreen(
            acta = actaEjemplo,
            onBack = {}
        )
    }
}