// API endpoint: /api/devices (Aapko apne worker mein POST handle karna hoga)
val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
val batteryLevel = getBatteryLevel() // Function to get %
val brand = Build.MANUFACTURER
val model = Build.MODEL

// JSON Body jo aapke Worker ko jayega:
// { "id": "deviceId", "brand": "Vivo", "model": "V2150", "battery": "85" }
