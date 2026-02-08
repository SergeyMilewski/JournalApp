package com.sergej.jornalapp.presentation.journal

import com.sergej.jornalapp.domain.model.VideoJournalEntry

data class VideoJournalUiState(
    val descriptionDraft: String = "",
    val entries: List<VideoJournalEntry> = emptyList(),
    val pendingCaptureUri: String? = null,
    val isSaving: Boolean = false,
)

sealed interface VideoJournalEvent {
    data class LaunchCapture(val uri: String) : VideoJournalEvent
    data class ShowMessage(val message: String) : VideoJournalEvent
}
