package com.example.echochat.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * 将 URI 指向的文件拷贝到应用内部存储，返回本地路径
 */
fun saveUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
