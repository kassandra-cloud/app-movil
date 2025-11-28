package com.example.proyecto.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Reproductor de audio simple para Compose usando ExoPlayer.
 * @param audioUrl La URL pública del archivo de audio (e.g., MP3, M4A).
 */
@Composable
fun AudioPlayer(audioUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Estado para controlar si el audio está en reproducción
    var isPlaying by remember { mutableStateOf(false) }

    // 1. Crear el ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
            prepare()
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                    isPlaying = isPlayingParam // Actualizar el estado de Compose
                }
                // Manejo básico de errores si el archivo no carga
                override fun onPlayerError(error: PlaybackException) {
                    println("ExoPlayer Error: ${error.message}")
                }
            })
        }
    }

    // 2. Limpieza: Pausar y liberar el reproductor cuando el componente se destruye
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Play/Pause
        IconButton(
            onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar audio" else "Reproducir audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Placeholder de la barra de progreso
        // Se puede reemplazar con un componente de Seekbar más avanzado si es necesario.
        Text(
            text = "Audio adjunto",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}