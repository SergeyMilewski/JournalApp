package com.sergej.jornalapp

import android.app.Application
import com.sergej.jornalapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class JournalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JournalApplication)
            modules(appModule)
        }
    }
}
