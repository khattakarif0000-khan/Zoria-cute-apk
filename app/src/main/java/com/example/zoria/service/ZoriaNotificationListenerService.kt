package com.example.zoria.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ZoriaNotificationEvent(
    val appName: String,
    val packageName: String,
    val title: String,
    val announcementUrdu: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ZoriaNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "ZoriaNotificationSvc"

        private val _notificationEvents = MutableSharedFlow<ZoriaNotificationEvent>(extraBufferCapacity = 10)
        val notificationEvents: SharedFlow<ZoriaNotificationEvent> = _notificationEvents.asSharedFlow()

        /**
         * Checks if the user has granted Android Notification Access permission to ZORIA.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val myComponent = ComponentName(context, ZoriaNotificationListenerService::class.java).flattenToString()
            return enabledListeners.contains(myComponent)
        }

        /**
         * Returns an intent to navigate directly to Android's Notification Access settings.
         */
        fun getNotificationSettingsIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        // Do not listen to own app notifications
        if (pkg == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Privacy Filter: strictly exclude OTPs, banking, and passwords
        if (isSensitiveContent(title, text)) {
            Log.d(TAG, "Suppressed sensitive notification for privacy.")
            return
        }

        val appFriendlyName = when (pkg) {
            "com.whatsapp" -> "WhatsApp (واٹس ایپ)"
            "com.google.android.apps.messaging", "com.android.mms" -> "پیغامات (Messages)"
            "com.google.android.gm" -> "جی میل (Gmail)"
            "com.google.android.youtube" -> "یوٹیوب (YouTube)"
            "com.instagram.android" -> "انسٹاگرام (Instagram)"
            else -> null
        }

        // Only announce recognized interactive apps to avoid spamming the user
        if (appFriendlyName != null) {
            val announcement = if (title.isNotBlank()) {
                "Boss، $appFriendlyName پر $title کی طرف سے ایک نیا notification آیا ہے۔"
            } else {
                "Boss، $appFriendlyName پر ایک نیا notification آیا ہے۔"
            }

            val event = ZoriaNotificationEvent(
                appName = appFriendlyName,
                packageName = pkg,
                title = title,
                announcementUrdu = announcement
            )
            _notificationEvents.tryEmit(event)
        }
    }

    private fun isSensitiveContent(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        val sensitiveKeywords = listOf(
            "otp", "code", "pin", "verification", "one time password",
            "password", "cvv", "bank", "hbl", "ubl", "mcb", "meezan",
            "jazzcash", "easypaisa", "debit", "credit", "transferred", "rs."
        )
        return sensitiveKeywords.any { combined.contains(it) }
    }
}
