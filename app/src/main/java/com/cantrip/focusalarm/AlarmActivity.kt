package com.cantrip.focusalarm

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.xr.runtime.math.toDegrees
import kotlin.math.atan2

class AlarmActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window flags to display over the lock screen and keep the screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm)

        // Retrieve the buttons
        val killButton = findViewById<ImageButton>(R.id.killButton)
        val extraKillButton = findViewById<ImageButton>(R.id.extraKillButton)

        // Determine a center point for the drag gesture (e.g., near the bottom center)
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val centerX = screenWidth / 2f
        val centerY = screenHeight * 0.8f

        // Cancel Alarm: drag clockwise motion threshold detection
        killButton.setOnTouchListener(object : View.OnTouchListener {
            var startAngle = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val x = event.rawX
                val y = event.rawY

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startAngle = angleFromCenter(x, y, centerX, centerY)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val currentAngle = angleFromCenter(x, y, centerX, centerY)
                        // Calculate clockwise difference
                        val diff = currentAngle - startAngle
                        val clockwiseDiff = if (diff < 0) diff + 360 else diff

                        if (clockwiseDiff > 60) { // threshold: 60 degrees, adjust as needed
                            Toast.makeText(
                                this@AlarmActivity,
                                "Cancel Alarm Activated",
                                Toast.LENGTH_SHORT
                            ).show()
                            startAngle = currentAngle
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // End gesture; optionally animate back if buttons are being moved
                        return true
                    }
                }
                return false
            }
        })

        // Remove Alarm: drag counter-clockwise threshold detection
        extraKillButton.setOnTouchListener(object : View.OnTouchListener {
            var startAngle = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val x = event.rawX
                val y = event.rawY

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startAngle = angleFromCenter(x, y, centerX, centerY)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val currentAngle = angleFromCenter(x, y, centerX, centerY)
                        val diff = startAngle - currentAngle
                        val ccwDiff = if (diff < 0) diff + 360 else diff

                        if (ccwDiff > 60) { // threshold: 60 degrees
                            Toast.makeText(
                                this@AlarmActivity,
                                "Remove Alarm Activated",
                                Toast.LENGTH_SHORT
                            ).show()
                            startAngle = currentAngle
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun angleFromCenter(x: Float, y: Float, centerX: Float, centerY: Float): Float {
        val dx = x - centerX
        val dy = y - centerY
        val radians = atan2(dy, dx) // returns angle in radians, between -π and π
        var degrees = toDegrees(radians) // convert to degrees
        if (degrees < 0) degrees += 360f // normalize to 0-360 degrees
        return degrees
    }
}
