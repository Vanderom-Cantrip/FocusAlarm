package com.cantrip.focusalarm

import android.content.Intent
import android.graphics.Rect
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
        const val EXTRA_USE_ESCALATION = "EXTRA_USE_ESCALATION"
    }

    private lateinit var alarmRootLayout: ConstraintLayout
    private lateinit var ackConfirmationText: TextView
    private lateinit var actionButtonsLayout: View
    private lateinit var buttonUndo: Button
    private lateinit var buttonOk: Button
    private lateinit var buttonKeepFuture: Button
    private lateinit var buttonExitFromAck: Button
    private lateinit var killButton: ImageButton
    private lateinit var extraKillButton: ImageButton
    private var isActionPerformed = false
    private var offsetX = 0f
    private var offsetY = 0f

    private var useEscalation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val alarmLevel = intent.getIntExtra(ALARM_LEVEL_EXTRA, 1)
        val alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "No Name"
        val isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, 0)
        useEscalation = intent.getBooleanExtra(EXTRA_USE_ESCALATION, false)

        findViewById<TextView>(R.id.alarmInfoTextView).text =
            getString(R.string.alarm_info_text, alarmLevel, alarmName, if (isOneOff) "Yes" else "No", alarmId)

        alarmRootLayout = findViewById(R.id.alarm_root_layout)
        ackConfirmationText = findViewById(R.id.ackConfirmationText)
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout)
        buttonUndo = findViewById(R.id.buttonUndo)
        buttonOk = findViewById(R.id.buttonOk)
        buttonKeepFuture = findViewById(R.id.buttonKeepFuture)
        buttonExitFromAck = findViewById(R.id.buttonExitFromAck)
        killButton = findViewById(R.id.killButton)
        extraKillButton = findViewById(R.id.extraKillButton)

        findViewById<ImageButton>(R.id.ackButton).setOnClickListener {
            if (useEscalation) {
                sendCommandToEscalationService(EscalationService.ACTION_ACKNOWLEDGE)
            }
            ackConfirmationText.visibility = View.VISIBLE
            buttonExitFromAck.visibility = View.VISIBLE
        }

        buttonUndo.setOnClickListener { resetToAlarmScreen() }
        buttonOk.setOnClickListener {
            if (useEscalation) {
                sendCommandToEscalationService(EscalationService.ACTION_CANCEL)
            }
            finishAffinity()
        }
        buttonKeepFuture.setOnClickListener {
            if (useEscalation) {
                sendCommandToEscalationService(EscalationService.ACTION_CANCEL)
            }
            finishAffinity()
        }
        buttonExitFromAck.setOnClickListener {
            if (useEscalation) {
                sendCommandToEscalationService(EscalationService.ACTION_CANCEL)
            }
            finishAffinity()
        }

        killButton.setOnTouchListener(dragTouchListener(killButton, extraKillButton, "cancel"))
        extraKillButton.setOnTouchListener(dragTouchListener(extraKillButton, killButton, "remove"))
    }

    private fun sendCommandToEscalationService(action: String) {
        val intent = Intent(this, EscalationService::class.java)
        intent.action = action
        startService(intent)
    }

    private fun resetToAlarmScreen() {
        isActionPerformed = false
        findViewById<View>(R.id.ackButton).visibility = View.VISIBLE
        findViewById<View>(R.id.ackLabel).visibility = View.VISIBLE
        ackConfirmationText.visibility = View.GONE
        buttonExitFromAck.visibility = View.GONE
        killButton.visibility = View.VISIBLE
        extraKillButton.visibility = View.VISIBLE
        findViewById<View>(R.id.killLabel).visibility = View.VISIBLE
        findViewById<View>(R.id.extraKillLabel).visibility = View.VISIBLE
        actionButtonsLayout.visibility = View.GONE
        alarmRootLayout.setBackgroundColor(getColor(android.R.color.holo_orange_light))
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
                    view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun performAction(type: String) {
        if (isActionPerformed) return
        isActionPerformed = true

        if (useEscalation) {
            sendCommandToEscalationService(EscalationService.ACTION_CANCEL)
        }

        findViewById<View>(R.id.ackButton).visibility = View.GONE
        findViewById<View>(R.id.ackLabel).visibility = View.GONE
        ackConfirmationText.visibility = View.GONE
        buttonExitFromAck.visibility = View.GONE
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

    override fun onDestroy() {
        super.onDestroy()
    }
}
