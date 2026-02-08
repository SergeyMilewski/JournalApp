package com.sergej.jornalapp.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sergej.jornalapp.domain.usecase.CreatePendingCaptureUriUseCase
import com.sergej.jornalapp.domain.usecase.ObserveVideoEntriesUseCase
import com.sergej.jornalapp.domain.usecase.SaveCapturedVideoUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoJournalViewModel(
    observeVideoEntriesUseCase: ObserveVideoEntriesUseCase,
    private val createPendingCaptureUriUseCase: CreatePendingCaptureUriUseCase,
    private val saveCapturedVideoUseCase: SaveCapturedVideoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoJournalUiState())
    val uiState: StateFlow<VideoJournalUiState> = _uiState.asStateFlow()

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
        runCatching { createPendingCaptureUriUseCase() }
            .onSuccess { uri ->
                _uiState.update { current -> current.copy(pendingCaptureUri = uri) }
                sendEvent(VideoJournalEvent.LaunchCapture(uri))
            }
            .onFailure {
                sendEvent(VideoJournalEvent.ShowMessage("Unable to access camera output file."))
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
            runCatching {
                saveCapturedVideoUseCase(
                    captureUri = pendingUri,
                    description = _uiState.value.descriptionDraft,
                )
            }.onSuccess {
                _uiState.update { current ->
                    current.copy(
                        descriptionDraft = "",
                        pendingCaptureUri = null,
                        isSaving = false,
                    )
                }
            }.onFailure {
                _uiState.update { current -> current.copy(pendingCaptureUri = null, isSaving = false) }
                sendEvent(VideoJournalEvent.ShowMessage("Failed to save clip."))
            }
        }
    }

    private fun sendEvent(event: VideoJournalEvent) {
        _events.trySend(event)
    }
}
