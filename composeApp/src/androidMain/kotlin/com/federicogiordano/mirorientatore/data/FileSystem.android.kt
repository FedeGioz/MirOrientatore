package com.federicogiordano.mirorientatore.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

@SuppressLint("StaticFieldLeak")
actual object FileSystem {
    private var appContext: Context? = null

    actual fun initialize(context: Any?) {
        if (this.appContext == null && context is Context) {
            this.appContext = context.applicationContext
            println("FileSystem initialized with Android context.")
        } else if (this.appContext != null) {
            println("FileSystem already initialized.")
        } else if (context == null) {
            println("Warning: FileSystem initialize called with null context, and not yet initialized. File operations may fail.")
        }
    }

    private fun getFilesDir(): File? {
        if (appContext == null) {
            println("Error: FileSystem (for internal files) not initialized. Call initialize(context) first.")
            return null
        }
        return appContext?.filesDir
    }

    actual fun readTextFromFile(fileName: String): String? {
        val filesDir = getFilesDir() ?: return null
        val file = File(filesDir, fileName)
        return try {
            if (file.exists()) {
                file.readText()
            } else {
                println("File not found: ${file.absolutePath}")
                null
            }
        } catch (e: IOException) {
            println("Error reading file $fileName: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    actual fun writeTextToFile(fileName: String, content: String): Boolean {
        val filesDir = getFilesDir() ?: return false
        val file = File(filesDir, fileName)
        return try {
            file.writeText(content)
            println("Successfully wrote to file: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            println("Error writing file $fileName: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            println("Error reading from URI $uri: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            true
        } catch (e: Exception) {
            println("Error writing to URI $uri: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}