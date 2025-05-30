package com.federicogiordano.mirorientatore.data

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

actual fun getDataStoreFile(fileName: String): Path {
    val context: Context = AppContextStore.getInstance().getAppContext()
    return context.filesDir.resolve(fileName).absolutePath.toPath()
}

object AppContextStore {
    @Volatile
    private var appContext: Context? = null
    private val lock = Any()

    fun initialize(context: Context) {
        synchronized(lock) {
            if (appContext == null) {
                appContext = context.applicationContext
            }
        }
    }

    fun getInstance(): AppContextStore {
        if (appContext == null) {
            throw IllegalStateException("AppContextStore deve essere inizializzato prima dell'uso.")
        }
        return this
    }

    fun getAppContext(): Context {
        return appContext ?: throw IllegalStateException("Contesto dell'applicazione non inizializzato.")
    }
}