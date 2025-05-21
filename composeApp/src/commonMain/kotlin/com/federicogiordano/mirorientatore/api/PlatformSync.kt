package com.federicogiordano.mirorientatore.api

internal expect fun <R> synchronizedBlock(lock: Any, block: () -> R): R