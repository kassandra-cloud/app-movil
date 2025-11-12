package com.example.proyecto.ui.actas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.proyecto.data.reuniones.ActaDto
// Importamos las constantes de color desde MainActivity.kt (asumiendo que están disponibles en el mismo módulo o se importarán las específicas)
import com.example.proyecto.webColorPrincipal
import com.example.proyecto.tuColorTextoPrimario

// Definiciones de color simuladas basadas en el nuevo esquema azul (las que deberías mover a un archivo de tema)
val ColorPrincipal = webColorPrincipal // #42A5F5 (Azul)
val ColorSecundario = Color(0xFF64B5F6) // Un azul más claro para el chip Aprobada
val ColorCardBg = Color(0xFFF5F5F5) // Gris muy claro para el fondo de la tarjeta
val ColorTextPrimary = tuColorTextoPrimario // #212121 (Negro/Gris muy oscuro)
val ColorGrisOscuroTexto = Color(0xFF757575) // Gris oscuro para fechas/subtítulos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActaDetalleScreen(
    acta: ActaDto,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Fondo BLANCO
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // 1. Barra de título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    // Icono oscuro
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = ColorTextPrimary // Usando el color de texto primario
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    acta.reunion_titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    // Título oscuro
                    color = ColorTextPrimary // Usando el color de texto primario
                )
            }

            Spacer(Modifier.height(16.dp))

            // 2. Tarjeta principal de Contenido
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCardBg), // Gris claro para fondo de tarjeta
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = acta.reunion_fecha,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorGrisOscuroTexto // Gris oscuro para texto secundario
                    )

                    // Chip de estado (Aprobada/No Aprobada)
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(if (acta.aprobada) "Aprobada" else "No aprobada")
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = if (acta.aprobada) {
                            // CHIP APROBADA: Usa el azul secundario para un toque vibrante
                            AssistChipDefaults.assistChipColors(
                                containerColor = ColorSecundario, // Azul más claro
                                labelColor = Color.White,
                                leadingIconContentColor = Color.White
                            )
                        } else {
                            // CHIP NO APROBADA: Usa tonos de gris consistentes
                            AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFEEEEEE), // Gris muy claro
                                labelColor = ColorGrisOscuroTexto,
                                leadingIconContentColor = ColorGrisOscuroTexto
                            )
                        },
                        leadingIcon = {
                            if (acta.aprobada) {
                                Icon(Icons.Filled.CheckCircle, "Aprobada")
                            } else {
                                Icon(Icons.Filled.Close, "No aprobada")
                            }
                        }
                    )

                    Divider()

                    // Contenido del Acta
                    Text(
                        text = acta.contenido.ifBlank { "Sin contenido." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextPrimary // Color de texto principal
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 3. Botón "Volver"
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrincipal) // Usa el azul principal
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
        contenido = "Este es el contenido completo del acta. Aquí se detallan todos los puntos discutidos y acuerdos alcanzados.\n\nEl contenido es largo y demostramos que el scroll funciona correctamente.",
        aprobada = true,
        reunion_titulo = "Reunión de aprobación de cuentas",
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