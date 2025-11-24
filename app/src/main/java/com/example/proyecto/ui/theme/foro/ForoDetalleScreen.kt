package com.example.proyecto.ui.theme.foro

import android.net.Uri
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrokenImage
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.proyecto.data.AdjuntoDto
import com.example.proyecto.data.ComentarioDto
import com.example.proyecto.data.PublicacionDto
import com.example.proyecto.utils.getMimeType
import com.example.proyecto.utils.uriToFile
import com.example.proyecto.viewmodel.ForoViewModel

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
        if (vm.uiState.publicaciones.isEmpty()) {
            vm.cargar(token)
        }
    }

    val state = vm.uiState
    val pubActual = state.publicaciones.find { it.id == publicacion.id } ?: publicacion
    val posting = state.posting[pubActual.id] == true
    val errorSend = state.postError[pubActual.id]

    var nuevoComentario by remember(pubActual.id) { mutableStateOf("") }
    var itemAEliminar by remember { mutableStateOf<DeleteTarget?>(null) }
    var comentarioAResponder by remember { mutableStateOf<String?>(null) }
    var parentId by remember { mutableStateOf<Int?>(null) }
    var imagenPreviewUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imagenPreviewUri = uri
    }

    // 1. CORRECCIÓN: Usamos '?: emptyList()' para manejar nulos en adjuntos
    val adjuntosPrincipales = remember(pubActual) {
        (pubActual.adjuntos ?: emptyList()).filter { !it.esMensaje }
    }

    // 2. CORRECCIÓN: Usamos '?: emptyList()' para manejar nulos en chat
    val chatList = remember(pubActual) {
        val comentariosItems = (pubActual.comentarios ?: emptyList()).map { ChatItem.Comentario(it) }
        val adjuntosItems = (pubActual.adjuntos ?: emptyList())
            .filter { it.esMensaje }
            .map { ChatItem.Adjunto(it) }

        (comentariosItems + adjuntosItems).sortedBy { it.fecha }
    }

    // --- DIÁLOGOS ---
    if (imagenPreviewUri != null) {
        var captionText by remember { mutableStateOf("") }
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
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("Comentario opcional...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { imagenPreviewUri = null }) { Text("Cancelar") }
                        Button(onClick = {
                            imagenPreviewUri?.let { uri ->
                                val file = uriToFile(context, uri)
                                if (file != null) {
                                    vm.enviarArchivo(token, pubActual.id, file, getMimeType(file), captionText)
                                }
                            }
                            imagenPreviewUri = null
                        }) { Text("Enviar") }
                    }
                }
            }
        }
    }

    if (itemAEliminar != null) {
        AlertDialog(
            onDismissRequest = { itemAEliminar = null },
            title = { Text("Eliminar") },
            text = { Text("¿Estás seguro?") },
            confirmButton = {
                TextButton(onClick = {
                    when(val t = itemAEliminar) {
                        is DeleteTarget.Comentario -> vm.eliminarComentario(token, t.id, pubActual.id)
                        is DeleteTarget.Adjunto -> vm.eliminarAdjunto(token, t.id, pubActual.id)
                        null -> {}
                    }
                    itemAEliminar = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { itemAEliminar = null }) { Text("Cancelar") } }
        )
    }

    // --- UI PRINCIPAL ---
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("Foro", style = MaterialTheme.typography.titleMedium)
                        Text("#${pubActual.autor ?: "Anónimo"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(pubActual.contenido ?: "", style = MaterialTheme.typography.bodyLarge)
                            Text(pubActual.fechaCreacion ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                            if (adjuntosPrincipales.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(adjuntosPrincipales) { adj -> ImagenOficial(adj) }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }

                // Chat
                items(chatList) { item ->
                    when (item) {
                        is ChatItem.Comentario -> {
                            ChatBubble(
                                autor = item.dto.autor ?: "Anónimo",
                                fecha = item.dto.fechaCreacion ?: "",
                                esMio = item.dto.autor == usuarioActual,
                                contenido = {
                                    val parent = (pubActual.comentarios ?: emptyList()).find { it.id == item.dto.parent }
                                    if (parent != null) {
                                        Text("↳ @${parent.autor ?: "..."}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(item.dto.contenido ?: "")
                                },
                                footer = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { comentarioAResponder = item.dto.autor; parentId = item.dto.id }, modifier = Modifier.size(20.dp)) { Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.Gray) }
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(onClick = { vm.toggleLike(token, item.dto.id, pubActual.id) }, modifier = Modifier.size(20.dp)) { Icon(if (item.dto.meGustaUsuario) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null, tint = if (item.dto.meGustaUsuario) MaterialTheme.colorScheme.primary else Color.Gray) }
                                        if (item.dto.totalLikes > 0) Text(" ${item.dto.totalLikes}", fontSize = 12.sp, color = Color.Gray)
                                        if (item.dto.autor == usuarioActual) {
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(onClick = { itemAEliminar = DeleteTarget.Comentario(item.dto.id) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
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
                                        if (item.dto.tipoArchivo == "imagen") {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(item.dto.url).crossfade(true).build(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Row { Icon(Icons.Default.Audiotrack, null); Text(" Audio") }
                                        }

                                        if (!item.dto.descripcion.isNullOrBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(item.dto.descripcion, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                },
                                footer = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { comentarioAResponder = item.dto.autor; parentId = null }, modifier = Modifier.size(20.dp)) { Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color.Gray) }
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(onClick = { vm.toggleLikeAdjunto(token, item.dto.id, pubActual.id) }, modifier = Modifier.size(20.dp)) { Icon(if (item.dto.meGustaUsuario) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null, tint = if (item.dto.meGustaUsuario) MaterialTheme.colorScheme.primary else Color.Gray) }
                                        if (item.dto.totalLikes > 0) Text(" ${item.dto.totalLikes}", fontSize = 12.sp, color = Color.Gray)
                                        if (item.dto.autor == usuarioActual) {
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(onClick = { itemAEliminar = DeleteTarget.Adjunto(item.dto.id) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Input
            Column(modifier = Modifier.background(Color.White).padding(8.dp)) {
                if (comentarioAResponder != null) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Respondiendo a @$comentarioAResponder", fontSize = 12.sp)
                        Icon(Icons.Default.Close, null, modifier = Modifier.clickable { comentarioAResponder = null; parentId = null })
                    }
                }
                if (errorSend != null) Text(errorSend, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) }
                    OutlinedTextField(
                        value = nuevoComentario,
                        onValueChange = { nuevoComentario = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        placeholder = { Text("Comentar...") },
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(
                        onClick = {
                            vm.comentar(token, pubActual.id, nuevoComentario, parentId)
                            nuevoComentario = ""
                            comentarioAResponder = null
                            parentId = null
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

@Composable
fun ImagenOficial(adjunto: AdjuntoDto) {
    val context = LocalContext.current
    AsyncImage(model = ImageRequest.Builder(context).data(adjunto.url).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
}

@Composable
fun ChatBubble(autor: String, fecha: String, esMio: Boolean, contenido: @Composable () -> Unit, footer: (@Composable () -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start) {
        if (!esMio) {
            Surface(shape = CircleShape, color = Color.Gray, modifier = Modifier.size(32.dp)) { Box(contentAlignment = Alignment.Center) { Text(autor.take(1).uppercase(), color = Color.White, fontSize = 14.sp) } }
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
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = if (esMio) 12.dp else 2.dp, bottomEnd = if (esMio) 2.dp else 12.dp),
                color = if (esMio) MaterialTheme.colorScheme.primaryContainer else Color.White,
                border = if (esMio) null else BorderStroke(1.dp, Color.LightGray),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    contenido()
                    if (footer != null) { Spacer(Modifier.height(8.dp)); footer() }
                }
            }
        }
    }
}