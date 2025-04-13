package com.cantrip.focusalarm

import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class AlarmActivity : AppCompatActivity() {

    companion object {
        const val ALARM_LEVEL_EXTRA = "ALARM_LEVEL_EXTRA"
        const val ALARM_NAME_EXTRA = "ALARM_NAME_EXTRA"
        const val IS_ONE_OFF_EXTRA = "IS_ONE_OFF_EXTRA"
        const val ALARM_ID_EXTRA = "ALARM_ID_EXTRA"
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var alarmRootLayout: ConstraintLayout
    private lateinit var actionButtonsLayout: View
    private lateinit var buttonUndo: Button
    private lateinit var buttonOk: Button
    private lateinit var buttonKeepFuture: Button
    private lateinit var killButton: ImageButton
    private lateinit var extraKillButton: ImageButton
    private var isActionPerformed = false
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val alarmLevel = intent.getIntExtra(ALARM_LEVEL_EXTRA, 0)
        val alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "No Name"
        val isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, 0)

        findViewById<TextView>(R.id.alarmInfoTextView).text =
            "Level: $alarmLevel, Name: $alarmName, One-Off: $isOneOff, ID: $alarmId"

        alarmRootLayout = findViewById(R.id.alarm_root_layout)
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout)
        buttonUndo = findViewById(R.id.buttonUndo)
        buttonOk = findViewById(R.id.buttonOk)
        buttonKeepFuture = findViewById(R.id.buttonKeepFuture)
        killButton = findViewById(R.id.killButton)
        extraKillButton = findViewById(R.id.extraKillButton)

        findViewById<ImageButton>(R.id.ackButton).setOnClickListener {
            stopAlarmSounds()
        }

        buttonUndo.setOnClickListener { resetToAlarmScreen() }
        buttonOk.setOnClickListener { finish() }
        buttonKeepFuture.setOnClickListener { finish() }

        killButton.setOnTouchListener(dragTouchListener(killButton, extraKillButton, "cancel"))
        extraKillButton.setOnTouchListener(dragTouchListener(extraKillButton, killButton, "remove"))

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm1)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    private fun stopAlarmSounds() {
        if (::mediaPlayer.isInitialized) {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (e: IllegalStateException) {
                // Already stopped
            } finally {
                mediaPlayer.release()
            }
        }
    }

    private fun performAction(type: String) {
        if (isActionPerformed) return
        isActionPerformed = true

        stopAlarmSounds()
        findViewById<View>(R.id.ackButton).visibility = View.GONE
        killButton.visibility = View.GONE
        extraKillButton.visibility = View.GONE
        findViewById<View>(R.id.killLabel).visibility = View.GONE
        findViewById<View>(R.id.extraKillLabel).visibility = View.GONE

        actionButtonsLayout.visibility = View.VISIBLE
        buttonUndo.visibility = View.VISIBLE
        buttonOk.visibility = View.VISIBLE
        buttonKeepFuture.visibility = if (type == "remove") View.VISIBLE else View.GONE

        alarmRootLayout.setBackgroundColor(getColor(android.R.color.holo_green_light))
    }

    private fun resetToAlarmScreen() {
        isActionPerformed = false
        findViewById<View>(R.id.ackButton).visibility = View.VISIBLE
        killButton.visibility = View.VISIBLE
        extraKillButton.visibility = View.VISIBLE
        findViewById<View>(R.id.killLabel).visibility = View.VISIBLE
        findViewById<View>(R.id.extraKillLabel).visibility = View.VISIBLE
        actionButtonsLayout.visibility = View.GONE

        alarmRootLayout.setBackgroundColor(getColor(android.R.color.holo_orange_light))

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm1)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    private fun dragTouchListener(source: View, target: View, type: String): View.OnTouchListener {
        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    offsetX = event.rawX - view.x
                    offsetY = event.rawY - view.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX - offsetX
                    view.y = event.rawY - offsetY

                    val sourceLoc = IntArray(2)
                    val targetLoc = IntArray(2)
                    view.getLocationOnScreen(sourceLoc)
                    target.getLocationOnScreen(targetLoc)

                    val sourceRect = Rect(
                        sourceLoc[0],
                        sourceLoc[1],
                        sourceLoc[0] + view.width,
                        sourceLoc[1] + view.height
                    )

                    val targetRect = Rect(
                        targetLoc[0],
                        targetLoc[1],
                        targetLoc[0] + target.width,
                        targetLoc[1] + target.height
                    )

                    if (Rect.intersects(sourceRect, targetRect)) {
                        performAction(type)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate().translationX(0f).translationY(0f).setDuration(150).start()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSounds()
    }
}
