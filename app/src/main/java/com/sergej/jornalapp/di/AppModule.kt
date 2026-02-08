package com.sergej.jornalapp.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sergej.jornalapp.common.coroutines.DefaultDispatcherProvider
import com.sergej.jornalapp.common.coroutines.DispatcherProvider
import com.sergej.jornalapp.data.local.FileVideoStorageRepository
import com.sergej.jornalapp.data.local.VideoJournalRepositoryImpl
import com.sergej.jornalapp.data.local.db.JournalDatabase
import com.sergej.jornalapp.domain.repository.VideoJournalRepository
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import com.sergej.jornalapp.domain.usecase.CreatePendingCaptureUriUseCase
import com.sergej.jornalapp.domain.usecase.DeleteVideoEntryUseCase
import com.sergej.jornalapp.domain.usecase.ObserveVideoEntriesUseCase
import com.sergej.jornalapp.domain.usecase.SaveCapturedVideoUseCase
import com.sergej.jornalapp.presentation.journal.VideoJournalViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }

    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = JournalDatabase.Schema,
            context = get(),
            name = "video_journal.db",
        )
    }
    single { JournalDatabase(driver = get()) }
    single { get<JournalDatabase>().videoJournalQueries }

    single<VideoJournalRepository> { VideoJournalRepositoryImpl(get(), get()) }
    single<VideoStorageRepository> { FileVideoStorageRepository(get(), get()) }

    factory { ObserveVideoEntriesUseCase(get()) }
    factory { CreatePendingCaptureUriUseCase(get()) }
    factory { SaveCapturedVideoUseCase(get(), get()) }
    factory { DeleteVideoEntryUseCase(get(), get()) }

    viewModel { VideoJournalViewModel(get(), get(), get(), get()) }
}
