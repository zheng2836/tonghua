package com.zheng2836.tonghua.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.zheng2836.tonghua.contacts.ContactRepository
import com.zheng2836.tonghua.identity.IdentityRepository
import com.zheng2836.tonghua.telecom.PhoneAccountRegistrar
import com.zheng2836.tonghua.telecom.TelecomFacade
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        setContent {
            val contactRepository = remember { ContactRepository(this) }
            val identityRepository = remember { IdentityRepository(this) }
            var contacts by remember { mutableStateOf(contactRepository.loadContacts()) }
            var myVirtualNumber by remember { mutableStateOf(identityRepository.getMyVirtualNumber()) }

            MaterialTheme {
                ContactsScreen(
                    myVirtualNumber = myVirtualNumber,
                    contacts = contacts,
                    phoneAccountEnabled = PhoneAccountRegistrar.isEnabled(this),
                    onOpenSettings = {
                        PhoneAccountRegistrar.registerIfNeeded(this)
                        PhoneAccountRegistrar.openPhoneAccountSettings(this)
                    },
                    onEditMyNumber = { value ->
                        identityRepository.setMyVirtualNumber(value)
                        myVirtualNumber = identityRepository.getMyVirtualNumber()
                    },
                    onAddContact = { name, number ->
                        contacts = contactRepository.addContact(name, number)
                    },
                    onDial = { contact ->
                        TelecomFacade.placeOutgoingCall(
                            context = this,
                            callId = UUID.randomUUID().toString(),
                            peerId = contact.virtualNumber,
                            peerName = contact.name
                        )
                    },
                    onUpdateVirtualNumber = { contactId, virtualNumber ->
                        contacts = contactRepository.updateVirtualNumber(contactId, virtualNumber)
                    }
                )
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.RECORD_AUDIO
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.CALL_PHONE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
