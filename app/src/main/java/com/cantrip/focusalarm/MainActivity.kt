package com.cantrip.focusalarm

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cantrip.focusalarm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Main alarm control buttons
        binding.buttonSetAlarm.setOnClickListener { setAlarm() }

        // 🚀 Launch alarm list screen
        binding.buttonShowAlarms.setOnClickListener {
            val intent = Intent(this, AlarmListActivity::class.java)
            startActivity(intent)
        }

        // Test alarm buttons
        binding.buttonTestAlarm1.setOnClickListener {
            val intent = Intent(this, AlarmActivity::class.java)
            startActivity(intent)
        }
        binding.buttonTestAlarm2.setOnClickListener { playAlarm(R.raw.alarm2, "Test Alarm 2") }
        binding.buttonTestAlarm3.setOnClickListener { playAlarm(R.raw.alarm3, "Test Alarm 3") }
        binding.buttonTestAlarm4.setOnClickListener { playAlarm(R.raw.alarm4, "Test Alarm 4") }

        // Stop button
        binding.buttonStopAlarm.setOnClickListener { stopAlarmSounds() }

        // Repeat toggle visibility
        binding.switchRepeat.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDays.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.textViewAlarmStatus.text = "Alarm status: Not set"
    }

    private fun setAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute

        val alarmTime = "%02d:%02d".format(hour, minute)
        val startMinute = (minute - 15 + 60) % 60
        val startHour = if (minute < 15) (hour - 1 + 24) % 24 else hour
        val startTime = "%02d:%02d".format(startHour, startMinute)

        val label = binding.editTextAlarmName.text.toString().take(20).trim()

        val nameInfo = if (label.isNotEmpty()) "Alarm '$label'" else "Alarm"
        binding.textViewAlarmStatus.text = "$nameInfo set for $alarmTime – Notifications will start from $startTime"
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
