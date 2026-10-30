package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects the classic double "karate chop" gesture (two rapid cutting/downward chopping motions)
 * using the device's accelerometer.
 */
class KarateChopDetector(
    private val context: Context,
    var sensitivity: Sensitivity = Sensitivity.NORMAL,
    private val onChopProgress: ((chopCount: Int, currentForce: Float) -> Unit)? = null,
    private val onDoubleChopDetected: () -> Unit
) : SensorEventListener {

    enum class Sensitivity(val label: String, val threshold: Float) {
        HIGH("High (Easy)", 10.5f),
        NORMAL("Normal (Recommended)", 13.5f),
        LOW("Low (Hard)", 16.5f)
    }

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var isListening = false
    private var firstChopTimestamp = 0L
    private var lastTriggerTimestamp = 0L

    // Low-pass filter component for gravity isolation
    private var gravityX = 0f
    private var gravityY = 9.8f
    private var gravityZ = 0f

    companion object {
        private const val TAG = "KarateChopDetector"
        const val MIN_DELAY_BETWEEN_CHOPS_MS = 180L
        const val MAX_DELAY_BETWEEN_CHOPS_MS = 1400L
        const val COOLDOWN_AFTER_TRIGGER_MS = 1600L
    }

    fun startListening(): Boolean {
        if (isListening || accelerometer == null || sensorManager == null) return false
        isListening = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        Log.d(TAG, "Karate chop detector started: $isListening")
        return isListening
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        firstChopTimestamp = 0L
        Log.d(TAG, "Karate chop detector stopped")
    }

    fun isCurrentlyListening(): Boolean = isListening

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerTimestamp < COOLDOWN_AFTER_TRIGGER_MS) {
            return
        }

        val alpha = 0.8f
        gravityX = alpha * gravityX + (1 - alpha) * event.values[0]
        gravityY = alpha * gravityY + (1 - alpha) * event.values[1]
        gravityZ = alpha * gravityZ + (1 - alpha) * event.values[2]

        // Linear dynamic acceleration without gravity
        val linearX = event.values[0] - gravityX
        val linearY = event.values[1] - gravityY
        val linearZ = event.values[2] - gravityZ

        val dynamicForce = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        // Check if chop expired (> MAX_DELAY_BETWEEN_CHOPS_MS)
        if (firstChopTimestamp > 0L && now - firstChopTimestamp > MAX_DELAY_BETWEEN_CHOPS_MS) {
            firstChopTimestamp = 0L
            onChopProgress?.invoke(0, dynamicForce)
        }

        if (dynamicForce >= sensitivity.threshold) {
            if (firstChopTimestamp == 0L) {
                // First karate chop registered
                firstChopTimestamp = now
                vibrateFeedback(short = true)
                onChopProgress?.invoke(1, dynamicForce)
                Log.d(TAG, "First karate chop registered with force: $dynamicForce")
            } else {
                val diff = now - firstChopTimestamp
                if (diff in MIN_DELAY_BETWEEN_CHOPS_MS..MAX_DELAY_BETWEEN_CHOPS_MS) {
                    // Second karate chop registered -> SUCCESS!
                    firstChopTimestamp = 0L
                    lastTriggerTimestamp = now
                    vibrateFeedback(short = false)
                    onChopProgress?.invoke(2, dynamicForce)
                    Log.d(TAG, "DOUBLE KARATE CHOP CONFIRMED! Triggering app action")
                    onDoubleChopDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Programmatically simulate two karate chops for automated testing or UI emulator demo.
     */
    fun simulateDoubleChop() {
        val now = System.currentTimeMillis()
        lastTriggerTimestamp = now
        firstChopTimestamp = 0L
        vibrateFeedback(short = false)
        onChopProgress?.invoke(2, sensitivity.threshold + 5f)
        onDoubleChopDetected()
    }

    private fun vibrateFeedback(short: Boolean) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (short) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        // Double haptic pulse
                        val timings = longArrayOf(0, 100, 80, 120)
                        val amplitudes = intArrayOf(0, 255, 0, 255)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (short) vibrator.vibrate(40L) else vibrator.vibrate(250L)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic vibration failed: ${e.message}")
        }
    }
}
