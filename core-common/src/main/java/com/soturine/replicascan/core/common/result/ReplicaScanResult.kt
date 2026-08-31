package com.soturine.replicascan.core.common.result

sealed interface ReplicaScanResult<out T> {
    data class Success<T>(val value: T) : ReplicaScanResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : ReplicaScanResult<Nothing>
}

