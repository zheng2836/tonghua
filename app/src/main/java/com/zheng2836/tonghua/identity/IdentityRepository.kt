package com.zheng2836.tonghua.identity

import android.content.Context
import java.util.UUID

class IdentityRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tonghua_identity", Context.MODE_PRIVATE)
    private val key = "my_virtual_number"

    fun getMyVirtualNumber(): String {
        val existing = prefs.getString(key, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = "u" + UUID.randomUUID().toString().replace("-", "").take(8)
        prefs.edit().putString(key, generated).apply()
        return generated
    }

    fun setMyVirtualNumber(value: String) {
        prefs.edit().putString(key, value.trim()).apply()
    }
}
