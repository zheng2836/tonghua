package com.zheng2836.tonghua.config

import android.content.Context

class AppConfigRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tonghua_config", Context.MODE_PRIVATE)
    private val httpKey = "server_http_base_url"
    private val wsKey = "server_ws_base_url"
    private val stunKey = "stun_server_url"
    private val turnKey = "turn_server_url"
    private val turnUserKey = "turn_username"
    private val turnPassKey = "turn_password"

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

    fun getStunServerUrl(): String {
        return prefs.getString(stunKey, "stun:stun.l.google.com:19302")!!.trim()
    }

    fun setStunServerUrl(value: String) {
        prefs.edit().putString(stunKey, value.trim()).apply()
    }

    fun getTurnServerUrl(): String {
        return prefs.getString(turnKey, "")!!.trim()
    }

    fun setTurnServerUrl(value: String) {
        prefs.edit().putString(turnKey, value.trim()).apply()
    }

    fun getTurnUsername(): String {
        return prefs.getString(turnUserKey, "")!!.trim()
    }

    fun setTurnUsername(value: String) {
        prefs.edit().putString(turnUserKey, value.trim()).apply()
    }

    fun getTurnPassword(): String {
        return prefs.getString(turnPassKey, "")!!.trim()
    }

    fun setTurnPassword(value: String) {
        prefs.edit().putString(turnPassKey, value.trim()).apply()
    }
}
