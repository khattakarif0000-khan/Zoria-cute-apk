package com.example.zoria.integration

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PhoneActionResult {
    data class Success(val messageUrdu: String, val messageEnglish: String) : PhoneActionResult()
    data class Failure(val reasonUrdu: String, val reasonEnglish: String) : PhoneActionResult()
    data class HandledText(val responseText: String) : PhoneActionResult()
    object NotACommand : PhoneActionResult()
}

class PhoneAppController(private val context: Context) {

    companion object {
        private const val TAG = "PhoneAppController"
        const val PKG_WHATSAPP = "com.whatsapp"
        const val PKG_YOUTUBE = "com.google.android.youtube"
        const val PKG_TIKTOK_GLOBAL = "com.zhiliaoapp.musically"
        const val PKG_TIKTOK_ASIA = "com.ss.android.ugc.trill"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var isTorchOn = false

    /**
     * Checks if the user's input is a phone action command (e.g., "WhatsApp kholo", "camera kholo", etc.)
     * If so, executes it via genuine Android system APIs and returns the result.
     */
    fun evaluateAndExecuteCommand(input: String): PhoneActionResult {
        val lower = input.trim().lowercase()

        // 1. WhatsApp Actions
        if (containsAny(lower, "whatsapp kholo", "واٹس ایپ کھولو", "open whatsapp", "واتس اب")) {
            return openApp(PKG_WHATSAPP, "WhatsApp", "واٹس ایپ")
        }
        if (containsAny(lower, "whatsapp search", "whatsapp par", "واٹس ایپ پر میسج")) {
            return openWhatsAppChat(null)
        }

        // 2. Camera Actions
        if (containsAny(lower, "camera kholo", "کیمرہ کھولو", "کیمرا کھولو", "open camera", "take picture")) {
            return openCamera()
        }

        // 3. Gallery / Photos Actions
        if (containsAny(lower, "gallery kholo", "گیلری کھولو", "تصاویر کھولو", "open gallery", "photos kholo")) {
            return openGallery()
        }

        // 4. Settings Actions
        if (containsAny(lower, "settings kholo", "سیٹنگز کھولو", "سیٹنگ کھولو", "open settings")) {
            return openSettings()
        }

        // 5. YouTube Actions
        if (containsAny(lower, "youtube kholo", "یوٹیوب کھولو", "open youtube")) {
            return openApp(PKG_YOUTUBE, "YouTube", "یوٹیوب")
        }
        if (lower.contains("youtube") && containsAny(lower, "search", "چلاؤ", "سرچ کرو", "dhundo")) {
            val query = extractQuery(lower, "youtube")
            return searchYouTube(query)
        }

        // 6. TikTok Actions
        if (containsAny(lower, "tiktok kholo", "ٹک ٹاک کھولو", "open tiktok")) {
            return openTikTok()
        }

        // 7. Flashlight / Torch
        if (containsAny(lower, "torch jalao", "torch on", "flashlight on", "ٹارچ آن", "ٹارچ جلاؤ", "batti jalao")) {
            return setFlashlight(true)
        }
        if (containsAny(lower, "torch band", "torch off", "flashlight off", "ٹارچ بند", "ٹارچ بجھاؤ")) {
            return setFlashlight(false)
        }

        // 8. Volume Controls
        if (containsAny(lower, "volume badhao", "volume up", "awaz badhao", "آواز بڑھاؤ", "آواز زیادہ کرو")) {
            return adjustVolume(AudioManager.ADJUST_RAISE)
        }
        if (containsAny(lower, "volume kam", "volume down", "awaz kam", "آواز کم کرو", "آواز ہلکی کرو")) {
            return adjustVolume(AudioManager.ADJUST_LOWER)
        }

        // 9. Time & Date Query
        if (containsAny(lower, "time kya", "waqt kya", "وقت کیا ہے", "ٹائم کیا ہے", "what time", "current time")) {
            return getCurrentTime()
        }
        if (containsAny(lower, "aaj kya date", "tareekh kya", "تاریخ کیا ہے", "today date", "aaj konsa din")) {
            return getCurrentDate()
        }

        // 10. Battery Query
        if (containsAny(lower, "battery kitni", "battery status", "بیٹری کتنی ہے", "battery percentage", "charging")) {
            return getBatteryStatus()
        }

        // 11. Alarm Set Intent
        if (containsAny(lower, "alarm lagao", "alarm set", "الارم لگاؤ", "wake me up")) {
            return openAlarmSetter()
        }

        // 12. Web Search
        if (lower.startsWith("google") || lower.startsWith("search karo") || lower.startsWith("سرچ کرو")) {
            val query = extractQuery(lower, "google", "search karo", "سرچ کرو")
            return searchWeb(query)
        }

        return PhoneActionResult.NotACommand
    }

    private fun openApp(packageName: String, appNameEng: String, appNameUrdu: String): PhoneActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                PhoneActionResult.Success(
                    "جی Boss، $appNameUrdu کھول رہی ہوں۔",
                    "Opening $appNameEng for you, Boss."
                )
            } else {
                PhoneActionResult.Failure(
                    "معذرت Boss، $appNameUrdu آپ کے فون میں انسٹال نہیں ہے۔",
                    "$appNameEng is not installed on this device."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $packageName", e)
            PhoneActionResult.Failure(
                "$appNameUrdu کھولنے میں دشواری پیش آئی۔",
                "Could not open $appNameEng: ${e.message}"
            )
        }
    }

