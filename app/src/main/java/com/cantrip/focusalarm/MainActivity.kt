package com.cantrip.focusalarm

import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.cantrip.focusalarm.AlarmListActivity.AlarmItem
import com.cantrip.focusalarm.AlarmListActivity.Companion.saveAlarms
import com.cantrip.focusalarm.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Constants for alarm active flag and its timestamp, and the timeout duration (15 minutes)
private const val PREF_ACTIVE_ALARM = "activeAlarm"
private const val PREF_ALARM_TIMESTAMP = "activeAlarmTimestamp"
private const val ALARM_TIMEOUT = 15 * 60 * 1000L  // 15 minutes in milliseconds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // SoundPool and sound IDs for test alarm playback.
    private lateinit var soundPool: SoundPool
    private var soundIdAlarm1: Int = 0
    private var soundIdAlarm2: Int = 0

    // Handler for debouncing (if necessary) for any button clicks.
    private val debounceHandler = Handler(Looper.getMainLooper())
    private val debounceDelay = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if an alarm is active and if the timestamp is recent (within 15 minutes).
        val prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val isAlarmActive = prefs.getBoolean(PREF_ACTIVE_ALARM, false)
        val alarmTimestamp = prefs.getLong(PREF_ALARM_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        if (isAlarmActive && (currentTime - alarmTimestamp < ALARM_TIMEOUT)) {
            // An alarm is active and recent, so immediately redirect to AlarmActivity in escalation mode.
            val alarmIntent = Intent(this, AlarmActivity::class.java)
            alarmIntent.putExtra(AlarmActivity.EXTRA_USE_ESCALATION, true)
            startActivity(alarmIntent)
            finish()  // Close MainActivity so that the user sees the alarm screen.
            return
        } else if (isAlarmActive) {
            // The alarm flag is stale—clear it.
            prefs.edit().remove(PREF_ACTIVE_ALARM).remove(PREF_ALARM_TIMESTAMP).apply()
        }

        // Normal setup
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SoundPool for short sound effects.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload sounds: alarm1.mp3 and alarm2.mp3.
        soundIdAlarm1 = soundPool.load(this, R.raw.alarm1, 1)
        soundIdAlarm2 = soundPool.load(this, R.raw.alarm2, 1)

        binding.buttonSetAlarm.setOnClickListener { setAlarm() }
        binding.buttonShowAlarms.setOnClickListener {
            startActivity(Intent(this, AlarmListActivity::class.java))
        }

        // Test Alarm 1: Launch AlarmActivity in normal mode.
        binding.buttonTestAlarm1.setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }

        // Test Alarm 2: Launch AlarmActivity in escalation mode and start the escalation service.
        binding.buttonTestAlarm2.setOnClickListener {
            val intent = Intent(this, AlarmActivity::class.java)
            // Extra flag to indicate AlarmActivity should not play a looping sound.
            intent.putExtra(AlarmActivity.EXTRA_USE_ESCALATION, true)
            startActivity(intent)
            // Start the escalation service (regular escalation mode).
            val escalationIntent = Intent(this, EscalationService::class.java)
            startForegroundService(escalationIntent)
        }

        // Test Alarm 3: Play alarm1.mp3 once directly using SoundPool.
        binding.buttonTestAlarm3.setOnClickListener {
            binding.textViewAlarmStatus.text = "🔊 Playing Test Alarm 3"
            soundPool.play(soundIdAlarm1, 1.0f, 1.0f, 1, 0, 1.0f)
            binding.textViewAlarmStatus.text = "✅ Test Alarm 3 finished"
        }

        // Test Alarm 4: Start EscalationService in single-beep mode.
        binding.buttonTestAlarm4.setOnClickListener {
            binding.textViewAlarmStatus.text = "🔊 Playing Test Alarm 4 via EscalationService"
            val intent = Intent(this, EscalationService::class.java)
            intent.putExtra(EscalationService.EXTRA_SINGLE_BEEP, true)
            startForegroundService(intent)
        }

        binding.buttonStopAlarm.setOnClickListener {
            binding.textViewAlarmStatus.text = "🔇 Alarm stopped"
        }

        binding.switchRepeat.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDays.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }
        binding.textViewAlarmStatus.text = "Alarm status: Not set"
    }

    private fun setAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val alarmTime = "%02d:%02d".format(hour, minute)
        val label = binding.editTextAlarmName.text.toString().take(20).trim()

        val newAlarm = AlarmItem(time = alarmTime, days = listOf(), enabled = true, label = label)
        val prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val json = prefs.getString("alarms", null)
        val gson = Gson()
        val type = object : TypeToken<List<AlarmItem>>() {}.type
        val existingAlarms = if (json != null) gson.fromJson<List<AlarmItem>>(json, type) else emptyList()
        val updatedAlarms = existingAlarms + newAlarm
        saveAlarms(this, updatedAlarms)

        val nameInfo = if (label.isNotEmpty()) "Alarm '$label'" else "Alarm"
        binding.textViewAlarmStatus.text = "$nameInfo set for $alarmTime"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}
