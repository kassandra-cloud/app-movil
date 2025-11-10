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
// Añade esta línea:
import androidx.compose.foundation.BorderStroke
// --------------------
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem

import com.example.proyecto.api.ApiClient
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.viewmodel.ForoViewModel

// ... (El resto del código de tu archivo ForoScreen.kt)
// 🎨 PALETA DE COLORES
val ColorPrincipal = Color(0xFF42A5F5) // Azul Vibrante
val ColorSecundario = Color(0xFF1E88E5) // Azul Oscuro (para el degradado)
val ColorGrisFondo = Color(0xFFF6F6F6) // Gris claro (fondo de componentes/anidados)
val ColorGrisBorde = Color(0xFFE0E0E0) // Gris para separadores

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
                title = { Text("Foro", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorPrincipal,
                    scrolledContainerColor = ColorPrincipal
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.cargando -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = ColorPrincipal
                )
                state.error != null -> ErrorView(
                    mensaje = state.error ?: "Error",
                    onRetry = { vm.cargar(token) },
                    modifier = Modifier.align(Alignment.Center)
                )
                state.publicaciones.isEmpty() ->
                    Text("Sin publicaciones", Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp) // Más espacio entre publicaciones
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
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrincipal)
        ) { Text("Reintentar") }
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
    LaunchedEffect(p.id) { vm.cargarComentarios(token, p.id) }

    val comentarios = vm.comentarios[p.id] ?: emptyList()
    val posting = vm.posting[p.id] == true
    val errorSend = vm.postError[p.id]

    var reply by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) // Aseguramos fondo blanco
    ) {
        Column(Modifier.padding(20.dp)) {

            // --- CABECERA DE LA PUBLICACIÓN (Autor y Contenido) ---
            Text(
                p.autor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ColorPrincipal
            )
            Spacer(Modifier.height(6.dp))
            Text(p.fechaCreacion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // Fecha más cerca del autor
            Spacer(Modifier.height(10.dp))
            Text(p.contenido, style = MaterialTheme.typography.bodyLarge)

            // --- ADJUNTOS ---
            val imagenes = p.adjuntos.filter { it.tipoArchivo.equals("imagen", ignoreCase = true) }
            val audios = p.adjuntos.filter { it.tipoArchivo.equals("audio", ignoreCase = true) }
            val otros = p.adjuntos.filter { it.tipoArchivo.lowercase() !in listOf("imagen", "audio") }

            if (imagenes.isNotEmpty() || audios.isNotEmpty() || otros.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = ColorGrisBorde)
                Spacer(Modifier.height(16.dp))
            }


            // IMÁGENES
            if (imagenes.isNotEmpty()) {
                AsyncImage(
                    model = absoluteUrl(imagenes.first().url),
                    contentDescription = imagenes.first().nombre ?: "Imagen adjunta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
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
                if (audios.isNotEmpty() || otros.isNotEmpty()) Spacer(Modifier.height(12.dp))
            }

            // AUDIO
            audios.forEach { a ->
                AudioPlayer(url = absoluteUrl(a.url), titulo = a.nombre ?: "Audio")
                if (a != audios.last()) Spacer(Modifier.height(8.dp))
            }

            // OTROS ADJUNTOS
            if (otros.isNotEmpty()) {
                if (audios.isNotEmpty()) Spacer(Modifier.height(12.dp))
                Text("Adjuntos:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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

            // --- COMENTARIOS (Visualización y Composer) ---
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ColorGrisBorde)
            Spacer(Modifier.height(16.dp))


            // TÍTULO COMENTARIOS
            Text("Comentarios (${comentarios.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // HILOS DE COMENTARIOS
            ComentariosThread(
                comentarios = comentarios,
                onReply = { parentId, text ->
                    vm.comentar(token = token, pubId = p.id, texto = text, parentId = parentId)
                }
            )

            if (errorSend != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorSend,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            // COMPOSER
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    placeholder = { Text("Escribe un comentario…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    shape = RoundedCornerShape(16.dp), // Más redondez
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrincipal,
                        focusedLabelColor = ColorPrincipal
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val t = reply.trim()
                            if (t.isNotBlank() && !posting) {
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
                        if (t.isNotBlank() && !posting) {
                            vm.comentar(token, p.id, t, parentId = null)
                            reply = ""
                        }
                    },
                    enabled = reply.isNotBlank() && !posting,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        containerColor = ColorPrincipal
                    ),
                    modifier = Modifier
                        .size(56.dp) // Hacemos el botón consistente con el alto del TextField
                        .clip(RoundedCornerShape(16.dp)) // Redondez consistente
                ) {
                    if (posting) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp), color = Color.White)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // Más espacio entre comentarios raíz
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

    val paddingStart = if (nivel == 0) 0.dp else 16.dp

    // Usamos un Surface para dar un fondo y redondez, mejorando la distinción de hilos
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (nivel > 0) ColorGrisFondo else Color.White, // Fondo ligeramente gris para raíz (si es nivel 0) o más para anidado
        border = if (nivel == 0) null else BorderStroke(1.dp, ColorGrisBorde), // Borde suave para anidados (opcional)
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${c.autor}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "· ${c.fechaCreacion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(text = c.contenido, style = MaterialTheme.typography.bodyMedium)

            TextButton(
                onClick = { abrirRespuesta = !abrirRespuesta },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp) // Padding más pequeño
            ) { Text(if (abrirRespuesta) "Cancelar" else "Responder", color = ColorPrincipal, style = MaterialTheme.typography.labelMedium) }

            if (abrirRespuesta) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        placeholder = { Text("Respuesta…", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrincipal,
                            focusedLabelColor = ColorPrincipal
                        )
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
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrincipal),
                        modifier = Modifier.height(48.dp)
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", modifier = Modifier.size(16.dp)) }
                }
            }

            if (hijos.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 10.dp), // Más espacio antes de los hijos
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    hijos.forEach { h ->
                        ComentarioItem(
                            c = h,
                            hijos = emptyList(),
                            onReply = onReply,
                            nivel = nivel + 1
                        )
                    }
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
        colors = CardDefaults.cardColors(containerColor = ColorGrisFondo),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = {
                if (player.isPlaying) { player.pause(); playing = false }
                else { player.play(); playing = true }
            },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White,
                    containerColor = ColorPrincipal
                ),
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pausar" else "Reproducir"
                )
            }
            Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
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