package com.cantrip.focusalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class EscalationService : Service() {

    companion object {
        const val ACTION_ACKNOWLEDGE = "com.cantrip.focusalarm.ACTION_ACKNOWLEDGE"
        const val ACTION_CANCEL = "com.cantrip.focusalarm.ACTION_CANCEL"
        // Extra flag to indicate that only a single beep (alarm1.mp3) should be played.
        const val EXTRA_SINGLE_BEEP = "com.cantrip.focusalarm.EXTRA_SINGLE_BEEP"
    }

    // Each urgency level lasts 20 seconds, with a beep attempted every 3 seconds.
    private val levelDurationMillis = 20_000L
    private val beepIntervalMillis = 3_000L

    // Urgency levels: 1 (lowest) to 4 (highest).
    private var currentLevel = 1
    private val maxLevel = 4

    // Flag to pause beeping for the current cycle if acknowledged.
    @Volatile private var acknowledged = false
    // Flag set when cancellation has been requested.
    @Volatile private var isCancelled = false

    // Handlers and runnables for scheduling beep playback and level escalation.
    private lateinit var beepHandler: Handler
    private lateinit var levelHandler: Handler
    private lateinit var beepRunnable: Runnable
    private lateinit var levelRunnable: Runnable

    // SoundPool for playing short alarm sound effects.
    private lateinit var soundPool: SoundPool
    // Map urgency level to its loaded sound ID.
    private var soundMap = mutableMapOf<Int, Int>()

    // Flag indicating whether sound for level 1 is loaded.
    @Volatile private var sound1Loaded = false

    override fun onCreate() {
        super.onCreate()
        isCancelled = false
        beepHandler = Handler(Looper.getMainLooper())
        levelHandler = Handler(Looper.getMainLooper())

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds for each urgency level.
        soundMap[1] = soundPool.load(this, R.raw.alarm1, 1)
        soundMap[2] = soundPool.load(this, R.raw.alarm2, 1)
        soundMap[3] = soundPool.load(this, R.raw.alarm3, 1)
        soundMap[4] = soundPool.load(this, R.raw.alarm4, 1)

        // Listen for when sound for level 1 is loaded.
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == soundMap[1]) {
                sound1Loaded = true
                Log.d("EscalationService", "Sound for level 1 loaded")
            }
        }

        beepRunnable = object : Runnable {
            override fun run() {
                if (isCancelled) return
                if (!acknowledged) {
                    playBeepForLevel(currentLevel)
                }
                beepHandler.postDelayed(this, beepIntervalMillis)
            }
        }

        levelRunnable = object : Runnable {
            override fun run() {
                if (isCancelled) return
                // Stop any current sounds before escalating.
                stopAllSounds()
                // Reset acknowledged flag for the new cycle.
                acknowledged = false
                // Advance urgency level if not at max; else remain at level 4.
                if (currentLevel < maxLevel) {
                    currentLevel++
                    Log.d("EscalationService", "Escalated to urgency level $currentLevel")
                } else {
                    Log.d("EscalationService", "At maximum urgency level ($maxLevel); maintaining level 4 cycles")
                }
                updateNotification(currentLevel)
                levelHandler.postDelayed(this, levelDurationMillis)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check for the extra flag to play a single beep.
        if (intent?.getBooleanExtra(EXTRA_SINGLE_BEEP, false) == true) {
            startForeground(1, createNotification(1))
            if (!sound1Loaded) {
                Log.d("EscalationService", "Sound for level 1 not ready; delaying single beep.")
                Handler(Looper.getMainLooper()).postDelayed({
                    singleBeepAndCancel()
                }, 300)
            } else {
                singleBeepAndCancel()
            }
            return START_NOT_STICKY
        }

        // Process incoming commands.
        intent?.action?.let { action ->
            when (action) {
                ACTION_ACKNOWLEDGE -> {
                    if (currentLevel == maxLevel) {
                        Log.d("EscalationService", "Alarm acknowledged at level $currentLevel; acting as cancel.")
                        cancelEscalation()
                    } else {
                        acknowledged = true
                        Log.d("EscalationService", "Alarm acknowledged at level $currentLevel; beeping halted until next cycle.")
                    }
                    return START_NOT_STICKY
                }
                ACTION_CANCEL -> {
                    cancelEscalation()
                    return START_NOT_STICKY
                }
            }
        }

        if (isCancelled) return START_NOT_STICKY
        startForeground(1, createNotification(currentLevel))
        beepHandler.post(beepRunnable)
        levelHandler.postDelayed(levelRunnable, levelDurationMillis)
        return START_NOT_STICKY
    }

    // Plays the level-1 sound once and then cancels the service.
    private fun singleBeepAndCancel() {
        val soundId = soundMap[1]
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            Log.d("EscalationService", "Playing single beep (alarm1) for EXTRA_SINGLE_BEEP mode")
        } else {
            Log.e("EscalationService", "Sound for level 1 not loaded for single beep mode.")
        }
        Handler(Looper.getMainLooper()).postDelayed({
            cancelEscalation()
        }, 500)
    }

    // Plays the sound corresponding to the current urgency level using SoundPool.
    private fun playBeepForLevel(level: Int) {
        val soundId = soundMap[level]
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            Log.d("EscalationService", "Playing sound for urgency level $level using SoundPool")
        } else {
            Log.e("EscalationService", "Sound for urgency level $level not loaded.")
        }
    }

    // Uses SoundPool.autoPause() to stop any currently playing sounds.
    private fun stopAllSounds() {
        soundPool.autoPause()
    }

    // Cancels the escalation service so no further sounds are played.
    fun cancelEscalation() {
        isCancelled = true
        beepHandler.removeCallbacksAndMessages(null)
        levelHandler.removeCallbacksAndMessages(null)
        stopAllSounds()
        soundPool.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("EscalationService", "Escalation canceled; alarm sounds stopped.")
    }

    // Builds the foreground notification.
    private fun createNotification(level: Int): Notification {
        val channelId = "escalation_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alarm Escalation", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm Escalation Active")
            .setContentText("Urgency Level: $level")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
    }

    // Updates the active notification with the current urgency level.
    private fun updateNotification(level: Int) {
        val notification = createNotification(level)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        beepHandler.removeCallbacksAndMessages(null)
        levelHandler.removeCallbacksAndMessages(null)
        if (!isCancelled) {
            soundPool.release()
        }
        Log.d("EscalationService", "Service destroyed; all scheduled tasks canceled.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
