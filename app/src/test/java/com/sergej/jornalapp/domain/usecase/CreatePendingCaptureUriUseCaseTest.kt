package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CreatePendingCaptureUriUseCaseTest {

    @Test
    fun `given storage repository when invoke then returns pending capture uri`() {
        val storageRepository = mockk<VideoStorageRepository>()
        val useCase = CreatePendingCaptureUriUseCase(storageRepository)
        every { storageRepository.createPendingCaptureUri() } returns "content://capture/generated.mp4"

        val result = useCase()

        assertEquals("content://capture/generated.mp4", result)
        verify(exactly = 1) { storageRepository.createPendingCaptureUri() }
    }

    @Test
    fun `given storage repository failure when invoke then propagates exception`() {
        val storageRepository = mockk<VideoStorageRepository>()
        val useCase = CreatePendingCaptureUriUseCase(storageRepository)
        every { storageRepository.createPendingCaptureUri() } throws IllegalStateException("create failed")

        assertThrows(IllegalStateException::class.java) { useCase() }
        verify(exactly = 1) { storageRepository.createPendingCaptureUri() }
    }
}
