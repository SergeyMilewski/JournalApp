package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.model.DomainError
import com.sergej.jornalapp.domain.model.DomainResult
import com.sergej.jornalapp.domain.model.VideoJournalEntry
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import com.sergej.jornalapp.domain.repository.VideoStorageRepository

class DeleteVideoEntryUseCase(
    private val journalRepository: VideoJournalRepository,
    private val storageRepository: VideoStorageRepository,
) {
    suspend operator fun invoke(entry: VideoJournalEntry): DomainResult<Unit> {
        return try {
            storageRepository.deleteVideoFile(entry.filePath)
            journalRepository.deleteEntry(entry.id)
            DomainResult.Success(Unit)
        } catch (error: Throwable) {
            DomainResult.Error(
                reason = DomainError.DELETE_VIDEO_ENTRY_FAILED,
                cause = error,
            )
        }
    }
}
