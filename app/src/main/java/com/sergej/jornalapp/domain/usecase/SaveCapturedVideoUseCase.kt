package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import com.sergej.jornalapp.domain.repository.VideoStorageRepository

class SaveCapturedVideoUseCase(
    private val journalRepository: VideoJournalRepository,
    private val storageRepository: VideoStorageRepository,
) {
    suspend operator fun invoke(captureUri: String, description: String?) {
        val localPath = storageRepository.persistCapturedVideo(captureUri)
        journalRepository.insertEntry(
            filePath = localPath,
            description = description?.takeIf { it.isNotBlank() },
            createdAtEpochMs = System.currentTimeMillis(),
        )
    }
}
