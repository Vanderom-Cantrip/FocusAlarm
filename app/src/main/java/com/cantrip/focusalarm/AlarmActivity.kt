package com.cantrip.focusalarm

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.util.Log
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import java.util.concurrent.TimeUnit
import android.widget.Toast
import android.content.Context
import com.cantrip.focusalarm.AlarmReceiver

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

        // Use modern approach for SDK 31+
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)


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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
                } else {
                    // Handle the case where the user has not granted the permission
                    Toast.makeText(this, "Permission required to schedule exact alarm", Toast.LENGTH_LONG).show()
                    Log.e("AlarmActivity", "Permission to schedule exact alarm not granted")
                    //  Consider redirecting the user to the system settings to grant the permission
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
            }
            Toast.makeText(this, "Snoozed for $SNOOZE_DURATION_MINUTES minutes", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            // Handle the SecurityException
            Toast.makeText(this, "Failed to schedule alarm: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("AlarmActivity", "SecurityException while scheduling alarm: ${e.message}")
            // Consider redirecting the user to the system settings to grant the permission if appropriate.
        }
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
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to cancel alarm: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("AlarmActivity", "SecurityException while cancelling alarm: ${e.message}")
        }


        if (!isOneOff) {
            Log.d("AlarmActivity", "Rescheduling alarm (One-off = false, id = $alarmId)")
            //  You'll need to have the logic to get the next alarm time and reschedule it here.  Make sure to include the permission check here as well.
        } else {
            Log.d("AlarmActivity", "One-off alarm, not rescheduling (id = $alarmId)")
        }
        finish()
    }
}