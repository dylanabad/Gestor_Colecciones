package com.example.gestor_colecciones.network

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Utilidad para preparar imágenes y poder enviarlas en peticiones multipart (subida de archivos)
/**
 * Helpers para transformar URIs y archivos locales en partes multipart compatibles con Retrofit.
 */
object UploadUtils {

    // Crea una parte multipart a partir de una imagen seleccionada desde el dispositivo
    fun createImagePart(context: Context, uri: Uri): MultipartBody.Part {

        // Acceso al ContentResolver para leer datos del URI
        val resolver = context.contentResolver

        // Obtiene el tipo MIME del archivo (si no existe, usa un tipo genérico de imagen)
        val mime = resolver.getType(uri) ?: "image/*"

        // Abre el stream del archivo y lo convierte a un array de bytes
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)

        // Convierte los bytes en un RequestBody con el tipo MIME correspondiente
        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())

        // Genera un nombre único basado en el tiempo actual
        val filename = "upload_${System.currentTimeMillis()}"

        // Crea la parte multipart con el nombre del campo "file"
        return MultipartBody.Part.createFormData("file", filename, requestBody)
    }
}