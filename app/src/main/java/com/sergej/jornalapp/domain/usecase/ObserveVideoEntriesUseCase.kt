package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.model.VideoJournalEntry
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import kotlinx.coroutines.flow.Flow

class ObserveVideoEntriesUseCase(
    private val repository: VideoJournalRepository,
) {
    operator fun invoke(): Flow<List<VideoJournalEntry>> = repository.observeEntries()
}
