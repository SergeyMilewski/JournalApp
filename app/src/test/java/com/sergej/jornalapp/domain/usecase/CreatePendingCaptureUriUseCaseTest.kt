package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.model.DomainError
import com.sergej.jornalapp.domain.model.DomainResult
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreatePendingCaptureUriUseCaseTest {

    @Test
    fun `given storage repository when invoke then returns pending capture uri`() {
        val storageRepository = mockk<VideoStorageRepository>()
        val useCase = CreatePendingCaptureUriUseCase(storageRepository)
        every { storageRepository.createPendingCaptureUri() } returns "content://capture/generated.mp4"

        val result = useCase()

        assertTrue(result is DomainResult.Success)
        assertEquals("content://capture/generated.mp4", (result as DomainResult.Success).data)
        verify(exactly = 1) { storageRepository.createPendingCaptureUri() }
    }

    @Test
    fun `given storage repository failure when invoke then returns error result`() {
        val storageRepository = mockk<VideoStorageRepository>()
        val useCase = CreatePendingCaptureUriUseCase(storageRepository)
        every { storageRepository.createPendingCaptureUri() } throws IllegalStateException("create failed")

        val result = useCase()

        assertTrue(result is DomainResult.Error)
        assertEquals(DomainError.CREATE_CAPTURE_URI_FAILED, (result as DomainResult.Error).reason)
        verify(exactly = 1) { storageRepository.createPendingCaptureUri() }
    }
}
