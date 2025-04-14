package com.cantrip.focusalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cantrip.focusalarm.AlarmListActivity.AlarmItem
import com.cantrip.focusalarm.AlarmListActivity.Companion.saveAlarms
import com.cantrip.focusalarm.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // SoundPool for test alarm playback.
    private lateinit var soundPool: SoundPool
    private var soundIdAlarm1: Int = 0
    private var soundIdAlarm2: Int = 0

    // Handler for debouncing button clicks.
    private val debounceHandler = Handler(Looper.getMainLooper())
    private val debounceDelay = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if an alarm is currently active. If so, immediately launch AlarmActivity.
        val prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("activeAlarm", false)) {
            val alarmIntent = Intent(this, AlarmActivity::class.java)
            alarmIntent.putExtra(AlarmActivity.EXTRA_USE_ESCALATION, true)
            startActivity(alarmIntent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SoundPool for playing test sounds.
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
            // Launch AlarmListActivity if desired.
            startActivity(Intent(this, AlarmListActivity::class.java))
        }

        // Test Alarm 1: Launch AlarmActivity in normal mode.
        binding.buttonTestAlarm1.setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }

        // Test Alarm 2: Launch AlarmActivity in escalation mode and start escalation service.
        binding.buttonTestAlarm2.setOnClickListener {
            val intent = Intent(this, AlarmActivity::class.java)
            intent.putExtra(AlarmActivity.EXTRA_USE_ESCALATION, true)
            startActivity(intent)
            val escalationIntent = Intent(this, EscalationService::class.java)
            startForegroundService(escalationIntent)
        }

        // Test Alarm 3: Play alarm1.mp3 once using SoundPool.
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
            binding.layoutDays.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.textViewAlarmStatus.text = "Alarm status: Not set"
    }

    private fun setAlarm() {
        // Get alarm details from UI.
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val alarmTime = "%02d:%02d".format(hour, minute)
        val label = binding.editTextAlarmName.text.toString().take(20).trim()

        // Create a new alarm item.
        val newAlarm = AlarmItem(time = alarmTime, days = listOf(), enabled = true, label = label)
        // Load any existing alarms.
        val sharedPrefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val json = sharedPrefs.getString("alarms", null)
        val gson = Gson()
        val type = object : TypeToken<List<AlarmItem>>() {}.type
        val existingAlarms = if (json != null) gson.fromJson<List<AlarmItem>>(json, type) else emptyList()
        // Save new alarm list.
        val updatedAlarms = existingAlarms + newAlarm
        saveAlarms(this, updatedAlarms)
        binding.textViewAlarmStatus.text = "Alarm '$label' set for $alarmTime"

        // Now schedule this alarm immediately.
        // Create a unique alarm ID, for example by using the hashCode of (label + alarmTime).
        val alarmId = (label + alarmTime).hashCode()
        val triggerTime = getTriggerTime(alarmTime)
        scheduleAlarm(this, triggerTime, alarmId, label)
    }

    // Converts a time string ("HH:mm") to the timestamp (in ms) for the next occurrence minus 15 minutes.
    private fun getTriggerTime(timeString: String): Long {
        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return System.currentTimeMillis()
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            calendar.add(Calendar.MINUTE, -15)
            return calendar.timeInMillis
        } catch (e: Exception) {
            e.printStackTrace()
            return System.currentTimeMillis()
        }
    }

    // Schedules an alarm via AlarmManager.
    private fun scheduleAlarm(context: Context, triggerTime: Long, alarmId: Int, alarmLabel: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // For Android 12+ (API level S), check permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            showExactAlarmPermissionPrompt()
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.ALARM_ID_EXTRA, alarmId)
            putExtra(AlarmReceiver.ALARM_NAME_EXTRA, alarmLabel)
            putExtra(AlarmReceiver.IS_ONE_OFF_EXTRA, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    // Prompts the user to grant the SCHEDULE_EXACT_ALARM permission.
    private fun showExactAlarmPermissionPrompt() {
        AlertDialog.Builder(this)
            .setTitle("Exact Alarms Permission Required")
            .setMessage("This app needs permission to schedule exact alarms. Please grant this in the system settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                val settingsIntent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
                settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(settingsIntent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }
}
