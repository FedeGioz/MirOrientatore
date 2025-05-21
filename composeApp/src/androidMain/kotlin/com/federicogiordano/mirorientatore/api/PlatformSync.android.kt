package com.federicogiordano.mirorientatore.api

internal actual fun <R> synchronizedBlock(lock: Any, block: () -> R): R = synchronized(lock, block)