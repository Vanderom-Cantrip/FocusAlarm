package com.cantrip.focusalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        // Constants should be in ALL_CAPS with underscores for multiple words
        const val ALARM_LEVEL_EXTRA = "ALARM_LEVEL_EXTRA"
        const val ALARM_NAME_EXTRA = "ALARM_NAME_EXTRA"
        const val IS_ONE_OFF_EXTRA = "IS_ONE_OFF_EXTRA"
        const val ALARM_ID_EXTRA = "ALARM_ID_EXTRA"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")

        // Use the constants from the companion object
        val alarmLevel = intent.getIntExtra(ALARM_LEVEL_EXTRA, 0)
        val alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "Unknown Alarm"
        val isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, 0)

        Toast.makeText(context, "Alarm! Level: $alarmLevel, Name: $alarmName", Toast.LENGTH_LONG).show()

        val activityIntent = Intent(context, AlarmActivity::class.java)
        // Use the constants from THIS class (AlarmReceiver)
        activityIntent.putExtra(ALARM_LEVEL_EXTRA, alarmLevel)
        activityIntent.putExtra(ALARM_NAME_EXTRA, alarmName)
        activityIntent.putExtra(IS_ONE_OFF_EXTRA, isOneOff)
        activityIntent.putExtra(ALARM_ID_EXTRA, alarmId)
        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(context, activityIntent, null)

        if (isOneOff) {
            cancelAlarm(context, alarmId)
        }
    }

    private fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmReceiver", "One-off alarm canceled")
    }
}