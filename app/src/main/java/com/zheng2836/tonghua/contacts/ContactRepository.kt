package com.zheng2836.tonghua.contacts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ContactRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tonghua_contacts", Context.MODE_PRIVATE)
    private val key = "contacts_json"

    fun loadContacts(): List<VirtualContact> {
        val raw = prefs.getString(key, null)
        if (raw.isNullOrBlank()) {
            val seeded = defaultContacts()
            saveContacts(seeded)
            return seeded
        }
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    VirtualContact(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        virtualNumber = obj.getString("virtualNumber")
                    )
                )
            }
        }
    }

    fun addContact(name: String, virtualNumber: String): List<VirtualContact> {
        val contact = VirtualContact(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            virtualNumber = virtualNumber.trim()
        )
        val updated = loadContacts() + contact
        saveContacts(updated)
        return updated
    }

    fun updateVirtualNumber(contactId: String, virtualNumber: String): List<VirtualContact> {
        val updated = loadContacts().map {
            if (it.id == contactId) it.copy(virtualNumber = virtualNumber.trim()) else it
        }
        saveContacts(updated)
        return updated
    }

    private fun saveContacts(contacts: List<VirtualContact>) {
        val array = JSONArray()
        contacts.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("virtualNumber", it.virtualNumber)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun defaultContacts(): List<VirtualContact> {
        return listOf(
            VirtualContact(UUID.randomUUID().toString(), "妈妈", "mom"),
            VirtualContact(UUID.randomUUID().toString(), "外公", "grandpa"),
            VirtualContact(UUID.randomUUID().toString(), "外婆", "grandma")
        )
    }
}
