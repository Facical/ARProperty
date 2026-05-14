import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String, defaultValue: String): String =
    localProperties.getProperty(name, defaultValue)

val mapsApiKey = localProperty("MAPS_API_KEY", "")
val geospatialApiKey = localProperty("GEOSPATIAL_API_KEY", "")
val baseUrl = localProperty("ARPROPERTY_BASE_URL", "http://10.0.2.2:8080/")

// 외부 빌더 환경에서도 동작하도록 fallback에 실제 네이티브 앱 키를 둔다.
// local.properties의 KAKAO_NATIVE_APP_KEY가 있으면 그 값이 우선.
// 카카오 콘솔(앱 ID 1455963 ARProperty)의 네이티브 앱 키 — 패키지명 + 키해시로 묶여 있어
// 다른 패키지에서는 동작하지 않음. 다만 public 리포지토리면 노출됨을 인지하고 사용.
val kakaoNativeAppKey =
    localProperty("KAKAO_NATIVE_APP_KEY", "140f1bd318e9835665f9673c6dd142a1")
val hasKakaoNativeAppKey = kakaoNativeAppKey.isNotBlank()

android {
    namespace = "com.arproperty.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arproperty.android"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("boolean", "HAS_MAPS_API_KEY", mapsApiKey.isNotBlank().toString())
        buildConfigField("boolean", "HAS_GEOSPATIAL_API_KEY", geospatialApiKey.isNotBlank().toString())
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("boolean", "HAS_KAKAO_NATIVE_APP_KEY", hasKakaoNativeAppKey.toString())
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        manifestPlaceholders["geospatialApiKey"] = geospatialApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)
    implementation(libs.arcore)
    implementation(libs.arsceneview)
    implementation(libs.google.material)

    // 카카오 공통 모듈
    implementation("com.kakao.sdk:v2-common:2.20.6")
    // 카카오 지도
    implementation("com.kakao.maps.open:android:2.13.1")

    testImplementation(libs.junit4)
}
