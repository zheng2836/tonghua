package com.zheng2836.tonghua.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zheng2836.tonghua.AppGraph
import com.zheng2836.tonghua.telecom.PhoneAccountRegistrar

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        setContent {
            MaterialTheme {
                MainScreen(
                    enabled = PhoneAccountRegistrar.isEnabled(this),
                    signalingState = AppGraph.signalingClient.connectionState,
                    iceState = AppGraph.webRtcEngine.iceState,
                    lastSignal = AppGraph.webRtcEngine.lastSignal,
                    onOpenSettings = {
                        PhoneAccountRegistrar.registerIfNeeded(this)
                        PhoneAccountRegistrar.openPhoneAccountSettings(this)
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
private fun MainScreen(
    enabled: Boolean,
    signalingState: String,
    iceState: String,
    lastSignal: String,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "TongHua Managed VoIP", style = MaterialTheme.typography.headlineSmall)
        Text(text = "PhoneAccount enabled: $enabled")
        Text(text = "Signaling state: $signalingState")
        Text(text = "ICE state: $iceState")
        Text(text = "Last WebRTC signal: $lastSignal")
        Button(onClick = onOpenSettings) {
            Text("Open phone account settings")
        }
    }
}
