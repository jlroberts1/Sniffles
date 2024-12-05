package com.contexts.example

import android.app.Application
import com.contexts.sniffles.Sniffles
import org.koin.core.context.startKoin

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule)
        }
        Sniffles.init(this)
    }
}