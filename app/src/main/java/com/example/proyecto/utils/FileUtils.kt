package com.example.proyecto.utils

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.getSystemService
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

// ==========================================================
// 1. UTILIDADES DE IMAGEN (EXISTENTES)
// ==========================================================

fun uriToFile(context: Context, uri: Uri): File? {
    try {
        val contentResolver = context.contentResolver

        // 1. Obtener Orientación EXIF original
        var inputForExif: InputStream? = contentResolver.openInputStream(uri)
        val exif = inputForExif?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
        inputForExif?.close()

        // 2. Decodificar la imagen
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) return null

        // 3. Rotar la imagen si es necesario
        val rotatedBitmap = rotateBitmap(originalBitmap, orientation)

        // 4. Redimensionar si es gigante (Max 1024px)
        val maxDimension = 1024
        val ratio = min(
            maxDimension.toDouble() / rotatedBitmap.width,
            maxDimension.toDouble() / rotatedBitmap.height
        )

        // Solo redimensionamos si es más grande que el límite
        val finalBitmap = if (ratio < 1) {
            val width = (rotatedBitmap.width * ratio).toInt()
            val height = (rotatedBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)
        } else {
            rotatedBitmap
        }

        // 5. Guardar como JPG comprimido
        val tempFile = File.createTempFile("img_chat_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)

        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        outputStream.flush()
        outputStream.close()

        // Limpieza memoria
        if (originalBitmap != finalBitmap && originalBitmap != rotatedBitmap) originalBitmap.recycle()
        if (rotatedBitmap != finalBitmap) rotatedBitmap.recycle()

        return tempFile

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// Función auxiliar para rotar
private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap // No necesita rotación
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// ==========================================================
// 2. UTILIDADES DE TIPO MIME Y COPIA (EXISTENTES)
// ==========================================================

fun getMimeType(file: File): String {
    // Esta función podría ser más avanzada, pero por ahora retorna JPG
    return "image/jpeg"
}

fun getMimeType(context: Context, uri: Uri): String? {
    return context.contentResolver.getType(uri)
}

fun copyUriToFile(context: Context, uri: Uri, fileName: String): File {
    val tempFile = File(context.cacheDir, fileName)
    val inputStream = context.contentResolver.openInputStream(uri)
    val outputStream = FileOutputStream(tempFile)

    inputStream?.copyTo(outputStream)

    outputStream.flush()
    outputStream.close()
    inputStream?.close()

    return tempFile
}


// ==========================================================
// 3. UTILIDADES DE DESCARGA (NUEVAS)
// ==========================================================

/**
 * Intenta inferir el tipo MIME de la URL a partir de su extensión.
 */
fun getMimeTypeFromUrl(url: String): String? {
    // Intenta obtener la extensión del archivo desde la URL
    val extension = MimeTypeMap.getFileExtensionFromUrl(url)
    // Busca el tipo MIME asociado a esa extensión
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

/**
 * Inicia una descarga usando el DownloadManager del sistema Android.
 * @param context Contexto de la aplicación.
 * @param url URL pública del archivo a descargar.
 * @param fileName Nombre sugerido del archivo.
 */
fun startDownload(context: Context, url: String, fileName: String) {

    val downloadManager = context.getSystemService<DownloadManager>()
    if (downloadManager == null) {
        Log.e("Download", "DownloadManager no está disponible.")
        // Mostrar un mensaje de error al usuario (opcional)
        return
    }

    try {
        // 1. Crear la solicitud de descarga
        val request = DownloadManager.Request(Uri.parse(url)).apply {

            // 2. Configuración de la notificación
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // 3. Establecer el destino (Carpeta de Descargas)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            // 4. Título y descripción
            setTitle(fileName)
            setDescription("Descargando archivo adjunto del foro.")

            // 5. Configurar el tipo MIME
            val mimeType = getMimeTypeFromUrl(url)
            if (mimeType != null) {
                setMimeType(mimeType)
            }
        }

        // 6. Encolar la descarga
        downloadManager.enqueue(request)
        // El Toast de confirmación se maneja en el ForoDetalleScreen.kt

    } catch (e: Exception) {
        Log.e("Download", "Error al iniciar la descarga: ${e.message}")
        // Aquí podría lanzar un Toast si la URI es inválida o hay un error de permiso/red.
    }
}