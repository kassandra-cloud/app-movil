@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.proyecto.ui.theme.foro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.viewmodel.ForoViewModel

@Composable
fun ForoScreen(
    token: String,
    onBack: () -> Unit,
    onVerComentar: (PublicacionDto) -> Unit,
    viewModel: ForoViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    // LLAMADA INICIAL
    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            viewModel.cargar(token)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foro de Vecinos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cargar(token) }, enabled = !uiState.cargando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    uiState.cargando && uiState.publicaciones.isEmpty() -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                            items(uiState.publicaciones) { publicacion ->
                                PublicacionCard(publicacion, onVerComentar)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        if (uiState.cargando) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun PublicacionCard(
    publicacion: PublicacionDto,
    onVerComentar: (PublicacionDto) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerComentar(publicacion) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 🛡️ CORRECCIÓN: Usamos '?:' para manejar nulos
            Text(
                text = "Autor: ${publicacion.autor ?: "Anónimo"}",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 🛡️ CORRECCIÓN: Si contenido es null, mostramos string vacío
            Text(
                text = publicacion.contenido ?: "Sin contenido",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { onVerComentar(publicacion) }) {
                Text("Ver comentarios y detalles...")
            }
        }
    }
}