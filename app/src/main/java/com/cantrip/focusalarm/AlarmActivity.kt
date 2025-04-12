package com.cantrip.focusalarm

import android.app.Activity
import android.os.Bundle
import android.util.Log // Import Log
import android.view.View // Import View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton // Import ImageButton
import android.widget.Toast
import android.os.Build

// Implement the listener interface
class AlarmActivity : Activity(), OnConfirmListener {

    private val TAG = "AlarmActivity" // Add TAG for logging
    private val DRAG_DISTANCE_DP = 60f // Define required drag distance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep using Toast for debug confirmation if needed
        // Toast.makeText(this, "AlarmActivity Launched", Toast.LENGTH_LONG).show()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON // or FLAG_DISMISS_KEYGUARD (with caveats)
        )
        // For modern Android (API 27+), preferred way:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Optional: Request dismiss keyguard (might need more setup)
            // val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            // keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // Use flags for older versions
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }


        setContentView(R.layout.activity_alarm)

        // --- Get references to ALL buttons ---
        val snoozeButton = findViewById<Button>(R.id.snoozeButton)
        val ackButton = findViewById<Button>(R.id.acknowledgeButton) // Corrected ID based on new XML
        val cancelIconButton = findViewById<ImageButton>(R.id.cancel_icon_button) // Icon Button
        val removeIconButton = findViewById<ImageButton>(R.id.remove_icon_button) // Icon Button

        // --- Standard Button Listeners ---
        snoozeButton.setOnClickListener {
            handleSnooze()
        }

        ackButton.setOnClickListener {
            handleAcknowledge()
        }

        // --- Custom Drag Listeners ---
        val dragListener = DragToConfirmListener(this, DRAG_DISTANCE_DP, this)
        cancelIconButton.setOnTouchListener(dragListener)
        removeIconButton.setOnTouchListener(dragListener)
    }

    // --- Implement OnConfirmListener methods ---

    override fun onConfirm(view: View) {
        Log.d(TAG, "Drag confirmed on view: ${view.id}")
        when (view.id) {
            R.id.cancel_icon_button -> handleCancelAlarm()
            R.id.remove_icon_button -> handleRemoveAlarm()
        }
    }

    override fun onDragStart(view: View) {
        Log.d(TAG, "Drag started on view: ${view.id}")
        // Optional: Add visual feedback like slight scaling or alpha change
        view.animate().scaleX(1.1f).scaleY(1.1f).alpha(0.8f).setDuration(100).start()
    }

    override fun onDragEnd(view: View, confirmed: Boolean) {
        Log.d(TAG, "Drag ended on view: ${view.id}, Confirmed: $confirmed")
        // Reset any visual feedback applied in onDragStart, even if not confirmed
        view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
        // Note: Position reset is handled within DragToConfirmListener itself
    }


    // --- Action Handler Methods ---

    private fun handleSnooze() {
        Toast.makeText(this, "Snooze pressed (Logic TBD)", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Snooze Action Triggered")
        // TODO: Implement snooze logic (reschedule alarm using AlarmManager)
        // TODO: Stop sound/vibration here
        finish() // Close the alarm alert for now
    }

    private fun handleAcknowledge() {
        Toast.makeText(this, "Acknowledge pressed (Logic TBD)", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Acknowledge Action Triggered")
        // TODO: Implement acknowledgment logic (cancel any specific pending intents for this instance if needed)
        // TODO: Stop sound/vibration here
        finish() // Close the alarm alert
    }

    private fun handleCancelAlarm() {
        Toast.makeText(this, "Cancel Alarm confirmed (Logic TBD)", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Cancel Alarm Action Triggered")
        // TODO: Implement cancel logic (stop sound/vibration, cancel pending intents for THIS instance via AlarmManager)
        // TODO: Stop sound/vibration here
        finish() // Close the alarm alert
    }

    private fun handleRemoveAlarm() {
        Toast.makeText(this, "Remove Alarm confirmed (Initial Action)", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Remove Alarm Action Triggered - Performing CANCEL logic for now.")
        // --- For now, behaves exactly like Cancel ---
        // TODO: Implement cancel logic (stop sound/vibration, cancel pending intents for THIS instance via AlarmManager)
        // TODO: Stop sound/vibration here

        // --- TODO: Implement Pending Deletion State ---
        // Instead of just finishing, trigger the state for the *next* confirmation step.
        // Examples:
        // 1. Save alarm ID to SharedPreferences as "pending_delete"
        // 2. Show a persistent Notification: "Alarm [Name] ready for removal. Tap to confirm."
        //    - Notification tap could open MainActivity or a confirmation dialog.
        // 3. Set a flag in a database associated with the alarm.
        Log.w(TAG, "TODO: Implement state tracking for pending permanent deletion confirmation.")

        finish() // Close the alarm alert
    }

    // --- TODO: Implement Sound/Vibration Control ---
    // private fun stopAlarmSoundAndVibration() {
    //    Log.d(TAG, "Stopping sound and vibration...")
    //    // Add code here to stop MediaPlayer/Vibrator loops when implemented
    // }

    // --- Lifecycle methods ---
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmActivity onDestroy")
        // Ensure any running sound/vibration is stopped cleanly here too
        // stopAlarmSoundAndVibration()
    }
}