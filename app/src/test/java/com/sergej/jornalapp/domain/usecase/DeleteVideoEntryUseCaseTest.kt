package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.model.DomainError
import com.sergej.jornalapp.domain.model.DomainResult
import com.sergej.jornalapp.domain.model.VideoJournalEntry
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

class DeleteVideoEntryUseCaseTest {

    @Test
    fun `given existing entry when invoke then deletes file and journal row`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        val entry = testEntry()
        coEvery { storageRepository.deleteVideoFile(entry.filePath) } just Runs
        coEvery { journalRepository.deleteEntry(entry.id) } just Runs
        val useCase = DeleteVideoEntryUseCase(journalRepository, storageRepository)

        val result = useCase(entry)

        assertTrue(result is DomainResult.Success)
        coVerify(exactly = 1) { storageRepository.deleteVideoFile(entry.filePath) }
        coVerify(exactly = 1) { journalRepository.deleteEntry(entry.id) }
    }

    @Test
    fun `given file deletion fails when invoke then returns error and does not delete row`() = runTest {
        val journalRepository = mockk<VideoJournalRepository>()
        val storageRepository = mockk<VideoStorageRepository>()
        val entry = testEntry()
        coEvery { storageRepository.deleteVideoFile(entry.filePath) } throws
            IllegalStateException("delete failed")
        val useCase = DeleteVideoEntryUseCase(journalRepository, storageRepository)

        val result = useCase(entry)

        assertTrue(result is DomainResult.Error)
        assertEquals(DomainError.DELETE_VIDEO_ENTRY_FAILED, (result as DomainResult.Error).reason)
        coVerify(exactly = 0) { journalRepository.deleteEntry(any()) }
    }

    private fun testEntry(): VideoJournalEntry {
        return VideoJournalEntry(
            id = 7L,
            filePath = "/videos/one.mp4",
            description = "Evening",
            createdAtEpochMs = 100L,
        )
    }
}
