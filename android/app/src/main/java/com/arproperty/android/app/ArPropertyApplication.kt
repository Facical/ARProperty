package com.arproperty.android.app

import android.app.Application
import android.util.Log
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

        if (BuildConfig.HAS_KAKAO_NATIVE_APP_KEY) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        } else {
            Log.w(
                "ArPropertyApp",
                "KAKAO_NATIVE_APP_KEY 미설정 — 카카오맵 SDK init 스킵. " +
                    "local.properties 또는 build.gradle.kts의 fallback에 실제 키를 넣어주세요.",
            )
        }
    }
}
