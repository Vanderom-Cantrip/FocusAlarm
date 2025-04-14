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
        // Constants used to pass information about an alarm.
        const val ALARM_LEVEL_EXTRA = "ALARM_LEVEL_EXTRA"
        const val ALARM_NAME_EXTRA = "ALARM_NAME_EXTRA"
        const val IS_ONE_OFF_EXTRA = "IS_ONE_OFF_EXTRA"
        const val ALARM_ID_EXTRA = "ALARM_ID_EXTRA"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")

        // Read alarm extras from the Intent.
        val alarmLevel = intent.getIntExtra(ALARM_LEVEL_EXTRA, 0)
        val alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "Unknown Alarm"
        val isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, 0)

        Toast.makeText(
            context,
            "Alarm Triggered! Level: $alarmLevel, Name: $alarmName",
            Toast.LENGTH_LONG
        ).show()

        // Create an intent to launch AlarmActivity in escalation mode.
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(ALARM_LEVEL_EXTRA, alarmLevel)
            putExtra(ALARM_NAME_EXTRA, alarmName)
            putExtra(IS_ONE_OFF_EXTRA, isOneOff)
            putExtra(ALARM_ID_EXTRA, alarmId)
            // This flag tells AlarmActivity not to start its local looping sound.
            putExtra(AlarmActivity.EXTRA_USE_ESCALATION, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Launch AlarmActivity.
        startActivity(context, activityIntent, null)

        // Start the escalation service so that the urgency escalation (ack, cancel, remove) is active.
        val escalationIntent = Intent(context, EscalationService::class.java)
        // You can also pass other extras to control escalation here if needed.
        context.startForegroundService(escalationIntent)

        // If this is a one-off alarm, cancel it so it does not repeat.
        if (isOneOff) {
            cancelAlarm(context, alarmId)
        }
    }

    private fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cancelIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            cancelIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmReceiver", "One-off alarm canceled (alarmId: $alarmId)")
    }
}
