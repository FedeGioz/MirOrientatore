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
            println("FileSystem inizializzato con contesto Android.")
        } else if (this.appContext != null) {
            println("FileSystem già inizializzato.")
        } else if (context == null) {
            println("Attenzione: FileSystem initialize chiamato con contesto null, e non ancora inizializzato. Le operazioni sui file potrebbero fallire.")
        }
    }

    private fun getFilesDir(): File? {
        if (appContext == null) {
            println("Errore: FileSystem (per i file interni) non inizializzato. Chiamare prima initialize(context).")
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
                println("File non trovato: ${file.absolutePath}")
                null
            }
        } catch (e: IOException) {
            println("Errore durante la lettura del file $fileName: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    actual fun writeTextToFile(fileName: String, content: String): Boolean {
        val filesDir = getFilesDir() ?: return false
        val file = File(filesDir, fileName)
        return try {
            file.writeText(content)
            println("Scrittura del file completata con successo: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            println("Errore durante la scrittura del file $fileName: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            println("Errore durante la lettura da URI $uri: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            true
        } catch (e: Exception) {
            println("Errore durante la scrittura su URI $uri: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}