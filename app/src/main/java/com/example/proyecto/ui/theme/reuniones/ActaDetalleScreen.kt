package com.example.proyecto.ui.actas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.reuniones.ActaDto
import com.example.proyecto.ui.theme.AppColors
import com.example.proyecto.viewmodel.ActasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActaDetalleScreen(
    acta: ActaDto,
    onBack: () -> Unit,
    viewModel: ActasViewModel = viewModel()
) {
    BackHandler { onBack() }

    // ✅ Anti-doble: evita que se registre 2 veces por recomposición
    val actaId = acta.reunion
    var registrado by rememberSaveable(actaId) { mutableStateOf(false) }

    LaunchedEffect(actaId) {
        if (!registrado) {
            viewModel.registrarConsulta(actaId)
            registrado = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Acta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.background(AppColors.GradientePrincipal)
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Text(
                    text = acta.reunionTitulo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = acta.reunionFecha,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            StatusChip(aprobada = acta.aprobada)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = acta.contenido.ifBlank { "Sin contenido." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Volver al listado", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// COMPONENTE VISUAL: CHIP DE ESTADO
@Composable
fun StatusChip(aprobada: Boolean) {
    val containerColor = if (aprobada) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (aprobada) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = if (aprobada) Icons.Filled.CheckCircle else Icons.Filled.Close
    val text = if (aprobada) "Aprobada" else "No aprobada"

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        border = if (!aprobada) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewActaDetalleScreen() {
    val actaEjemplo = ActaDto(
        reunion = 1,
        contenido = "Este es el contenido del acta de prueba.\nAquí se detallan los acuerdos.",
        aprobada = true,
        reunionTitulo = "Reunión General de Vecinos",
        reunionFecha = "2025-11-28",
        reunionTipo = "Ordinaria",
        autorUsername = "admin",
        resumen = "Resumen corto"
    )

    MaterialTheme {
        ActaDetalleScreen(
            acta = actaEjemplo,
            onBack = {}
        )
    }
}
