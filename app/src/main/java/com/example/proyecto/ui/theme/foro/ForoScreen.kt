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
    token: String, // 💡 El token debe venir de MainActivity/LoginViewModel
    onBack: () -> Unit, // 👈 Parámetro para volver atrás
    onVerComentar: (PublicacionDto) -> Unit, // 👈 Parámetro para navegar al detalle
    viewModel: ForoViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    // LLAMADA INICIAL: Cargar publicaciones al entrar a la pantalla
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
                    IconButton(onClick = onBack) { // 👈 Botón de volver
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón para refrescar manualmente
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
                                // 💡 La Card ahora llama al callback de navegación
                                PublicacionCard(publicacion, onVerComentar)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        // Mostrar el indicador si está cargando pero hay contenido
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
    onVerComentar: (PublicacionDto) -> Unit // 👈 Callback para el click
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerComentar(publicacion) } // 👈 Al hacer click, navega al detalle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Autor: ${publicacion.autor}")
            Text(text = publicacion.contenido, style = MaterialTheme.typography.bodyLarge)

            TextButton(onClick = { onVerComentar(publicacion) }) {
                Text("Ver comentarios y detalles...")
            }
        }
    }
}