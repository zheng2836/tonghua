package com.zheng2836.tonghua.telecom

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

object PhoneAccountRegistrar {
    private const val ACCOUNT_ID = "managed_voip_account"

    fun phoneAccountHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, ManagedVoipConnectionService::class.java),
            ACCOUNT_ID
        )
    }

    fun registerIfNeeded(context: Context) {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val handle = phoneAccountHandle(context)
        if (telecom.getPhoneAccount(handle) != null) return

        val account = PhoneAccount.builder(handle, "TongHua VoIP")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setShortDescription("Managed VoIP via Telecom")
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .build()

        telecom.registerPhoneAccount(account)
    }

    fun isEnabled(context: Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java)
        return telecom.getPhoneAccount(phoneAccountHandle(context))?.isEnabled == true
    }

    fun openPhoneAccountSettings(context: Context) {
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun buildPeerUri(peerId: String): Uri = Uri.fromParts(PhoneAccount.SCHEME_SIP, peerId, null)
}
