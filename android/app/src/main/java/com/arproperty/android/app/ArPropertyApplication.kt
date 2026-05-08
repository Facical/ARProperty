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

        // local.properties에 KAKAO_NATIVE_APP_KEY를 넣으면 카카오맵 SDK가 초기화됩니다
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
