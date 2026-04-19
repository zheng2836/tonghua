package com.zheng2836.tonghua.config

import android.content.Context

class AppConfigRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tonghua_config", Context.MODE_PRIVATE)
    private val httpKey = "server_http_base_url"
    private val wsKey = "server_ws_base_url"

    fun getServerHttpBaseUrl(): String {
        return prefs.getString(httpKey, "http://10.0.2.2:8080")!!.trim().removeSuffix("/")
    }

    fun setServerHttpBaseUrl(value: String) {
        prefs.edit().putString(httpKey, value.trim().removeSuffix("/")).apply()
    }

    fun getServerWsBaseUrl(): String {
        val explicit = prefs.getString(wsKey, null)?.trim()?.removeSuffix("/")
        if (!explicit.isNullOrBlank()) return explicit
        val http = getServerHttpBaseUrl()
        return if (http.startsWith("https://")) {
            http.replaceFirst("https://", "wss://") + "/ws"
        } else {
            http.replaceFirst("http://", "ws://") + "/ws"
        }
    }

    fun setServerWsBaseUrl(value: String) {
        prefs.edit().putString(wsKey, value.trim().removeSuffix("/")).apply()
    }
}
