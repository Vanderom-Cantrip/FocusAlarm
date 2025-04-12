// MainActivity.kt
package com.example.alarmexample  // Make sure this package name is correct and matches your AndroidManifest.xml

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var setAlarmButton: Button
    private lateinit var alarmStatusTextView: TextView
    private lateinit var permissionLauncher: ActivityResultLauncher<Intent>
    private val ALARM_REQUEST_CODE = 123
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // **CRUCIAL:  This MUST match the layout file name**

        // Initialize views *after* setContentView()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setAlarmButton = findViewById(R.id.setAlarmButton)
        alarmStatusTextView = findViewById(R.id.alarmStatusTextView)

        // Initialize ActivityResultLauncher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // You can handle the result here, but in this case, we'll also check in onResume()
            Log.d(TAG, "ActivityResultLauncher result: $result")
        }

        setAlarmButton.setOnClickListener {
            checkAndSetAlarm()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permission status whenever the activity resumes
        updateAlarmStatus()
    }

    private fun checkAndSetAlarm() {
        if (alarmManager.canScheduleExactAlarms()) {
            setExactAlarm()
        } else {
            // Explain why the permission is needed
            Toast.makeText(
                this,
                "Exact alarm permission is required for this app to function correctly.",
                Toast.LENGTH_LONG
            ).show()

            // Request the permission
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            permissionLauncher.launch(intent)
        }
    }

    private fun setExactAlarm() {
        // Create an intent for the BroadcastReceiver
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  // Use FLAG_IMMUTABLE
        )

        // Set the alarm time (10 seconds from now for demonstration)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.SECOND, 10)
        }

        // Set the exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Log.d(TAG, "Alarm set for ${calendar.time}")
                Toast.makeText(this, "Alarm set for 10 seconds", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "setExactAlarm: Permission not granted") //should not happen, but good to have.
                Toast.makeText(this, "Permission to set alarm not granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Log.d(TAG, "Alarm set for ${calendar.time}")
            Toast.makeText(this, "Alarm set for 10 seconds", Toast.LENGTH_SHORT).show()
        }
        updateAlarmStatus()
    }

    private fun updateAlarmStatus() {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmStatusTextView.text = "Exact alarm permission is granted"
        } else {
            alarmStatusTextView.text = "Exact alarm permission is NOT granted"
        }
    }

    // BroadcastReceiver to handle the alarm event
    class AlarmReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("AlarmReceiver", "Alarm triggered!")
            Toast.makeText(context, "Alarm triggered!", Toast.LENGTH_LONG).show()
            // You can add any action you want to perform here, e.g., show a notification,
            // start a service, etc.
        }
    }
}

