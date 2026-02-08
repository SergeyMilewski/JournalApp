package com.sergej.jornalapp.domain.repository

interface VideoStorageRepository {
    fun createPendingCaptureUri(): String
    suspend fun persistCapturedVideo(captureUri: String): String
}
