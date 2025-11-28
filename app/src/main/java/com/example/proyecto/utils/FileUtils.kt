package com.example.proyecto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface // Usamos la nativa de Android
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun uriToFile(context: Context, uri: Uri): File? {
    try {
        val contentResolver = context.contentResolver

        // 1. Obtener Orientación EXIF original
        // Abrimos un stream solo para leer los metadatos
        var inputForExif: InputStream? = contentResolver.openInputStream(uri)
        val exif = inputForExif?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
        inputForExif?.close()

        // 2. Decodificar la imagen (reducida si es muy grande)
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) return null

        // 3. Rotar la imagen si es necesario (según EXIF)
        val rotatedBitmap = rotateBitmap(originalBitmap, orientation)

        // 4. Redimensionar si es gigante (Max 1024px) para optimizar chat
        val maxDimension = 1024
        val ratio = Math.min(
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

fun getMimeType(file: File): String {
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
