package com.zheng2836.tonghua.telecom

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

object PhoneAccountRegistrar {
    private const val ACCOUNT_ID = "managed_voip_account"

    fun phoneAccountHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, ManagedVoipConnectionService::class.java),
            ACCOUNT_ID
        )
    }

    fun registerIfNeeded(context: Context): Boolean {
        val telecom = telecomManagerOrNull(context) ?: return false
        val handle = phoneAccountHandle(context)

        val existing = if (canInspectPhoneAccount(context)) {
            runCatching { telecom.getPhoneAccount(handle) }.getOrNull()
        } else {
            null
        }
        if (existing != null) return true

        val account = PhoneAccount.builder(handle, "TongHua VoIP")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setShortDescription("Managed VoIP via Telecom")
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .build()

        return runCatching {
            telecom.registerPhoneAccount(account)
            true
        }.getOrDefault(false)
    }

    fun isEnabled(context: Context): Boolean {
        if (!canInspectPhoneAccount(context)) return false
        val telecom = telecomManagerOrNull(context) ?: return false
        return runCatching {
            telecom.getPhoneAccount(phoneAccountHandle(context))?.isEnabled == true
        }.getOrDefault(false)
    }

    fun openPhoneAccountSettings(context: Context): Boolean {
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun buildPeerUri(peerId: String): Uri = Uri.fromParts(PhoneAccount.SCHEME_SIP, peerId, null)

    private fun telecomManagerOrNull(context: Context): TelecomManager? {
        return runCatching { context.getSystemService(TelecomManager::class.java) }.getOrNull()
    }

    private fun canInspectPhoneAccount(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val hasReadPhoneNumbers = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED
        val hasReadPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return hasReadPhoneNumbers || hasReadPhoneState
    }
}
