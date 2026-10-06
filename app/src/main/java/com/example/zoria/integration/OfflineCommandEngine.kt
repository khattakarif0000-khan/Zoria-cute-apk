package com.example.zoria.integration

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class OfflineCommandEngine(
    private val context: Context,
    private val phoneAppController: PhoneAppController
) {

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Executes local offline commands (time, date, battery, flashlight, volume, apps)
     * when device is disconnected from internet.
     */
    fun processOffline(input: String): String {
        val actionResult = phoneAppController.evaluateAndExecuteCommand(input)
        return when (actionResult) {
            is PhoneActionResult.HandledText -> actionResult.responseText
            is PhoneActionResult.Success -> actionResult.messageUrdu
            is PhoneActionResult.Failure -> actionResult.reasonUrdu
            PhoneActionResult.NotACommand -> {
                "معذرت Boss، اس وقت انٹرنیٹ کنکشن دستیاب نہیں ہے۔ انٹرنیٹ کے بغیر میں ٹائم، تاریخ، بیٹری، ٹارچ، یا ایپس کھول سکتی ہوں۔ برائے مہربانی انٹرنیٹ بحال کریں۔"
            }
        }
    }
}
