package com.sergej.jornalapp.domain.usecase

import com.sergej.jornalapp.domain.repository.VideoStorageRepository

class CreatePendingCaptureUriUseCase(
    private val storageRepository: VideoStorageRepository,
) {
    operator fun invoke(): String = storageRepository.createPendingCaptureUri()
}
