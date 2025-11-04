package com.example.proyecto.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem

import com.example.proyecto.api.ApiClient
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
    LaunchedEffect(token) { vm.cargar(token) }

    var fullImageUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> ErrorView(
                    mensaje = state.error ?: "Error",
                    onRetry = { vm.cargar(token) },
                    modifier = Modifier.align(Alignment.Center)
                )
                state.publicaciones.isEmpty() ->
                    Text("Sin publicaciones", Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.publicaciones, key = { it.id }) { p ->
                        PublicacionCard(
                            p = p,
                            token = token,
                            vm = vm,
                            onImageClick = { url -> fullImageUrl = url }
                        )
                    }
                }
            }
        }
    }

    fullImageUrl?.let { url ->
        FullscreenImage(url = url, onDismiss = { fullImageUrl = null })
    }
}

@Composable
private fun ErrorView(
    mensaje: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No se pudo cargar.\n$mensaje")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

/** Convierte URL relativa del backend (p. ej. /media/x.jpg) a absoluta usando ApiClient.baseUrl */
private fun absoluteUrl(url: String): String {
    return if (url.startsWith("http", ignoreCase = true)) url
    else ApiClient.baseUrl.trimEnd('/') + url
}

@Composable
private fun PublicacionCard(
    p: PublicacionDto,
    token: String,
    vm: ForoViewModel,
    onImageClick: (String) -> Unit
) {
    // Cargar comentarios al montar
    LaunchedEffect(p.id) { vm.cargarComentarios(token, p.id) }

    val comentarios = vm.comentarios[p.id] ?: emptyList()
    val posteando = vm.posting[p.id] == true
    val errorSend = vm.postError[p.id]

    var reply by remember { mutableStateOf("") }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text(p.autor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(p.contenido, style = MaterialTheme.typography.bodyMedium)

            // --- IMÁGENES ---
            val imagenes = p.adjuntos.filter { it.tipoArchivo.equals("imagen", ignoreCase = true) }
            if (imagenes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = absoluteUrl(imagenes.first().url),
                    contentDescription = imagenes.first().nombre ?: "Imagen adjunta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(absoluteUrl(imagenes.first().url)) },
                    contentScale = ContentScale.Crop
                )
                if (imagenes.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imagenes.drop(1)) { img ->
                            AsyncImage(
                                model = absoluteUrl(img.url),
                                contentDescription = img.nombre ?: "Imagen adjunta",
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onImageClick(absoluteUrl(img.url)) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // --- AUDIO ---
            val audios = p.adjuntos.filter { it.tipoArchivo.equals("audio", ignoreCase = true) }
            audios.forEach { a ->
                Spacer(Modifier.height(10.dp))
                AudioPlayer(url = absoluteUrl(a.url), titulo = a.nombre ?: "Audio")
            }

            // --- OTROS ADJUNTOS ---
            val otros = p.adjuntos.filter { it.tipoArchivo.lowercase() !in listOf("imagen", "audio") }
            if (otros.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Adjuntos:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                otros.forEach { a ->
                    Text(
                        "• ${a.nombre ?: a.url} (${a.tipoArchivo})",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // --- COMENTARIOS (con hilos estilo web) ---
            Text("Comentarios (${comentarios.size})", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            ComentariosThread(
                comentarios = comentarios,
                onReply = { parentId, text ->
                    vm.comentar(token = token, pubId = p.id, texto = text, parentId = parentId)
                }
            )

            if (errorSend != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    errorSend,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- Composer (nuevo comentario raíz) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    placeholder = { Text("Escribe un comentario…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val t = reply.trim()
                            if (t.isNotBlank() && !posteando) {
                                vm.comentar(token, p.id, t, parentId = null)
                                reply = ""
                            }
                        }
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val t = reply.trim()
                        if (t.isNotBlank() && !posteando) {
                            vm.comentar(token, p.id, t, parentId = null)
                            reply = ""
                        }
                    },
                    enabled = reply.isNotBlank() && !posteando
                ) {
                    if (posteando) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(p.fechaCreacion, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/* ---------- Lista de comentarios con hilos (estilo web) ---------- */

@Composable
private fun ComentariosThread(
    comentarios: List<ComentarioDto>,
    onReply: (parentId: Int, text: String) -> Unit
) {
    val porPadre = remember(comentarios) { comentarios.groupBy { it.parent } }
    val raiz = porPadre[null].orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        raiz.forEach { c ->
            ComentarioItem(
                c = c,
                hijos = porPadre[c.id].orEmpty(),
                onReply = onReply,
                nivel = 0
            )
        }
    }
}

@Composable
private fun ComentarioItem(
    c: ComentarioDto,
    hijos: List<ComentarioDto>,
    onReply: (parentId: Int, text: String) -> Unit,
    nivel: Int
) {
    var abrirRespuesta by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(start = if (nivel == 0) 0.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "@${c.autor}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "· ${c.fechaCreacion}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(text = c.contenido, style = MaterialTheme.typography.bodySmall)

        TextButton(
            onClick = { abrirRespuesta = !abrirRespuesta },
            contentPadding = PaddingValues(0.dp)
        ) { Text(if (abrirRespuesta) "Cancelar" else "Responder") }

        if (abrirRespuesta) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    placeholder = { Text("Escribe tu respuesta…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val t = texto.trim()
                        if (t.isNotBlank()) {
                            onReply(c.id, t)
                            texto = ""
                            abrirRespuesta = false
                        }
                    }
                ) { Text("Enviar") }
            }
        }

        if (hijos.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 4.dp, start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hijos.forEach { h ->
                    ComentarioItem(
                        c = h,
                        hijos = emptyList(),   // 2 niveles, igual que la web
                        onReply = onReply,
                        nivel = nivel + 1
                    )
                }
            }
        }
    }
}

/* ------------------ Audio player ------------------ */

@Composable
private fun AudioPlayer(url: String, titulo: String) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = {
                if (player.isPlaying) { player.pause(); playing = false }
                else { player.play(); playing = true }
            }) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pausar" else "Reproducir"
                )
            }
            Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/* ---------------- Imagen fullscreen ---------------- */

@Composable
private fun FullscreenImage(url: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Imagen",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    )
}
