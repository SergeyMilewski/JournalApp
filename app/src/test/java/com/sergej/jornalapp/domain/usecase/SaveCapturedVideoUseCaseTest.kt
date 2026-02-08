package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.model.DomainError
import com.sergej.jornalapp.domain.model.DomainResult
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaveCapturedVideoUseCaseTest {

    @Test
    fun `given captured video and description when invoke then persists file and stores entry`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        coEvery { journalRepository.insertEntry(any(), any(), any()) } just Runs
        coEvery { storageRepository.persistCapturedVideo("content://capture/video.mp4") } returns "/local/videos/saved.mp4"

        val useCase = SaveCapturedVideoUseCase(journalRepository, storageRepository)

        val result = useCase(captureUri = "content://capture/video.mp4", description = "Morning walk")

        coVerify(exactly = 1) { storageRepository.persistCapturedVideo("content://capture/video.mp4") }
        coVerify(exactly = 1) { journalRepository.insertEntry("/local/videos/saved.mp4", "Morning walk", any()) }
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `given blank description when invoke then stores null description`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        coEvery { journalRepository.insertEntry(any(), any(), any()) } just Runs
        coEvery { storageRepository.persistCapturedVideo("content://capture/video.mp4") } returns "/local/videos/saved.mp4"

        val useCase = SaveCapturedVideoUseCase(journalRepository, storageRepository)

        val result = useCase(captureUri = "content://capture/video.mp4", description = "   ")

        coVerify(exactly = 1) { journalRepository.insertEntry("/local/videos/saved.mp4", null, any()) }
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `given storage persist failure when invoke then returns error and does not insert entry`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        coEvery { storageRepository.persistCapturedVideo("content://capture/video.mp4") } throws
            IllegalStateException("persist failed")

        val useCase = SaveCapturedVideoUseCase(journalRepository, storageRepository)

        val result = useCase(captureUri = "content://capture/video.mp4", description = "Morning walk")

        assertTrue(result is DomainResult.Error)
        assertEquals(DomainError.SAVE_CAPTURED_VIDEO_FAILED, (result as DomainResult.Error).reason)
        coVerify(exactly = 0) { journalRepository.insertEntry(any(), any(), any()) }
    }
}
