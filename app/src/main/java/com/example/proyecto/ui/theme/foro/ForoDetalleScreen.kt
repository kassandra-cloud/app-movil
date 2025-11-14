package com.example.proyecto.ui.theme.foro

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.viewmodel.ForoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoDetalleScreen(
    token: String, // 👈 Parámetro token
    publicacion: PublicacionDto, // 👈 Parámetro Publicacion
    onBack: () -> Unit, // 👈 Parámetro para volver atrás
    vm: ForoViewModel = viewModel()
) {
    // Si quisieras asegurarte de tener lo último del backend:
    LaunchedEffect(token) {
        if (vm.uiState.publicaciones.isEmpty()) {
            vm.cargar(token)
        }
    }

    val state = vm.uiState
    // Si el VM tiene una versión más nueva de la publicación, úsala
    val pubActual = state.publicaciones.find { it.id == publicacion.id } ?: publicacion

    val comentarios = pubActual.comentarios
    val posting = state.posting[pubActual.id] == true
    val errorSend = state.postError[pubActual.id]

    var nuevoComentario by remember(pubActual.id) { mutableStateOf("") }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Detalle de publicación") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ----- Cabecera -----
            Text(
                text = "@Vecino #${pubActual.autor}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = pubActual.fechaCreacion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = pubActual.contenido,
                style = MaterialTheme.typography.bodyMedium
            )

            // ----- Lista de comentarios -----
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Comentarios (${comentarios.size})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (comentarios.isEmpty()) {
                Text(
                    text = "Sé la primera persona en comentar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    comentarios.forEach { c ->
                        ComentarioItemDetalle(c)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            if (errorSend != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = errorSend,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            // ----- Nuevo comentario -----
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
                        // 💡 Llama al ViewModel con el token y el ID de la publicación
                        vm.comentar(token, pubActual.id, nuevoComentario)
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

/** Item de comentario SOLO para la pantalla de detalle */
@Composable
fun ComentarioItemDetalle(
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