@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.proyecto.ui.theme.foro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.ui.theme.AppColors // Importado para usar el color principal
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
        // 🔴 ENCABEZADO CON DISEÑO DE MARCA (como en otros módulos)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Principal, // Usando color principal
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.background(AppColors.Principal)
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Indicador de refresco en la parte superior
                if (uiState.cargando && uiState.publicaciones.isNotEmpty()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }

                when {
                    // Muestra spinner si está cargando y no hay datos
                    uiState.cargando && uiState.publicaciones.isEmpty() -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    // Muestra error
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    // Muestra contenido o mensaje de lista vacía
                    else -> {
                        if (uiState.publicaciones.isEmpty()) {
                            // 🔴 MENSAJE DE LISTA VACÍA MEJORADO
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Forum,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "El foro está tranquilo.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Sé el primero en iniciar una conversación.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                items(uiState.publicaciones) { publicacion ->
                                    PublicacionCard(publicacion, onVerComentar)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
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
    // Cuenta adjuntos de publicación (excluyendo los que son mensajes de chat)
    val numAdjuntos = (publicacion.adjuntos ?: emptyList()).count { !it.esMensaje }
    val numComentarios = (publicacion.comentarios ?: emptyList()).size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerComentar(publicacion) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp) // Tarjeta con esquinas redondeadas
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. CABECERA: Autor + Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Autor",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = publicacion.autor ?: "Anónimo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    // Muestra solo la fecha (asumiendo formato largo)
                    text = publicacion.fechaCreacion?.takeIf { it.length > 10 }?.substring(0, 10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. CONTENIDO PRINCIPAL (Snippet)
            Text(
                text = publicacion.contenido ?: "Sin contenido",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4, // Límite para vista previa
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 3. PIE DE PÁGINA: Indicadores de actividad
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de Adjuntos
                if (numAdjuntos > 0) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$numAdjuntos Archivos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                }

                // Indicador de Comentarios (funciona como botón)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onVerComentar(publicacion) }
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (numComentarios == 0) "Comentar" else "$numComentarios Comentarios",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}