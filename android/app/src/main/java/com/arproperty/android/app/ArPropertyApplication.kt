package com.arproperty.android.app

import android.app.Application
import com.arproperty.android.BuildConfig
import com.arproperty.android.core.network.AppContainer
import com.arproperty.android.core.network.DefaultAppContainer
import com.kakao.vectormap.KakaoMapSdk

class ArPropertyApplication : Application() {
    val appContainer: AppContainer by lazy {
        DefaultAppContainer(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
