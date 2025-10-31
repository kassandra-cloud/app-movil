package com.example.proyecto.ui.actas


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyecto.data.ActaDto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActaDetalleScreen(
    acta: ActaDto,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(acta.reunion_titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = acta.reunion_fecha, style = MaterialTheme.typography.bodySmall)

            // estado (aprobada / no aprobada)
            AssistChip(
                onClick = {},
                label = {
                    Text(if (acta.aprobada) "Aprobada" else "No aprobada")
                }
            )

            Divider()

            Text(
                text = acta.contenido.ifBlank { "Sin contenido." },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}