    fun openWhatsAppChat(phoneNumber: String?): PhoneActionResult {
        return try {
            val pm = context.packageManager
            val isInstalled = isPackageInstalled(PKG_WHATSAPP, pm)
            if (!isInstalled) {
                return PhoneActionResult.Failure(
                    "Boss، واٹس ایپ آپ کے فون میں موجود نہیں ہے۔",
                    "WhatsApp is not installed on this device."
                )
            }

            val uri = if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
            } else {
                Uri.parse("whatsapp://send")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(PKG_WHATSAPP)
            }
            context.startActivity(intent)
            PhoneActionResult.Success("جی Boss، واٹس ایپ چیٹ کھول رہی ہوں۔", "Opening WhatsApp chat.")
        } catch (e: Exception) {
            // Fallback to opening WhatsApp normally
            openApp(PKG_WHATSAPP, "WhatsApp", "واٹس ایپ")
        }
    }

    private fun openCamera(): PhoneActionResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneActionResult.Success("کیمرہ کھول رہی ہوں Boss۔", "Opening Camera, Boss.")
        } catch (e: Exception) {
            PhoneActionResult.Failure("کیمرہ نہیں کھل سکا۔", "Failed to open Camera: ${e.message}")
        }
    }

    private fun openGallery(): PhoneActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneActionResult.Success("گیلری کھول رہی ہوں Boss۔", "Opening Gallery, Boss.")
        } catch (e: Exception) {
            PhoneActionResult.Failure("گیلری کھولنے میں مسئلہ ہوا۔", "Failed to open Gallery: ${e.message}")
        }
    }

    private fun openSettings(): PhoneActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneActionResult.Success("فون سیٹنگز کھول دی گئی ہیں Boss۔", "Opening phone Settings.")
        } catch (e: Exception) {
            PhoneActionResult.Failure("سیٹنگز نہیں کھل سکیں۔", "Failed to open Settings: ${e.message}")
        }
    }

    fun searchYouTube(query: String): PhoneActionResult {
        return try {
            val cleanQuery = query.ifBlank { "trending" }
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage(PKG_YOUTUBE)
                putExtra("query", cleanQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneActionResult.Success(
                "یوٹیوب پر '$cleanQuery' تلاش کر رہی ہوں Boss۔",
                "Searching YouTube for '$cleanQuery'."
            )
        } catch (e: Exception) {
            // Fallback to browser YouTube
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                PhoneActionResult.Success("یوٹیوب ویب پر سرچ کر رہی ہوں۔", "Searching YouTube on web.")
            } catch (err: Exception) {
                PhoneActionResult.Failure("یوٹیوب سرچ نہیں ہو سکی۔", "Failed to search YouTube.")
            }
        }
    }

    fun openTikTok(): PhoneActionResult {
        val pm = context.packageManager
        if (isPackageInstalled(PKG_TIKTOK_GLOBAL, pm)) {
            return openApp(PKG_TIKTOK_GLOBAL, "TikTok", "ٹک ٹاک")
        }
        if (isPackageInstalled(PKG_TIKTOK_ASIA, pm)) {
            return openApp(PKG_TIKTOK_ASIA, "TikTok", "ٹک ٹاک")
        }
        // Fallback to Web TikTok
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            PhoneActionResult.Success("ٹک ٹاک براؤزر میں کھول رہی ہوں۔", "Opening TikTok in browser.")
        } catch (e: Exception) {
            PhoneActionResult.Failure("ٹک ٹاک ایپ انسٹال نہیں ہے۔", "TikTok is not installed.")
        }
    }

    fun setFlashlight(turnOn: Boolean): PhoneActionResult {
        if (cameraManager == null) {
            return PhoneActionResult.Failure("کیمرہ فلیش لائٹ دستیاب نہیں ہے۔", "Flashlight hardware not available.")
        }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                isTorchOn = turnOn
                val statusUrdu = if (turnOn) "ٹارچ آن کر دی گئی ہے Boss۔ 🔦" else "ٹارچ بند کر دی گئی ہے Boss۔"
                val statusEng = if (turnOn) "Flashlight turned on, Boss." else "Flashlight turned off, Boss."
                PhoneActionResult.Success(statusUrdu, statusEng)
            } else {
                PhoneActionResult.Failure("کیمرہ شناختی نمبر نہیں ملا۔", "No Camera ID found for torch.")
            }
        } catch (e: Exception) {
            PhoneActionResult.Failure("ٹارچ کنٹرول کرنے میں دشواری: ${e.message}", "Flashlight error: ${e.message}")
        }
    }

    private fun adjustVolume(direction: Int): PhoneActionResult {
        if (audioManager == null) {
            return PhoneActionResult.Failure("آڈیو سروس دستیاب نہیں ہے۔", "Audio service unavailable.")
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val msgUrdu = if (direction == AudioManager.ADJUST_RAISE) "آواز بڑھا دی گئی ہے Boss۔ 🔊" else "آواز کم کر دی گئی ہے Boss۔ 🔉"
        val msgEng = if (direction == AudioManager.ADJUST_RAISE) "Volume increased, Boss." else "Volume decreased, Boss."
        return PhoneActionResult.Success(msgUrdu, msgEng)
    }

    fun getCurrentTime(): PhoneActionResult {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formatted = sdf.format(Date())
        val text = "Boss، اس وقت کا ٹائم $formatted ہے۔"
        return PhoneActionResult.HandledText(text)
    }

    fun getCurrentDate(): PhoneActionResult {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val formatted = sdf.format(Date())
        val text = "Boss، آج کا دن اور تاریخ ہے: $formatted۔"
        return PhoneActionResult.HandledText(text)
    }

    fun getBatteryStatus(): PhoneActionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (level >= 0) {
            PhoneActionResult.HandledText("Boss، آپ کے فون کی بیٹری اس وقت $level% ہے۔ 🔋")
        } else {
            PhoneActionResult.HandledText("معذرت Boss، بیٹری کی معلومات حاصل نہیں ہو سکیں۔")
        }
    }

    private fun openAlarmSetter(): PhoneActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            context.startActivity(intent)
            PhoneActionResult.Success("الارم سیٹنگ کھول رہی ہوں Boss۔", "Opening Alarm settings.")
        } catch (e: Exception) {
            PhoneActionResult.Failure("الارم ایپ نہیں کھل سکی۔", "Failed to open Alarm app.")
        }
    }

    private fun searchWeb(query: String): PhoneActionResult {
        val clean = query.ifBlank { "Google" }
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", clean)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneActionResult.Success("گوگل پر '$clean' تلاش کر رہی ہوں Boss۔", "Searching Google for '$clean'.")
        } catch (e: Exception) {
            PhoneActionResult.Failure("ویب سرچ نہیں کھل سکی۔", "Web search failed: ${e.message}")
        }
    }

    private fun isPackageInstalled(packageName: String, pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun containsAny(text: String, vararg targets: String): Boolean {
        return targets.any { text.contains(it) }
    }

    private fun extractQuery(text: String, vararg keywords: String): String {
        var result = text
        for (kw in keywords) {
            result = result.replace(kw, "")
        }
        return result.replace("search", "")
            .replace("par", "")
            .replace("karo", "")
            .replace("chalao", "")
            .replace("سرچ", "")
            .replace("کرو", "")
            .trim()
    }
}
