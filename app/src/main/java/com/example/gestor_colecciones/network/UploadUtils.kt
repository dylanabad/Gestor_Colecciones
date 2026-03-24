package com.example.gestor_colecciones.network

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

object UploadUtils {
    fun createImagePart(context: Context, uri: Uri): MultipartBody.Part {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/*"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val filename = "upload_${System.currentTimeMillis()}"
        return MultipartBody.Part.createFormData("file", filename, requestBody)
    }
}
