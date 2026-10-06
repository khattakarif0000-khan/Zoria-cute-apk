package com.example.zoria.integration

/**
 * Architectural integration point for battery-aware background operations
 * and push/local background notifications.
 */
interface BackgroundOperationManager {
    fun scheduleBackgroundNotification(title: String, message: String, delayMillis: Long)
    fun isBatteryOptimizationIgnored(): Boolean
    fun requestIgnoreBatteryOptimization()
}
