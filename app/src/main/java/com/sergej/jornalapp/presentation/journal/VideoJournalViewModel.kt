package com.sergej.jornalapp.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergej.jornalapp.domain.model.DomainResult
import com.sergej.jornalapp.domain.model.VideoJournalEntry
import com.sergej.jornalapp.domain.usecase.CreatePendingCaptureUriUseCase
import com.sergej.jornalapp.domain.usecase.DeleteVideoEntryUseCase
import com.sergej.jornalapp.domain.usecase.ObserveVideoEntriesUseCase
import com.sergej.jornalapp.domain.usecase.SaveCapturedVideoUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoJournalViewModel(
    observeVideoEntriesUseCase: ObserveVideoEntriesUseCase,
    private val createPendingCaptureUriUseCase: CreatePendingCaptureUriUseCase,
    private val saveCapturedVideoUseCase: SaveCapturedVideoUseCase,
    private val deleteVideoEntryUseCase: DeleteVideoEntryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoJournalUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<VideoJournalEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeVideoEntriesUseCase().collectLatest { entries ->
                _uiState.update { current -> current.copy(entries = entries) }
            }
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { current -> current.copy(descriptionDraft = description) }
    }

    fun onRecordRequested() {
        when (val result = createPendingCaptureUriUseCase()) {
            is DomainResult.Success -> {
                val uri = result.data
                _uiState.update { current -> current.copy(pendingCaptureUri = uri) }
                sendEvent(VideoJournalEvent.LaunchCapture(uri))
            }

            is DomainResult.Error -> {
                sendEvent(VideoJournalEvent.ShowMessage("Unable to access camera output file."))
            }
        }
    }

    fun onCaptureCompleted(success: Boolean) {
        val pendingUri = _uiState.value.pendingCaptureUri ?: return
        if (!success) {
            _uiState.update { current -> current.copy(pendingCaptureUri = null) }
            sendEvent(VideoJournalEvent.ShowMessage("Recording canceled."))
            return
        }

        viewModelScope.launch {
            _uiState.update { current -> current.copy(isSaving = true) }
            when (
                saveCapturedVideoUseCase(
                    captureUri = pendingUri,
                    description = _uiState.value.descriptionDraft,
                )
            ) {
                is DomainResult.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            descriptionDraft = "",
                            pendingCaptureUri = null,
                            isSaving = false,
                        )
                    }
                }

                is DomainResult.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            pendingCaptureUri = null,
                            isSaving = false,
                        )
                    }
                    sendEvent(VideoJournalEvent.ShowMessage("Failed to save clip."))
                }
            }
        }
    }

    fun onDeleteRequested(entry: VideoJournalEntry) {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(isDeleting = true) }
            when (deleteVideoEntryUseCase(entry)) {
                is DomainResult.Success -> {
                    _uiState.update { current -> current.copy(isDeleting = false) }
                    sendEvent(VideoJournalEvent.ShowMessage("Entry deleted."))
                }

                is DomainResult.Error -> {
                    _uiState.update { current -> current.copy(isDeleting = false) }
                    sendEvent(VideoJournalEvent.ShowMessage("Failed to delete entry."))
                }
            }
        }
    }

    private fun sendEvent(event: VideoJournalEvent) {
        _events.trySend(event)
    }
}
