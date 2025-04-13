package com.cantrip.focusalarm

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cantrip.focusalarm.AlarmListActivity.AlarmItem
import com.cantrip.focusalarm.AlarmListActivity.Companion.saveAlarms
import com.cantrip.focusalarm.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSetAlarm.setOnClickListener { setAlarm() }
        binding.buttonShowAlarms.setOnClickListener {
            startActivity(Intent(this, AlarmListActivity::class.java))
        }

        binding.buttonTestAlarm1.setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }

        binding.buttonTestAlarm2.setOnClickListener { playAlarm(R.raw.alarm2, "Test Alarm 2") }
        binding.buttonTestAlarm3.setOnClickListener { playAlarm(R.raw.alarm3, "Test Alarm 3") }
        binding.buttonTestAlarm4.setOnClickListener { playAlarm(R.raw.alarm4, "Test Alarm 4") }

        binding.buttonStopAlarm.setOnClickListener { stopAlarmSounds() }

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

        val sharedPrefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val json = sharedPrefs.getString("alarms", null)
        val gson = Gson()
        val type = object : TypeToken<List<AlarmItem>>() {}.type
        val existingAlarms = if (json != null) gson.fromJson<List<AlarmItem>>(json, type) else emptyList()
        val updatedAlarms = existingAlarms + newAlarm

        saveAlarms(this, updatedAlarms)

        val nameInfo = if (label.isNotEmpty()) "Alarm '$label'" else "Alarm"
        binding.textViewAlarmStatus.text = "$nameInfo set for $alarmTime"
    }

    private fun playAlarm(resId: Int, label: String) {
        stopAlarmSounds()
        binding.textViewAlarmStatus.text = "🔊 Playing $label"
        currentPlayer = MediaPlayer.create(this, resId)
        currentPlayer?.setOnCompletionListener {
            it.release()
            currentPlayer = null
            binding.textViewAlarmStatus.text = "✅ $label finished"
        }
        currentPlayer?.start()
    }

    private fun stopAlarmSounds() {
        currentPlayer?.release()
        currentPlayer = null
        binding.textViewAlarmStatus.text = "🔇 Alarm stopped"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSounds()
    }
}
