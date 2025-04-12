package com.cantrip.focusalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm triggered!") // Debugging

        // Get data from the intent
        val alarmName = intent.getStringExtra(AlarmActivity.ALARM_NAME_EXTRA) ?: "An Alarm"
        val alarmId = intent.getIntExtra(AlarmActivity.ALARM_ID_EXTRA, -1)

        // Show a toast message.
        Toast.makeText(context, "$alarmName is ringing!", Toast.LENGTH_LONG).show()

        // Start the AlarmActivity to show the drag-to-dismiss UI.
        val alarmActivityIntent = Intent(context, AlarmActivity::class.java)
        alarmActivityIntent.putExtra(AlarmActivity.ALARM_NAME_EXTRA, alarmName)
        alarmActivityIntent.putExtra(AlarmActivity.IS_ONE_OFF_EXTRA, intent.getBooleanExtra(AlarmActivity.IS_ONE_OFF_EXTRA, false))
        alarmActivityIntent.putExtra(AlarmActivity.ALARM_ID_EXTRA, alarmId) // Pass the id.
        alarmActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(alarmActivityIntent)

        // Play a ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        if (ringtone != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.audioAttributes = audioAttributes;
            }
            ringtone.play()
        }

        // Vibrate
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}