package com.cantrip.focusalarm

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import java.util.Calendar

class AlarmActivity : Activity() {

    private lateinit var killButton: ImageButton
    private lateinit var extraKillButton: ImageButton
    private var killButtonOriginalX = 0f
    private var killButtonOriginalY = 0f
    private var extraKillButtonOriginalX = 0f
    private var extraKillButtonOriginalY = 0f
    private var alarmName: String = "" // Added alarm name storage
    private var isOneOff: Boolean = false // Added flag for one-off alarm

    // Constants for intent extra keys
    companion object {
        const val ALARM_NAME_EXTRA = "alarm_name"
        const val IS_ONE_OFF_EXTRA = "is_one_off"
        const val ALARM_ID_EXTRA = "alarm_id" //Pass the alarm ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use modern APIs for showing the activity over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContentView(R.layout.activity_alarm)

        // Retrieve ImageButtons from the layout
        killButton = findViewById(R.id.killButton)
        extraKillButton = findViewById(R.id.extraKillButton)

        // Get data from the intent
        alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "This Alarm"  // Default Value
        isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, -1) // Get the alarm ID.

        // Set up OnTouchListeners for the buttons
        setupDragButton(killButton, "$alarmName Alarm Cancelled", alarmId) // Pass alarmId
        setupDragButton(extraKillButton, "$alarmName Alarm Removed", alarmId) // Pass alarmId
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Get the initial positions of the buttons.  Do this *after* the layout is complete.
            killButtonOriginalX = killButton.x
            killButtonOriginalY = killButton.y
            extraKillButtonOriginalX = extraKillButton.x
            extraKillButtonOriginalY = extraKillButton.y
        }
    }

    private fun setupDragButton(button: ImageButton, toastText: String, alarmId: Int) { // Added alarmId parameter
        button.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f
            var startY = 0f
            var isDragging = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        isDragging = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!isDragging) return false
                        val dx = event.rawX - startX
                        val dy = event.rawY - startY
                        v.x = v.x + dx
                        v.y = v.y + dy
                        startX = event.rawX
                        startY = event.rawY
                        // Check for snap back and trigger
                        if (v.id == R.id.killButton && isNear(v, extraKillButton)) {
                            // Cancel the alarm
                            cancelAlarm(alarmId)
                            showToastAndFinish(toastText) // Use the helper
                            isDragging = false // Stop dragging
                            return true
                        } else if (v.id == R.id.extraKillButton && isNear(v, killButton)) {
                            if (isOneOff) {
                                cancelAlarm(alarmId)
                                showToastAndFinish(toastText) //and use the helper.
                            } else {
                                // Show confirmation dialog for repeating alarms
                                showRemoveConfirmationDialog(alarmId) // Pass alarmId
                            }
                            isDragging = false // Stop dragging
                            return true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            resetButtonPosition(v)
                            isDragging = false
                            return true
                        }
                        return false
                    }
                    else -> return false
                }
            }
        })
    }

    private fun isNear(v1: View, v2: View): Boolean {
        val xThreshold = v2.width * 0.5f
        val yThreshold = v2.height * 0.5f
        val xDiff = Math.abs(v1.x - v2.x)
        val yDiff = Math.abs(v1.y - v2.y)
        return xDiff < xThreshold && yDiff < yThreshold
    }

    private fun resetButtonPosition(v: View) {
        if (v.id == R.id.killButton) {
            v.x = killButtonOriginalX
            v.y = killButtonOriginalY
        } else if (v.id == R.id.extraKillButton) {
            v.x = extraKillButtonOriginalX
            v.y = extraKillButtonOriginalY
        }
    }

    private fun showToastAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({
            finish() // Close the activity after the toast
        }, 5000)
    }

    private fun showRemoveConfirmationDialog(alarmId: Int) { // Added alarmId parameter
        AlertDialog.Builder(this)
            .setTitle("Remove Alarm")
            .setMessage("Are you sure you want to permanently delete all instances of $alarmName alarm?")
            .setPositiveButton("Yes") { dialog, which ->
                // Remove the alarm
                cancelAlarm(alarmId)
                Toast.makeText(this@AlarmActivity, "$alarmName Alarm Removed", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("No") { dialog, which ->
                // Do nothing
                dialog.dismiss()
            }
            .show()
    }

    private fun cancelAlarm(alarmId: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)  // Use your AlarmReceiver
        val pendingIntent = PendingIntent.getBroadcast(this, alarmId, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE) // Use the alarmId
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmActivity", "Alarm with ID $alarmId cancelled")
    }
}

