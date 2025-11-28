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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.proyecto.utils.getMimeType
import com.example.proyecto.utils.uriToFile
import com.example.proyecto.viewmodel.ForoViewModel
import java.io.File
import java.io.IOException
import android.util.Log

// NUEVOS IMPORTS para AudioPlayer y Descargas
import com.example.proyecto.ui.components.AudioPlayer
import com.example.proyecto.utils.startDownload
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
    // VARIABLES PARA AUDIO
    // -----------------------------------------------------------
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    // -----------------------------------------------------------
    // DOCUMENTOS - LAUNCHER FUNCIONAL
    // -----------------------------------------------------------
    val docLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { docPreviewUri = it }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { audioPreviewUri = it }
    }

    // -----------------------------------------------------------
    // PERMISO DE MIC
    // -----------------------------------------------------------
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    // -----------------------------------------------------------
    // FUNCIONES AUDIO
    // -----------------------------------------------------------
    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val file = File(context.cacheDir, "nota_voz.m4a")
        audioFile = file

        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else MediaRecorder()

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
            vm.enviarArchivo(
                token = token,
                publicacionId = pubActual.id,
                file = file,
                mimeType = "audio/m4a",
                textoCaption = ""
            )
            Toast.makeText(context, "Audio enviado", Toast.LENGTH_SHORT).show()
        }
    }


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imagenPreviewUri = uri
    }

    val adjuntosPrincipales = remember(pubActual) {
        (pubActual.adjuntos ?: emptyList()).filter { !it.esMensaje }
    }

    val chatList = remember(pubActual) {
        val comentarios = (pubActual.comentarios ?: emptyList()).map { ChatItem.Comentario(it) }
        val adjuntos = (pubActual.adjuntos ?: emptyList())
            .filter { it.esMensaje }
            .map { ChatItem.Adjunto(it) }

        (comentarios + adjuntos).sortedBy { it.fecha }
    }

    // -----------------------------------------------------------
    // DIALOGO IMAGEN
    // -----------------------------------------------------------
    if (imagenPreviewUri != null) {
        var caption by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { imagenPreviewUri = null }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Enviar Imagen", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    AsyncImage(
                        model = imagenPreviewUri,
                        contentDescription = null,
                        modifier = Modifier.heightIn(max = 250.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Comentario opcional...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { imagenPreviewUri = null }) {
                            Text("Cancelar")
                        }
                        Button(onClick = {
                            imagenPreviewUri?.let { uri ->
                                val file = uriToFile(context, uri)
                                if (file != null) {
                                    vm.enviarArchivo(
                                        token,
                                        pubActual.id,
                                        file,
                                        getMimeType(file),
                                        caption
                                    )
                                }
                            }
                            imagenPreviewUri = null
                        }) {
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }
    if (audioPreviewUri != null) {
        Dialog(onDismissRequest = { audioPreviewUri = null; caption = "" }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Enviar Audio", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    // Player simple
                    Text("Archivo de audio seleccionado") // puedes mejorar con MediaPlayer

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Comentario opcional...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { audioPreviewUri = null; caption = "" }) {
                            Text("Cancelar")
                        }
                        Button(onClick = {
                            audioPreviewUri?.let { uri ->
                                val file = uriToFile(context, uri)
                                if (file != null) {
                                    vm.enviarArchivo(token, pubActual.id, file, "audio/m4a", caption)
                                }
                            }
                            audioPreviewUri = null
                            caption = ""
                        }) { Text("Enviar") }
                    }
                }
            }
        }
    }
    if (docPreviewUri != null) {
        Dialog(onDismissRequest = { docPreviewUri = null; caption = "" }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Enviar Documento", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    // Intentamos mostrar el nombre real
                    val fileName = remember(docPreviewUri) {
                        docPreviewUri?.let { uri ->
                            var name = "Desconocido"
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val index = cursor.getColumnIndex("_display_name")
                                if (cursor.moveToFirst() && index != -1) {
                                    name = cursor.getString(index)
                                }
                            }
                            name
                        } ?: "Desconocido"
                    }

                    Text("Archivo: $fileName")

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Comentario opcional...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { docPreviewUri = null; caption = "" }) {
                            Text("Cancelar")
                        }
                        Button(onClick = {
                            docPreviewUri?.let { uri ->
                                try {
                                    // Crear archivo temporal en cache
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val tempFile = File(context.cacheDir, fileName)
                                    inputStream?.use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    // Enviar archivo
                                    vm.enviarArchivo(
                                        token = token,
                                        publicacionId = pubActual.id,
                                        file = tempFile,
                                        mimeType = getMimeType(tempFile),
                                        textoCaption = caption
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al enviar documento", Toast.LENGTH_SHORT).show()
                                    e.printStackTrace()
                                }
                            }
                            docPreviewUri = null
                            caption = ""
                        }) {
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }


    // -----------------------------------------------------------
    // DIALOGO BORRAR
    // -----------------------------------------------------------
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
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { itemAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    // -----------------------------------------------------------
    // UI PRINCIPAL
    // -----------------------------------------------------------

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("Foro", style = MaterialTheme.typography.titleMedium)
                        Text("#${pubActual.autor ?: "Anónimo"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {

            // -----------------------------------------------------------
            // LISTA DE MENSAJES
            // -----------------------------------------------------------
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CABECERA PUBLICACION
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(pubActual.contenido ?: "", style = MaterialTheme.typography.bodyLarge)
                            Text(pubActual.fechaCreacion ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                            if (adjuntosPrincipales.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(adjuntosPrincipales) { adj ->
                                        ImagenOficial(adj)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }

                // CHAT
                items(chatList) { item ->
                    when (item) {
                        is ChatItem.Comentario -> {
                            ChatBubble(
                                autor = item.dto.autor ?: "Anónimo",
                                fecha = item.dto.fechaCreacion ?: "",
                                esMio = item.dto.autor == usuarioActual,
                                contenido = {
                                    val parent = pubActual.comentarios?.find { it.id == item.dto.parent }
                                    if (parent != null) {
                                        Text("↳ @${parent.autor}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(item.dto.contenido ?: "")
                                },
                                footer = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                comentarioAResponder = item.dto.autor
                                                parentId = item.dto.id
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.Gray)
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { vm.toggleLike(token, item.dto.id, pubActual.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                if (item.dto.meGustaUsuario) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                                null,
                                                tint = if (item.dto.meGustaUsuario) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                        }

                                        if (item.dto.totalLikes > 0)
                                            Text(" ${item.dto.totalLikes}", fontSize = 12.sp, color = Color.Gray)

                                        if (item.dto.autor == usuarioActual) {
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { itemAEliminar = DeleteTarget.Comentario(item.dto.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        is ChatItem.Adjunto -> {
                            ChatBubble(
                                autor = item.dto.autor ?: "Anónimo",
                                fecha = item.dto.fechaCreacion ?: "",
                                esMio = item.dto.autor == usuarioActual,
                                contenido = {
                                    Column {
                                        when (item.dto.tipoArchivo) {
                                            "imagen" -> {
                                                AsyncImage(
                                                    model = item.dto.url,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 250.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                            "audio" -> {
                                                // 🔊 INTEGRACIÓN DEL REPRODUCTOR DE AUDIO
                                                if (!item.dto.url.isNullOrBlank()) {
                                                    AudioPlayer(audioUrl = item.dto.url)
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Audiotrack, null, tint = Color.Red)
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Error: Audio no disponible")
                                                    }
                                                }
                                            }

                                            // Manejar otros documentos (PDF, DOCX, etc.)
                                            else -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
                                                    Spacer(Modifier.width(8.dp))

                                                    // 📝 Nombre del archivo
                                                    val fileName = item.dto.archivo ?: "documento_adjunto"
                                                    Text(
                                                        fileName,
                                                        modifier = Modifier.weight(1f),
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )

                                                    // ⬇️ Botón de Descarga
                                                    IconButton(
                                                        onClick = {
                                                            if (!item.dto.url.isNullOrBlank()) {
                                                                startDownload(context, item.dto.url, fileName)
                                                                Toast.makeText(context, "Descarga iniciada", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "URL de descarga no disponible", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = "Descargar documento", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }

                                        if (!item.dto.descripcion.isNullOrBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(item.dto.descripcion, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                },
                                footer = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        IconButton(
                                            onClick = {
                                                comentarioAResponder = item.dto.autor
                                                parentId = null
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.Gray)
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { vm.toggleLikeAdjunto(token, item.dto.id, pubActual.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                if (item.dto.meGustaUsuario) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                                null,
                                                tint = if (item.dto.meGustaUsuario) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                        }

                                        if (item.dto.totalLikes > 0)
                                            Text(" ${item.dto.totalLikes}", fontSize = 12.sp, color = Color.Gray)

                                        if (item.dto.autor == usuarioActual) {
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { itemAEliminar = DeleteTarget.Adjunto(item.dto.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------
            // INPUT
            // -----------------------------------------------------------

            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(8.dp)
            ) {

                if (comentarioAResponder != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Respondiendo a @$comentarioAResponder", fontSize = 12.sp)
                        Icon(
                            Icons.Default.Close,
                            null,
                            modifier = Modifier.clickable {
                                comentarioAResponder = null
                                parentId = null
                            }
                        )
                    }
                }

                if (errorSend != null)
                    Text(errorSend, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {

                    // IMAGEN
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary)
                    }

                    // DOCUMENTO
                    IconButton(onClick = { docLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Description, null, tint = Color.Gray)
                    }

                    // AUDIO
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                stopRecordingAndSend()  // Detiene la grabación y envía
                            } else {
                                startRecording()        // Empieza la grabación
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            "Grabar",
                            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.secondary
                        )
                    }
                    // TEXTO
                    OutlinedTextField(
                        value = nuevoComentario,
                        onValueChange = { nuevoComentario = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        placeholder = { Text(if (isRecording) "Grabando..." else "Comentar...") },
                        shape = RoundedCornerShape(24.dp)
                    )

                    // ENVIAR
                    IconButton(
                        onClick = {
                            vm.comentar(token, pubActual.id, nuevoComentario, parentId)
                            nuevoComentario = ""
                            comentarioAResponder = null
                            parentId = null
                        },
                        enabled = !posting && nuevoComentario.isNotBlank()
                    ) {
                        if (posting)
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else
                            Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// COMPONENTES
// -----------------------------------------------------------

@Composable
fun ImagenOficial(adjunto: AdjuntoDto) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(adjunto.url).crossfade(true).build(),
        contentDescription = null,
        modifier = Modifier.size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ChatBubble(
    autor: String,
    fecha: String,
    esMio: Boolean,
    contenido: @Composable () -> Unit,
    footer: (@Composable () -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        if (!esMio) {
            Surface(
                shape = CircleShape,
                color = Color.Gray,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(autor.take(1).uppercase(), color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f, fill = false)) {

            Row(verticalAlignment = Alignment.Bottom) {
                Text(autor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(fecha, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (esMio) 12.dp else 2.dp,
                    bottomEnd = if (esMio) 2.dp else 12.dp
                ),
                color = if (esMio) MaterialTheme.colorScheme.primaryContainer else Color.White,
                border = if (esMio) null else BorderStroke(1.dp, Color.LightGray),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    contenido()
                    if (footer != null) {
                        Spacer(Modifier.height(8.dp))
                        footer()
                    }
                }
            }
        }
    }
}