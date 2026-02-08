package com.sergej.jornalapp.domain.model

data class VideoJournalEntry(
    val id: Long,
    val filePath: String,
    val description: String?,
    val createdAtEpochMs: Long,
)
