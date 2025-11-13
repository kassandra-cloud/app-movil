package com.example.proyecto.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
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
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.viewmodel.ForoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoScreen(
    token: String,
    onBack: () -> Unit,
    vm: ForoViewModel = viewModel()
) {
    val state = vm.uiState

    // Carga inicial de publicaciones + comentarios incluidos
    LaunchedEffect(token) {
        vm.cargar(token)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Foro comunitario") },
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.publicaciones, key = { it.id }) { pub ->
                                PublicacionCard(
                                    p = pub,
                                    token = token,
                                    vm = vm,
                                    onImageClick = { /* aquí podrías abrir un visor de imagen si quieres */ }
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
    token: String,
    vm: ForoViewModel,
    onImageClick: (String) -> Unit
) {
    // Los comentarios vienen directamente en la publicación
    val comentarios = p.comentarios

    // Estados de envío y error se leen desde el uiState del ViewModel
    val posting = vm.uiState.posting[p.id] == true
    val errorSend = vm.uiState.postError[p.id]

    var nuevoComentario by remember(p.id) { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // AUTOR (por ahora mostramos el ID, luego se puede cambiar a username)
            Text(
                text = "Vecino #${p.autor}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            // FECHA
            Text(
                text = p.fechaCreacion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // CONTENIDO
            Text(
                text = p.contenido,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            // IMAGEN (primer adjunto marcado como "imagen")
            val imagenAdj = p.adjuntos.firstOrNull {
                it.tipoArchivo.equals("imagen", ignoreCase = true)
            }
            if (imagenAdj != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
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
                Spacer(Modifier.height(8.dp))
            }

            Divider(thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))

            // TÍTULO COMENTARIOS
            Text(
                text = "Comentarios (${comentarios.size})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            if (comentarios.isEmpty()) {
                Text(
                    text = "Sé la primera persona en comentar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 👇 OJO: ya NO hay verticalScroll aquí, solo una Column normal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    comentarios.forEach { c ->
                        ComentarioItem(c)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (errorSend != null) {
                Text(
                    text = errorSend,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
            }

            // NUEVO COMENTARIO
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nuevoComentario,
                    onValueChange = { nuevoComentario = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un comentario...") },
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        vm.comentar(token, p.id, nuevoComentario)
                        nuevoComentario = ""
                    },
                    enabled = !posting && nuevoComentario.isNotBlank()
                ) {
                    if (posting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar comentario"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComentarioItem(
    c: ComentarioDto
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "@${c.autor}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = c.fechaCreacion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = c.contenido,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}