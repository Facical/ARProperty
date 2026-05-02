package com.arproperty.android.app

import android.app.Application
import com.arproperty.android.core.network.AppContainer
import com.arproperty.android.core.network.DefaultAppContainer

class ArPropertyApplication : Application() {
    val appContainer: AppContainer by lazy {
        DefaultAppContainer(applicationContext)
    }
}
