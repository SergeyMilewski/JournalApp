package com.sergej.jornalapp.domain.repository

import com.sergej.jornalapp.domain.model.VideoJournalEntry
import kotlinx.coroutines.flow.Flow

interface VideoJournalRepository {
    fun observeEntries(): Flow<List<VideoJournalEntry>>
    suspend fun insertEntry(filePath: String, description: String?, createdAtEpochMs: Long)
    suspend fun deleteEntry(entryId: Long)
}
