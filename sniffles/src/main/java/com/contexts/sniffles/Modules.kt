package com.contexts.sniffles

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    viewModel { DebugMenuViewModel() }
}