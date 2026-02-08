package com.sergej.jornalapp.domain.usecase

import app.cash.turbine.test
import com.sergej.jornalapp.domain.model.VideoJournalEntry
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveVideoEntriesUseCaseTest {

    @Test
    fun `given repository entries when invoke then emits the same entries stream`() = runTest {
        val entriesFlow = MutableStateFlow<List<VideoJournalEntry>>(emptyList())
        val repository = mockk<VideoJournalRepository>()
        every { repository.observeEntries() } returns entriesFlow

        val useCase = ObserveVideoEntriesUseCase(repository)
        val expectedEntries = listOf(
            VideoJournalEntry(
                id = 1L,
                filePath = "/videos/one.mp4",
                description = "Morning",
                createdAtEpochMs = 10L,
            ),
        )

        useCase().test {
            assertEquals(emptyList<VideoJournalEntry>(), awaitItem())

            entriesFlow.value = expectedEntries

            assertEquals(expectedEntries, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { repository.observeEntries() }
    }
}
