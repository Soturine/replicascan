package com.soturine.replicascan.core.data.ocr

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { error ->
        continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}

