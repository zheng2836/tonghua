package com.zheng2836.tonghua.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zheng2836.tonghua.contacts.ContactRepository
import com.zheng2836.tonghua.contacts.VirtualContact
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
            val repository = remember { ContactRepository(this) }
            var contacts by remember { mutableStateOf(repository.loadContacts()) }

            MaterialTheme {
                ContactsScreen(
                    contacts = contacts,
                    phoneAccountEnabled = PhoneAccountRegistrar.isEnabled(this),
                    onOpenSettings = {
                        PhoneAccountRegistrar.registerIfNeeded(this)
                        PhoneAccountRegistrar.openPhoneAccountSettings(this)
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
                        contacts = repository.updateVirtualNumber(contactId, virtualNumber)
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

@Composable
private fun ContactsScreen(
    contacts: List<VirtualContact>,
    phoneAccountEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onDial: (VirtualContact) -> Unit,
    onUpdateVirtualNumber: (String, String) -> Unit
) {
    var editingContact by remember { mutableStateOf<VirtualContact?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(phoneAccountEnabled = phoneAccountEnabled, onOpenSettings = onOpenSettings)
            ContactList(
                contacts = contacts,
                onDial = onDial,
                onLongPress = { editingContact = it }
            )
        }

        editingContact?.let { contact ->
            EditVirtualNumberDialog(
                contact = contact,
                onDismiss = { editingContact = null },
                onSave = { value ->
                    onUpdateVirtualNumber(contact.id, value)
                    editingContact = null
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    phoneAccountEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "联系人",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        if (!phoneAccountEnabled) {
            Button(onClick = onOpenSettings) {
                Text("启用")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactList(
    contacts: List<VirtualContact>,
    onDial: (VirtualContact) -> Unit,
    onLongPress: (VirtualContact) -> Unit
) {
    val grouped = contacts
        .sortedBy { it.name }
        .groupBy { firstKey(it.name) }
        .toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        grouped.forEach { (key, itemsInGroup) ->
            item(key = "header_$key") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101010))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = key,
                        color = Color(0xFF8A8A8A),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            items(itemsInGroup, key = { it.id }) { contact ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onDial(contact) },
                            onLongClick = { onLongPress(contact) }
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = contact.virtualNumber,
                        color = Color(0xFF7E7E7E),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditVirtualNumberDialog(
    contact: VirtualContact,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(contact.id) { mutableStateOf(contact.virtualNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑虚拟号码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = contact.name)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text("服务器虚拟号码") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun firstKey(name: String): String {
    val first = name.firstOrNull() ?: return "#"
    return if (first.isLetter()) {
        first.uppercase()
    } else {
        "#"
    }
}
