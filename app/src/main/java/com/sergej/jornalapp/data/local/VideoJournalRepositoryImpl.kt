package com.sergej.jornalapp.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sergej.jornalapp.data.local.db.VideoJournalQueries
import com.sergej.jornalapp.domain.model.VideoJournalEntry
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class VideoJournalRepositoryImpl(
    private val queries: VideoJournalQueries,
) : VideoJournalRepository {

    override fun observeEntries(): Flow<List<VideoJournalEntry>> {
        return queries
            .selectAll(::mapEntry)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    override suspend fun insertEntry(filePath: String, description: String?, createdAtEpochMs: Long) {
        queries.insertEntry(
            file_path = filePath,
            description = description,
            created_at_epoch_ms = createdAtEpochMs,
        )
    }

    private fun mapEntry(
        id: Long,
        file_path: String,
        description: String?,
        created_at_epoch_ms: Long,
    ): VideoJournalEntry {
        return VideoJournalEntry(
            id = id,
            filePath = file_path,
            description = description,
            createdAtEpochMs = created_at_epoch_ms,
        )
    }
}
