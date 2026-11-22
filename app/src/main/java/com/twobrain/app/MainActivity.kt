package com.twobrain.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twobrain.app.ui.HomeScreen
import com.twobrain.app.ui.WelcomeScreen
import com.twobrain.app.ui.theme.TwoBrainTheme

class MainActivity : ComponentActivity() {
    private val vm: BrainViewModel by viewModels()
    private var micGranted by mutableStateOf(false)
    private var askedOnce by mutableStateOf(false)

    private val permissions: Array<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_CALENDAR)
        add(Manifest.permission.WRITE_CALENDAR)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_IMAGES)
        else add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }.toTypedArray()

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askedOnce = true
        getPreferences(MODE_PRIVATE).edit().putBoolean("asked", true).apply()
        micGranted = hasMic()
        if (micGranted) vm.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        askedOnce = getPreferences(MODE_PRIVATE).getBoolean("asked", false)
        micGranted = hasMic()
        setContent {
            TwoBrainTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                if (micGranted) HomeScreen(state)
                else WelcomeScreen(micDenied = askedOnce, onContinue = {
                    if (askedOnce && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                    } else request.launch(permissions)
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        micGranted = hasMic()
        if (micGranted) vm.onResume()
    }

    override fun onPause() {
        super.onPause()
        vm.onPause()
    }

    private fun hasMic() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}
