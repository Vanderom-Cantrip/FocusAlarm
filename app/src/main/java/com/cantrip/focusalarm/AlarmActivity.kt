package com.cantrip.focusalarm

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    companion object {
        const val ALARM_LEVEL_EXTRA = "ALARM_LEVEL_EXTRA"
        const val ALARM_NAME_EXTRA = "ALARM_NAME_EXTRA"
        const val IS_ONE_OFF_EXTRA = "IS_ONE_OFF_EXTRA"
        const val ALARM_ID_EXTRA = "ALARM_ID_EXTRA"
    }

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val repeatIntervalMillis = 10_000L // 10 seconds

    private val playLoop = object : Runnable {
        override fun run() {
            player = MediaPlayer.create(this@AlarmActivity, R.raw.alarm1)
            player?.setOnCompletionListener {
                it.release()
                handler.postDelayed(this, repeatIntervalMillis)
            }
            player?.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val alarmLevel = intent.getIntExtra(ALARM_LEVEL_EXTRA, 0)
        val alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "No Name"
        val isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, 0)

        val alarmInfoTextView = findViewById<TextView>(R.id.alarmInfoTextView)
        alarmInfoTextView.text = "Level: $alarmLevel, Name: $alarmName, One-Off: $isOneOff, ID: $alarmId"

        playLoop.run() // Start alarm loop
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}
