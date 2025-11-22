package com.example.proyecto.ui.theme.foro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    token: String,
    usuarioActual: String,
    publicacion: PublicacionDto,
    onBack: () -> Unit,
    vm: ForoViewModel = viewModel()
) {
    // Cargar datos al entrar
    LaunchedEffect(token) {
        if (vm.uiState.publicaciones.isEmpty()) {
            vm.cargar(token)
        }
    }

    val state = vm.uiState
    val pubActual = state.publicaciones.find { it.id == publicacion.id } ?: publicacion
    val comentarios = pubActual.comentarios
    val posting = state.posting[pubActual.id] == true
    val errorSend = state.postError[pubActual.id]

    var nuevoComentario by remember(pubActual.id) { mutableStateOf("") }

    // Estados para acciones
    var comentarioAEliminar by remember { mutableStateOf<Int?>(null) }
    var comentarioAResponder by remember { mutableStateOf<ComentarioDto?>(null) }

    // --- DIÁLOGO DE CONFIRMACIÓN (ELIMINAR) ---
    if (comentarioAEliminar != null) {
        AlertDialog(
            onDismissRequest = { comentarioAEliminar = null },
            title = { Text("¿Eliminar comentario?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarComentario(token, comentarioAEliminar!!, pubActual.id)
                    comentarioAEliminar = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { comentarioAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Detalle de publicación") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            // --- CABECERA PUBLICACIÓN ---
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

            HorizontalDivider()

            // --- LISTA DE COMENTARIOS ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(comentarios) { c ->
                    ComentarioItemDetalle(
                        c = c,
                        usuarioActual = usuarioActual,
                        listaComentarios = comentarios,
                        onEliminar = { id -> comentarioAEliminar = id },
                        onLike = { id -> vm.toggleLike(token, id, pubActual.id) },
                        onResponder = { com -> comentarioAResponder = com } // 👈 Activa el modo respuesta
                    )
                }
            }

            // --- BARRA "RESPONDIENDO A..." ---
            if (comentarioAResponder != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Respondiendo a @${comentarioAResponder?.autor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = comentarioAResponder?.contenido ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { comentarioAResponder = null }) {
                        Icon(Icons.Default.Close, "Cancelar", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            if (errorSend != null) {
                Text(text = errorSend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            // --- INPUT TEXTO ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nuevoComentario,
                    onValueChange = { nuevoComentario = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        if (comentarioAResponder != null) Text("Escribe tu respuesta...")
                        else Text("Escribe un comentario...")
                    },
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        // 🚀 ENVIAR CON O SIN PARENT ID
                        vm.comentar(
                            token = token,
                            publicacionId = pubActual.id,
                            texto = nuevoComentario,
                            parentId = comentarioAResponder?.id
                        )
                        nuevoComentario = ""
                        comentarioAResponder = null // Limpiar respuesta
                    },
                    enabled = !posting && nuevoComentario.isNotBlank()
                ) {
                    if (posting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}

@Composable
fun ComentarioItemDetalle(
    c: ComentarioDto,
    usuarioActual: String,
    listaComentarios: List<ComentarioDto>,
    onEliminar: (Int) -> Unit,
    onLike: (Int) -> Unit,
    onResponder: (ComentarioDto) -> Unit
) {
    // Lógica Escalón
    val esRespuesta = c.parent != null
    val paddingStart = if (esRespuesta) 32.dp else 0.dp

    val nombrePadre = if (esRespuesta) {
        listaComentarios.find { it.id == c.parent }?.autor ?: "usuario"
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart),
        colors = CardDefaults.cardColors(
            containerColor = if (esRespuesta)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (esRespuesta) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            // "↳ Respondiendo a..."
            if (nombrePadre != null) {
                Text(
                    text = "↳ Respondiendo a @$nombrePadre",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Autor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "@${c.autor}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Acciones
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // ↩️ RESPONDER
                    IconButton(onClick = { onResponder(c) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Responder",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 👍 LIKE
                    IconButton(onClick = { onLike(c.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (c.meGustaUsuario) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Me gusta",
                            tint = if (c.meGustaUsuario) Color(0xFF1976D2) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (c.totalLikes > 0) {
                        Text(
                            text = "${c.totalLikes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    // 🗑️ ELIMINAR
                    if (c.autor == usuarioActual) {
                        IconButton(onClick = { onEliminar(c.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Text(text = c.contenido, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = c.fechaCreacion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}