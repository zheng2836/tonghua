package com.zheng2836.tonghua.telecom

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager

object TelecomFacade {
    const val EXTRA_CALL_ID = "x_call_id"
    const val EXTRA_PEER_ID = "x_peer_id"
    const val EXTRA_PEER_NAME = "x_peer_name"

    fun placeOutgoingCall(context: Context, callId: String, peerId: String, peerName: String) {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val uri: Uri = PhoneAccountRegistrar.buildPeerUri(peerId)
        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, PhoneAccountRegistrar.phoneAccountHandle(context))
            putString(EXTRA_CALL_ID, callId)
            putString(EXTRA_PEER_ID, peerId)
            putString(EXTRA_PEER_NAME, peerName)
            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
        }
        telecom.placeCall(uri, extras)
    }

    fun addIncomingCall(context: Context, callId: String, callerId: String, callerName: String) {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, PhoneAccountRegistrar.buildPeerUri(callerId))
            putString(EXTRA_CALL_ID, callId)
            putString(EXTRA_PEER_ID, callerId)
            putString(EXTRA_PEER_NAME, callerName)
        }
        telecom.addNewIncomingCall(PhoneAccountRegistrar.phoneAccountHandle(context), extras)
    }
}
