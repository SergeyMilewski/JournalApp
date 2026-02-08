package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SaveCapturedVideoUseCaseTest {

    @Test
    fun `given captured video and description when invoke then persists file and stores entry`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        coEvery { journalRepository.insertEntry(any(), any(), any()) } just Runs
        coEvery { storageRepository.persistCapturedVideo("content://capture/video.mp4") } returns "/local/videos/saved.mp4"

        val useCase = SaveCapturedVideoUseCase(journalRepository, storageRepository)

        useCase(captureUri = "content://capture/video.mp4", description = "Morning walk")

        coVerify(exactly = 1) { storageRepository.persistCapturedVideo("content://capture/video.mp4") }
        coVerify(exactly = 1) { journalRepository.insertEntry("/local/videos/saved.mp4", "Morning walk", any()) }
    }

    @Test
    fun `given blank description when invoke then stores null description`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        coEvery { journalRepository.insertEntry(any(), any(), any()) } just Runs
        coEvery { storageRepository.persistCapturedVideo("content://capture/video.mp4") } returns "/local/videos/saved.mp4"

        val useCase = SaveCapturedVideoUseCase(journalRepository, storageRepository)

        useCase(captureUri = "content://capture/video.mp4", description = "   ")

        coVerify(exactly = 1) { journalRepository.insertEntry("/local/videos/saved.mp4", null, any()) }
    }
}
