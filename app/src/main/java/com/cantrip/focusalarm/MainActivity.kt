package com.cantrip.focusalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cantrip.focusalarm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        binding.buttonSetAlarm.setOnClickListener {
            setAlarm()
        }

        binding.buttonCancelAlarm.setOnClickListener {
            cancelAlarm()
        }

        // Assuming you have a TextView with the ID "textViewAlarmStatus" in your activity_main.xml
        binding.textViewAlarmStatus.text = "Alarm Not Set"
    }

    private fun setAlarm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
            set(Calendar.MINUTE, binding.timePicker.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= Calendar.getInstance().timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            data = Uri.parse("custom://" + System.currentTimeMillis()) // Unique URI for each alarm
            action = "com.cantrip.focusalarm.ALARM_ACTION"
        }

        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        val alarmTimeText = "Alarm set for ${binding.timePicker.hour}:${binding.timePicker.minute}"
        binding.textViewAlarmStatus.text = alarmTimeText
        Toast.makeText(this, alarmTimeText, Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm() {
        alarmManager.cancel(pendingIntent)
        binding.textViewAlarmStatus.text = "Alarm Cancelled"
        Toast.makeText(this, "Alarm Cancelled", Toast.LENGTH_SHORT).show()
    }
}