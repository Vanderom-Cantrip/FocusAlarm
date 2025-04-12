package com.cantrip.focusalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit
import android.app.AlarmManager
import android.app.PendingIntent

// Ensure this class is ONLY defined here. There should be no other AlarmReceiver class in your project.
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm triggered!")

        val alarmName = intent.getStringExtra(AlarmActivity.ALARM_NAME_EXTRA) ?: "An Alarm"
        val alarmId = intent.getIntExtra(AlarmActivity.ALARM_ID_EXTRA, -1)

        Toast.makeText(context, "$alarmName is ringing!", Toast.LENGTH_LONG).show()

        val alarmActivityIntent = Intent(context, AlarmActivity::class.java)
        alarmActivityIntent.putExtra(AlarmActivity.ALARM_NAME_EXTRA, alarmName)
        alarmActivityIntent.putExtra(AlarmActivity.IS_ONE_OFF_EXTRA, intent.getBooleanExtra(AlarmActivity.IS_ONE_OFF_EXTRA, false))
        alarmActivityIntent.putExtra(AlarmActivity.ALARM_ID_EXTRA, alarmId)
        alarmActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(alarmActivityIntent)

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                it.audioAttributes = audioAttributes
            }
            it.play()
        }

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}

class AlarmActivity : Activity() {
    private var alarmName: String = ""
    private var isOneOff: Boolean = false
    private var alarmId: Int = -1

    private lateinit var snoozeButton: ImageButton
    private lateinit var ackButton: ImageButton
    private lateinit var killButton: ImageButton
    private lateinit var extraKillButton: ImageButton
    private lateinit var alarmTextView: TextView

    companion object {
        const val ALARM_NAME_EXTRA = "alarm_name"
        const val IS_ONE_OFF_EXTRA = "is_one_off"
        const val ALARM_ID_EXTRA = "alarm_id"
        private const val SNOOZE_DURATION_MINUTES = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_alarm)

        snoozeButton = findViewById(R.id.snoozeButton)
        ackButton = findViewById(R.id.ackButton)
        killButton = findViewById(R.id.killButton)
        extraKillButton = findViewById(R.id.extraKillButton)
        alarmTextView = findViewById(R.id.alarmText)

        alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "Alarm"
        isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        alarmId = intent.getIntExtra(ALARM_ID_EXTRA, -1)

        alarmTextView.text = getString(R.string.alarm_is_ringing, alarmName)

        // Make absolutely sure you have @color/alarm_button_color defined in res/values/colors.xml
        val buttonColor = ContextCompat.getColor(this, R.color.alarm_button_color)
        snoozeButton.setColorFilter(buttonColor)
        ackButton.setColorFilter(buttonColor)
        killButton.setColorFilter(buttonColor)
        extraKillButton.setColorFilter(buttonColor)

        snoozeButton.setOnClickListener {
            Log.d("AlarmActivity", "Snooze clicked")
            scheduleSnooze()
            finish()
        }

        ackButton.setOnClickListener {
            Log.d("AlarmActivity", "Acknowledge clicked")
            finishAndRemoveAlarm()
        }

        killButton.setOnClickListener {
            Log.d("AlarmActivity", "Kill clicked")
            finishAndRemoveAlarm()
        }

        extraKillButton.setOnClickListener {
            Log.d("AlarmActivity", "Extra Kill clicked")
            finishAndRemoveAlarm()
        }
    }

    private fun scheduleSnooze() {
        val snoozeTimeMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(SNOOZE_DURATION_MINUTES.toLong())
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(this, AlarmReceiver::class.java)
        snoozeIntent.putExtra(ALARM_NAME_EXTRA, alarmName)
        snoozeIntent.putExtra(IS_ONE_OFF_EXTRA, true)
        snoozeIntent.putExtra(ALARM_ID_EXTRA, alarmId)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
        }
        Toast.makeText(this, "Snoozed for $SNOOZE_DURATION_MINUTES minutes", Toast.LENGTH_SHORT).show()
    }

    private fun finishAndRemoveAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        if (!isOneOff) {
            Log.d("AlarmActivity", "Rescheduling alarm (One-off = false, id = $alarmId)")
            //  You'll need to have the logic to get the next alarm time and reschedule it here.
        } else {
            Log.d("AlarmActivity", "One-off alarm, not rescheduling (id = $alarmId)")
        }
        finish()
    }
}

