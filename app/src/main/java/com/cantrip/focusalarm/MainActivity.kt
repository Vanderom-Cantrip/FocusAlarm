package com.cantrip.focusalarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    // Unique code for the PendingIntent (important if you add multiple alarms later)
    private val ALARM_REQUEST_CODE = 0
    // Unique code for notification permission request (if you implement runtime request)
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val repeatSpinner = findViewById<Spinner>(R.id.repeatSpinner)
        val setAlarmButton = findViewById<Button>(R.id.setAlarmButton)
        val testAlarmButton = findViewById<Button>(R.id.testAlarmButton)

        timePicker.setIs24HourView(true)

        val repeatOptions = arrayOf("Daily", "Weekdays", "Weekly", "Select Days", "None") // Added "None" for single alarm
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, repeatOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        repeatSpinner.adapter = adapter
        // Default to "None" for single alarm test
        repeatSpinner.setSelection(repeatOptions.indexOf("None"))


        // --- SET ALARM BUTTON ---
        setAlarmButton.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val repeat = repeatSpinner.selectedItem.toString() // We'll use this later

            // Basic validation: check if we need to ask for notification permission (Android 13+)
            // Although not strictly needed for scheduling itself, it IS needed for the foreground service
            // notification which is essential for reliable alarms on modern Android.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                    // --- Suggestion: Implement runtime permission request here ---
                    // For now, just inform the user. A real app should request the permission.
                    // ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
                    Toast.makeText(this, "Notification permission needed for reliable alarms.", Toast.LENGTH_LONG).show()
                    // Optionally guide to settings, or implement requestPermissions flow.
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted.")
                    // You might want to return here or disable the button until granted.
                    // return@setOnClickListener // Uncomment this to prevent scheduling without permission
                }
            }

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Check for exact alarm permission (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Permission not granted. Guide user to settings.
                    Toast.makeText(this, "Permission needed to schedule exact alarms.", Toast.LENGTH_LONG).show()
                    Log.w(TAG, "Exact alarm permission not granted.")
                    // --- Suggestion: Intent to open settings ---
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { intent ->
                        // Optional: Specify package to go directly to your app's setting
                        // intent.data = Uri.parse("package:$packageName") // Requires import android.net.Uri
                        startActivity(intent)
                    }
                    return@setOnClickListener // Stop execution here
                }
            }

            // --- Calculate Trigger Time ---
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the calculated time is in the past, schedule it for the next day
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                    Log.d(TAG, "Time is in the past, setting for tomorrow.")
                }
            }
            val triggerMillis = calendar.timeInMillis

            // --- Create PendingIntent ---
            val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
                // You can add extras here if AlarmActivity needs info later
                // e.g., putExtra("ALARM_ID", ALARM_REQUEST_CODE)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                ALARM_REQUEST_CODE, // Use a unique code for each alarm later
                alarmIntent,
                // Use FLAG_IMMUTABLE for security; FLAG_UPDATE_CURRENT to update if already set
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // --- Schedule the Alarm ---
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )

                // --- Confirmation Toast ---
                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault())
                val formattedTime = dateFormat.format(calendar.time)
                val message = "Alarm set for $formattedTime" // (${repeat})" // Add repeat later
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, message)

            } catch (e: SecurityException) {
                // Should be caught by canScheduleExactAlarms, but good practice
                Toast.makeText(this, "SecurityException: Could not schedule exact alarm.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Failed to set exact alarm", e)
                // Maybe guide to settings again
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { intent ->
                        startActivity(intent)
                    }
                }
            }
        }

        // --- TEST ALARM BUTTON (remains the same for now) ---
        testAlarmButton.setOnClickListener {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200) // Shorter test beep
            // Consider releasing the tone generator
            // toneG.release() // Causes issues if clicked rapidly, manage instance better if needed

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") // Suppress deprecation warning for older APIs
                vibrator.vibrate(500)
            }

            Toast.makeText(this, "Test alarm triggered!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, AlarmActivity::class.java)
            startActivity(intent)
            Log.d(TAG, "Launched AlarmActivity via Test button")
        }
    }

    // --- Suggestion: Handle Notification Permission Result ---
    /* Remove comment block if you implement runtime permission request
   override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
       super.onRequestPermissionsResult(requestCode, permissions, grantResults)
       if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
           if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
               Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.")
           } else {
               Toast.makeText(this, "Notification permission denied. Alarms may be less reliable.", Toast.LENGTH_LONG).show()
               Log.w(TAG, "POST_NOTIFICATIONS permission denied by user.")
               // Explain to the user why it's needed, maybe guide to settings
           }
       }
   }
   */
}