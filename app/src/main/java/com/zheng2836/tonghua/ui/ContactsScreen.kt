package com.zheng2836.tonghua.ui

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
import com.zheng2836.tonghua.contacts.VirtualContact

@Composable
fun ContactsScreen(
    myVirtualNumber: String,
    serverHttpBaseUrl: String,
    contacts: List<VirtualContact>,
    phoneAccountEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onEditMyNumber: (String) -> Unit,
    onEditServerHttpBaseUrl: (String) -> Unit,
    onAddContact: (String, String) -> Unit,
    onDial: (VirtualContact) -> Unit,
    onUpdateVirtualNumber: (String, String) -> Unit
) {
    var editingContact by remember { mutableStateOf<VirtualContact?>(null) }
    var editingSelf by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf(false) }
    var addingContact by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                phoneAccountEnabled = phoneAccountEnabled,
                onOpenSettings = onOpenSettings,
                onAddContact = { addingContact = true }
            )
            SelfNumberCard(
                myVirtualNumber = myVirtualNumber,
                onEdit = { editingSelf = true }
            )
            ServerCard(
                serverHttpBaseUrl = serverHttpBaseUrl,
                onEdit = { editingServer = true }
            )
            ContactList(
                contacts = contacts,
                onDial = onDial,
                onLongPress = { editingContact = it }
            )
        }

        editingContact?.let { contact ->
            EditVirtualNumberDialog(
                title = "编辑虚拟号码",
                name = contact.name,
                initialValue = contact.virtualNumber,
                label = "服务器虚拟号码",
                onDismiss = { editingContact = null },
                onSave = { value ->
                    onUpdateVirtualNumber(contact.id, value)
                    editingContact = null
                }
            )
        }

        if (editingSelf) {
            EditVirtualNumberDialog(
                title = "编辑我的号码",
                name = "当前设备",
                initialValue = myVirtualNumber,
                label = "服务器虚拟号码",
                onDismiss = { editingSelf = false },
                onSave = { value ->
                    onEditMyNumber(value)
                    editingSelf = false
                }
            )
        }

        if (editingServer) {
            EditVirtualNumberDialog(
                title = "编辑服务器地址",
                name = "HTTP Base URL",
                initialValue = serverHttpBaseUrl,
                label = "例如 http://joker404.xyz",
                onDismiss = { editingServer = false },
                onSave = { value ->
                    onEditServerHttpBaseUrl(value)
                    editingServer = false
                }
            )
        }

        if (addingContact) {
            AddContactDialog(
                onDismiss = { addingContact = false },
                onSave = { name, number ->
                    onAddContact(name, number)
                    addingContact = false
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    phoneAccountEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onAddContact: () -> Unit
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddContact) {
                Text("新增")
            }
            if (!phoneAccountEnabled) {
                Button(onClick = onOpenSettings) {
                    Text("启用")
                }
            }
        }
    }
}

@Composable
private fun SelfNumberCard(
    myVirtualNumber: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "我的号码",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = myVirtualNumber,
                color = Color(0xFF7E7E7E),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        TextButton(onClick = onEdit) {
            Text("编辑")
        }
    }
}

@Composable
private fun ServerCard(
    serverHttpBaseUrl: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "服务器",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = serverHttpBaseUrl,
                color = Color(0xFF7E7E7E),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        TextButton(onClick = onEdit) {
            Text("编辑")
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    title: String,
    name: String,
    initialValue: String,
    label: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(name, initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = name)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text(label) }
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

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增联系人") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("联系人名称") }
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    singleLine = true,
                    label = { Text("服务器虚拟号码") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), number.trim()) }) {
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
    return if (first.isLetter()) first.uppercase() else "#"
}
