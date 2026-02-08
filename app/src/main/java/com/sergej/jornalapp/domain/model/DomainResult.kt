package com.sergej.jornalapp.domain.model

sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Error(
        val reason: DomainError,
        val cause: Throwable? = null,
    ) : DomainResult<Nothing>
}

enum class DomainError {
    CREATE_CAPTURE_URI_FAILED,
    SAVE_CAPTURED_VIDEO_FAILED,
    DELETE_VIDEO_ENTRY_FAILED,
}
