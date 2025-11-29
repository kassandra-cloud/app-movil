package com.example.proyecto.ui.theme.foro

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.proyecto.data.AdjuntoDto
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.ui.components.AudioPlayer
import com.example.proyecto.ui.theme.AppColors // 👈 IMPORTANTE: Tus colores
import com.example.proyecto.utils.getMimeType
import com.example.proyecto.utils.startDownload
import com.example.proyecto.utils.uriToFile
import com.example.proyecto.viewmodel.ForoViewModel
import java.io.File
import java.io.IOException

// -----------------------------------------------------------
// CLASES SELLADAS
// -----------------------------------------------------------

sealed class ChatItem {
    abstract val fecha: String
    data class Comentario(val dto: ComentarioDto) : ChatItem() {
        override val fecha = dto.fechaCreacion ?: ""
    }
    data class Adjunto(val dto: AdjuntoDto) : ChatItem() {
        override val fecha = dto.fechaCreacion ?: ""
    }
}

sealed class DeleteTarget {
    data class Comentario(val id: Int) : DeleteTarget()
    data class Adjunto(val id: Int) : DeleteTarget()
}

// -----------------------------------------------------------
// PANTALLA PRINCIPAL
// -----------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoDetalleScreen(
    token: String,
    usuarioActual: String,
    publicacion: PublicacionDto,
    onBack: () -> Unit,
    vm: ForoViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(token) {
        if (vm.uiState.publicaciones.isEmpty()) vm.cargar(token)
    }

    val state = vm.uiState
    val pubActual = state.publicaciones.find { it.id == publicacion.id } ?: publicacion

    // -----------------------------------------------------------
    // VARIABLES UI
    // -----------------------------------------------------------

    val posting = state.posting[pubActual.id] == true
    val errorSend = state.postError[pubActual.id]
    var nuevoComentario by remember(pubActual.id) { mutableStateOf("") }
    var itemAEliminar by remember { mutableStateOf<DeleteTarget?>(null) }
    var comentarioAResponder by remember { mutableStateOf<String?>(null) }
    var parentId by remember { mutableStateOf<Int?>(null) }
    var imagenPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var audioPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var docPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }

    // -----------------------------------------------------------
    // VARIABLES AUDIO Y ARCHIVOS
    // -----------------------------------------------------------
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        docPreviewUri = uri
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imagenPreviewUri = uri
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val file = File(context.cacheDir, "nota_voz.m4a")
        audioFile = file
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                recorder = this
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecordingAndSend() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
        isRecording = false
        audioFile?.let { file ->
            vm.enviarArchivo(token, pubActual.id, file, "audio/m4a", "")
            Toast.makeText(context, "Audio enviado", Toast.LENGTH_SHORT).show()
        }
    }

    val adjuntosPrincipales = remember(pubActual) { (pubActual.adjuntos ?: emptyList()).filter { !it.esMensaje } }
    val chatList = remember(pubActual) {
        val comentarios = (pubActual.comentarios ?: emptyList()).map { ChatItem.Comentario(it) }
        val adjuntos = (pubActual.adjuntos ?: emptyList()).filter { it.esMensaje }.map { ChatItem.Adjunto(it) }
        (comentarios + adjuntos).sortedBy { it.fecha }
    }

    // -----------------------------------------------------------
    // DIALOGOS DE CONFIRMACIÓN DE ENVÍO
    // -----------------------------------------------------------
    // (Mantenemos la lógica de diálogos igual, solo asegurando colores)
    if (imagenPreviewUri != null || audioPreviewUri != null || docPreviewUri != null) {
        Dialog(onDismissRequest = {
            imagenPreviewUri = null; audioPreviewUri = null; docPreviewUri = null; caption = ""
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Confirmar Envío", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))

                    if (imagenPreviewUri != null) {
                        AsyncImage(
                            model = imagenPreviewUri, contentDescription = null,
                            modifier = Modifier.heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else if (docPreviewUri != null) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Documento seleccionado", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = caption, onValueChange = { caption = it },
                        placeholder = { Text("Comentario opcional...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { imagenPreviewUri = null; docPreviewUri = null; caption = "" }) { Text("Cancelar") }
                        Button(onClick = {
                            val uri = imagenPreviewUri ?: docPreviewUri
                            uri?.let { u ->
                                val file = uriToFile(context, u)
                                if (file != null) vm.enviarArchivo(token, pubActual.id, file, getMimeType(file), caption)
                            }
                            imagenPreviewUri = null; docPreviewUri = null; caption = ""
                        }) { Text("Enviar") }
                    }
                }
            }
        }
    }

    // Dialogo Borrar
    if (itemAEliminar != null) {
        AlertDialog(
            onDismissRequest = { itemAEliminar = null },
            title = { Text("Eliminar") },
            text = { Text("¿Estás seguro?") },
            confirmButton = {
                TextButton(onClick = {
                    when (val t = itemAEliminar) {
                        is DeleteTarget.Comentario -> vm.eliminarComentario(token, t.id, pubActual.id)
                        is DeleteTarget.Adjunto -> vm.eliminarAdjunto(token, t.id, pubActual.id)
                        null -> {}
                    }
                    itemAEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { itemAEliminar = null }) { Text("Cancelar") } }
        )
    }

    // -----------------------------------------------------------
    // UI PRINCIPAL SCAFFOLD
    // -----------------------------------------------------------
    Scaffold(
        topBar = {
            // ✅ BARRA CON GRADIENTE DE MARCA
            TopAppBar(
                title = {
                    Column {
                        Text("Foro", style = MaterialTheme.typography.titleMedium)
                        Text("#${pubActual.autor ?: "Anónimo"}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.background(AppColors.GradientePrincipal)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // ✅ Fondo Dinámico
                .background(MaterialTheme.colorScheme.background)
        ) {

            // -----------------------------------------------------------
            // LISTA DE MENSAJES (CHAT)
            // -----------------------------------------------------------
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CABECERA DE LA PUBLICACIÓN ORIGINAL
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(pubActual.contenido ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(pubActual.fechaCreacion ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (adjuntosPrincipales.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(adjuntosPrincipales) { adj -> ImagenOficial(adj) }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }

                // MENSAJES
                items(chatList) { item ->
                    val esMio = when(item) {
                        is ChatItem.Comentario -> item.dto.autor == usuarioActual
                        is ChatItem.Adjunto -> item.dto.autor == usuarioActual
                    }
                    val autor = if(item is ChatItem.Comentario) item.dto.autor else (item as ChatItem.Adjunto).dto.autor
                    val fecha = item.fecha

                    // Renderizar Comentario o Adjunto
                    if (item is ChatItem.Comentario) {
                        val isReply = item.dto.parent != null
                        // Sangría visual para respuestas
                        val indentModifier = if (isReply) Modifier.padding(start = 20.dp) else Modifier

                        ChatBubble(
                            autor = autor ?: "Anónimo",
                            fecha = fecha,
                            esMio = esMio,
                            modifier = indentModifier,
                            contenido = {
                                val parent = pubActual.comentarios?.find { it.id == item.dto.parent }
                                if (parent != null) {
                                    Text("↳ @${parent.autor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(item.dto.contenido ?: "", style = MaterialTheme.typography.bodyMedium)
                            },
                            footer = {
                                ChatFooterActions(
                                    esMio = esMio,
                                    liked = item.dto.meGustaUsuario,
                                    likes = item.dto.totalLikes,
                                    onReply = { comentarioAResponder = item.dto.autor; parentId = item.dto.id },
                                    onLike = { vm.toggleLike(token, item.dto.id, pubActual.id) },
                                    onDelete = { itemAEliminar = DeleteTarget.Comentario(item.dto.id) }
                                )
                            }
                        )
                    } else if (item is ChatItem.Adjunto) {
                        ChatBubble(
                            autor = autor ?: "Anónimo",
                            fecha = fecha,
                            esMio = esMio,
                            contenido = {
                                ChatAttachmentContent(item.dto, context)
                            },
                            footer = {
                                ChatFooterActions(
                                    esMio = esMio,
                                    liked = item.dto.meGustaUsuario,
                                    likes = item.dto.totalLikes,
                                    onReply = { comentarioAResponder = item.dto.autor; parentId = null },
                                    onLike = { vm.toggleLikeAdjunto(token, item.dto.id, pubActual.id) },
                                    onDelete = { itemAEliminar = DeleteTarget.Adjunto(item.dto.id) }
                                )
                            }
                        )
                    }
                }
            }

            // -----------------------------------------------------------
            // BARRA DE INPUT
            // -----------------------------------------------------------
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface) // ✅ Fondo barra input
                    .padding(8.dp)
            ) {
                // Barra de respuesta activa
                if (comentarioAResponder != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Respondiendo a @$comentarioAResponder", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.Close, null, modifier = Modifier.clickable { comentarioAResponder = null; parentId = null }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (errorSend != null) Text(errorSend, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botones de adjuntos
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { docLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Audio
                    IconButton(onClick = { if (isRecording) stopRecordingAndSend() else startRecording() }) {
                        Icon(Icons.Default.Mic, "Grabar", tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                    }

                    // Campo de texto
                    OutlinedTextField(
                        value = nuevoComentario,
                        onValueChange = { nuevoComentario = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        placeholder = { Text(if (isRecording) "Grabando..." else "Comentar...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Enviar
                    IconButton(
                        onClick = {
                            vm.comentar(token, pubActual.id, nuevoComentario, parentId)
                            nuevoComentario = ""; comentarioAResponder = null; parentId = null
                        },
                        enabled = !posting && nuevoComentario.isNotBlank()
                    ) {
                        if (posting) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// COMPONENTES DE AYUDA
// -----------------------------------------------------------

@Composable
fun ImagenOficial(adjunto: AdjuntoDto) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(adjunto.url).crossfade(true).build(),
        contentDescription = null,
        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ChatBubble(
    autor: String,
    fecha: String,
    esMio: Boolean,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
    footer: (@Composable () -> Unit)?
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // Empujar a la derecha si es mío
        if (esMio) Spacer(Modifier.weight(1f))
        else {
            // Avatar para otros
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(autor.take(1).uppercase(), color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        // Contenido Burbuja
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (esMio) Alignment.End else Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
            ) {
                Text(autor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(8.dp))
                Text(fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }

            Spacer(Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(
                    topStart = 12.dp, topEnd = 12.dp,
                    bottomStart = if (esMio) 12.dp else 2.dp,
                    bottomEnd = if (esMio) 2.dp else 12.dp
                ),
                // ✅ Colores dinámicos para burbujas
                color = if (esMio) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = if (esMio) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // El contenido hereda el color de texto correcto automáticamente
                    CompositionLocalProvider(LocalContentColor provides if(esMio) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
                        contenido()
                    }
                    if (footer != null) {
                        Spacer(Modifier.height(8.dp))
                        footer()
                    }
                }
            }
        }

        // Empujar a la izquierda si no es mío
        if (!esMio) Spacer(Modifier.weight(1f))
    }
}

@Composable
fun ChatFooterActions(esMio: Boolean, liked: Boolean, likes: Int, onReply: ()->Unit, onLike: ()->Unit, onDelete: ()->Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val tintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

        IconButton(onClick = onReply, modifier = Modifier.size(20.dp)) {
            Icon(Icons.AutoMirrored.Filled.Reply, null, tint = tintColor)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onLike, modifier = Modifier.size(20.dp)) {
            Icon(
                if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                null,
                tint = if (liked) MaterialTheme.colorScheme.primary else tintColor
            )
        }
        if (likes > 0) Text(" $likes", fontSize = 12.sp, color = tintColor)

        if (esMio) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ChatAttachmentContent(dto: AdjuntoDto, context: android.content.Context) {
    Column {
        when (dto.tipoArchivo) {
            "imagen" -> AsyncImage(
                model = dto.url, contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            "audio" -> if (!dto.url.isNullOrBlank()) AudioPlayer(audioUrl = dto.url) else Text("Audio no disponible", color = MaterialTheme.colorScheme.error)
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(dto.archivo ?: "Documento", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = {
                        if (!dto.url.isNullOrBlank()) {
                            startDownload(context, dto.url, dto.archivo ?: "doc")
                            Toast.makeText(context, "Descargando...", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (!dto.descripcion.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(dto.descripcion, style = MaterialTheme.typography.bodyMedium)
        }
    }
}