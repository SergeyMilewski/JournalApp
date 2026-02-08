package com.sergej.jornalapp.presentation.journal

import app.cash.turbine.test
import com.sergej.jornalapp.domain.model.VideoJournalEntry
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import com.sergej.jornalapp.domain.usecase.CreatePendingCaptureUriUseCase
import com.sergej.jornalapp.domain.usecase.DeleteVideoEntryUseCase
import com.sergej.jornalapp.domain.usecase.ObserveVideoEntriesUseCase
import com.sergej.jornalapp.domain.usecase.SaveCapturedVideoUseCase
import com.sergej.jornalapp.testing.MainDispatcherExtension
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class VideoJournalViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    @Test
    fun `given description text when changed then updates draft`() {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.onDescriptionChanged("Evening reflection")

        assertEquals("Evening reflection", viewModel.uiState.value.descriptionDraft)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given repository entries when viewmodel starts then ui state contains entries`() = runTest {
        val expectedEntries = listOf(
            VideoJournalEntry(
                id = 1L,
                filePath = "/videos/one.mp4",
                description = "Morning",
                createdAtEpochMs = 10L,
            ),
        )
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(expectedEntries)

        val viewModel = createViewModel(journalRepository, storageRepository)
        advanceUntilIdle()

        assertEquals(expectedEntries, viewModel.uiState.value.entries)
    }

    @Test
    fun `given record request when uri creation succeeds then emits launch event and stores pending uri`() = runTest {
        val pendingUri = "content://capture/pending.mp4"
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())
        every { storageRepository.createPendingCaptureUri() } returns pendingUri

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onRecordRequested()

            assertEquals(VideoJournalEvent.LaunchCapture(pendingUri), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(pendingUri, viewModel.uiState.value.pendingCaptureUri)
    }

    @Test
    fun `given record request when uri creation fails then emits error message and keeps pending uri null`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())
        every { storageRepository.createPendingCaptureUri() } throws IllegalStateException("create failed")

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onRecordRequested()

            assertEquals(
                VideoJournalEvent.ShowMessage("Unable to access camera output file."),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(viewModel.uiState.value.pendingCaptureUri)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given successful capture when completed then saves entry and clears saving state`() = runTest {
        val pendingUri = "content://capture/pending.mp4"
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())
        every { storageRepository.createPendingCaptureUri() } returns pendingUri
        coEvery { storageRepository.persistCapturedVideo(pendingUri) } returns "/local/videos/final.mp4"
        coEvery { journalRepository.insertEntry(any(), any(), any()) } just Runs

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.onDescriptionChanged("Evening reflection")
        viewModel.onRecordRequested()
        viewModel.onCaptureCompleted(success = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { storageRepository.persistCapturedVideo(pendingUri) }
        coVerify(exactly = 1) {
            journalRepository.insertEntry("/local/videos/final.mp4", "Evening reflection", any())
        }
        assertEquals("", viewModel.uiState.value.descriptionDraft)
        assertNull(viewModel.uiState.value.pendingCaptureUri)
        assertEquals(false, viewModel.uiState.value.isSaving)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given successful capture when persist fails then emits save error and resets saving state`() = runTest {
        val pendingUri = "content://capture/pending.mp4"
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())
        every { storageRepository.createPendingCaptureUri() } returns pendingUri
        coEvery { storageRepository.persistCapturedVideo(pendingUri) } throws IllegalStateException("persist failed")

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onRecordRequested()
            assertEquals(VideoJournalEvent.LaunchCapture(pendingUri), awaitItem())

            viewModel.onCaptureCompleted(success = true)
            advanceUntilIdle()

            assertEquals(VideoJournalEvent.ShowMessage("Failed to save clip."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { journalRepository.insertEntry(any(), any(), any()) }
        assertNull(viewModel.uiState.value.pendingCaptureUri)
        assertEquals(false, viewModel.uiState.value.isSaving)
    }

    @Test
    fun `given canceled capture when completed then clears pending uri and emits canceled message`() = runTest {
        val pendingUri = "content://capture/pending.mp4"
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())
        every { storageRepository.createPendingCaptureUri() } returns pendingUri

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onRecordRequested()
            assertEquals(VideoJournalEvent.LaunchCapture(pendingUri), awaitItem())

            viewModel.onCaptureCompleted(success = false)
            advanceUntilIdle()

            assertEquals(VideoJournalEvent.ShowMessage("Recording canceled."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { storageRepository.persistCapturedVideo(any()) }
        coVerify(exactly = 0) { journalRepository.insertEntry(any(), any(), any()) }
        assertNull(viewModel.uiState.value.pendingCaptureUri)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given no pending capture when completion callback arrives then ignores callback`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        every { journalRepository.observeEntries() } returns MutableStateFlow(emptyList())

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onCaptureCompleted(success = true)
            advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { storageRepository.persistCapturedVideo(any()) }
        coVerify(exactly = 0) { journalRepository.insertEntry(any(), any(), any()) }
    }

    @Test
    fun `given entry when delete succeeds then emits success message and resets deleting state`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        val entry = VideoJournalEntry(
            id = 21L,
            filePath = "/videos/delete.mp4",
            description = "Delete me",
            createdAtEpochMs = 1L,
        )
        every { journalRepository.observeEntries() } returns MutableStateFlow(listOf(entry))
        coEvery { storageRepository.deleteVideoFile(entry.filePath) } just Runs
        coEvery { journalRepository.deleteEntry(entry.id) } just Runs

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onDeleteRequested(entry)
            advanceUntilIdle()

            assertEquals(VideoJournalEvent.ShowMessage("Entry deleted."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { storageRepository.deleteVideoFile(entry.filePath) }
        coVerify(exactly = 1) { journalRepository.deleteEntry(entry.id) }
        assertEquals(false, viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `given entry when delete fails then emits error message and keeps deleting state false`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        val entry = VideoJournalEntry(
            id = 34L,
            filePath = "/videos/delete-failed.mp4",
            description = "Delete fail",
            createdAtEpochMs = 2L,
        )
        every { journalRepository.observeEntries() } returns MutableStateFlow(listOf(entry))
        coEvery { storageRepository.deleteVideoFile(entry.filePath) } throws
            IllegalStateException("delete failed")

        val viewModel = createViewModel(journalRepository, storageRepository)

        viewModel.events.test {
            viewModel.onDeleteRequested(entry)
            advanceUntilIdle()

            assertEquals(VideoJournalEvent.ShowMessage("Failed to delete entry."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { journalRepository.deleteEntry(any()) }
        assertEquals(false, viewModel.uiState.value.isDeleting)
    }

    private fun createViewModel(
        journalRepository: VideoJournalRepository,
        storageRepository: VideoStorageRepository,
    ): VideoJournalViewModel {
        return VideoJournalViewModel(
            observeVideoEntriesUseCase = ObserveVideoEntriesUseCase(journalRepository),
            createPendingCaptureUriUseCase = CreatePendingCaptureUriUseCase(storageRepository),
            saveCapturedVideoUseCase = SaveCapturedVideoUseCase(journalRepository, storageRepository),
            deleteVideoEntryUseCase = DeleteVideoEntryUseCase(journalRepository, storageRepository),
        )
    }
}
