package com.arproperty.android.core.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

private const val PREFS_NAME = "arproperty_debug"
private const val KEY_BASE_URL = "base_url_override"

class BaseUrlStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }

    fun set(url: String?) {
        prefs.edit().apply {
            if (url.isNullOrBlank()) remove(KEY_BASE_URL) else putString(KEY_BASE_URL, url.trim())
        }.apply()
    }
}

class BaseUrlOverrideInterceptor(private val store: BaseUrlStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val override = store.get() ?: return chain.proceed(chain.request())
        val overrideUrl = override.toHttpUrlOrNull() ?: return chain.proceed(chain.request())
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .scheme(overrideUrl.scheme)
            .host(overrideUrl.host)
            .port(overrideUrl.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
