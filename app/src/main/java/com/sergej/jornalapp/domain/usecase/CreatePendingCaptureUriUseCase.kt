package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.model.DomainError
import com.sergej.jornalapp.domain.model.DomainResult
import com.sergej.jornalapp.domain.repository.VideoStorageRepository

class CreatePendingCaptureUriUseCase(
    private val storageRepository: VideoStorageRepository,
) {
    operator fun invoke(): DomainResult<String> {
        return try {
            DomainResult.Success(storageRepository.createPendingCaptureUri())
        } catch (error: Throwable) {
            DomainResult.Error(
                reason = DomainError.CREATE_CAPTURE_URI_FAILED,
                cause = error,
            )
        }
    }
}
