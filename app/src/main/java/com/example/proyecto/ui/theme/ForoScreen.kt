package com.example.proyecto.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.viewmodel.ForoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoScreen(
    token: String,
    onBack: () -> Unit,
    // 👉 aquí te dejo el callback para abrir la pantalla de detalle/comentarios
    onVerComentar: (PublicacionDto) -> Unit = {},
    vm: ForoViewModel = viewModel()
) {
    val state = vm.uiState

    // Carga inicial de publicaciones
    LaunchedEffect(token) {
        vm.cargar(token)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Foro de la Junta de Vecinos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No se pudo cargar el foro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.cargar(token) }) {
                            Text("Reintentar")
                        }
                    }
                }

                else -> {
                    if (state.publicaciones.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aún no hay publicaciones en el foro.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.publicaciones, key = { it.id }) { pub ->
                                PublicacionCard(
                                    p = pub,
                                    onImageClick = { /* aquí luego puedes abrir visor de imagen */ },
                                    onVerComentar = onVerComentar
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicacionCard(
    p: PublicacionDto,
    onImageClick: (String) -> Unit,
    onVerComentar: (PublicacionDto) -> Unit
) {
    val comentarios = p.comentarios
    val comentariosCount = comentarios.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera: autor + fecha (similar al web)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@Vecino #${p.autor}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${p.fechaCreacion})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // Contenido de la publicación
            Text(
                text = p.contenido,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            // Título "Archivos Adjuntos:" si hay imagen, igual que en web
            val imagenAdj = p.adjuntos.firstOrNull {
                it.tipoArchivo.equals("imagen", ignoreCase = true)
            }
            if (imagenAdj != null) {
                Text(
                    text = "Archivos Adjuntos:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 260.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onImageClick(imagenAdj.url) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imagenAdj.url,
                        contentDescription = "Imagen publicación",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            Divider(thickness = 0.6.dp)
            Spacer(Modifier.height(8.dp))

            // Botón "Ver / Comentar (N)" – solo lógica de navegación
            Button(
                onClick = { onVerComentar(p) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Ver / Comentar (${comentariosCount})",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
