package com.cantrip.focusalarm

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    companion object {
        //  Define the same constants here in AlarmActivity
        const val ALARM_LEVEL_EXTRA = "ALARM_LEVEL_EXTRA"
        const val ALARM_NAME_EXTRA = "ALARM_NAME_EXTRA"
        const val IS_ONE_OFF_EXTRA = "IS_ONE_OFF_EXTRA"
        const val ALARM_ID_EXTRA = "ALARM_ID_EXTRA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm) //  Make sure you have this layout

        val alarmLevel = intent.getIntExtra(ALARM_LEVEL_EXTRA, 0)
        val alarmName = intent.getStringExtra(ALARM_NAME_EXTRA) ?: "No Name"
        val isOneOff = intent.getBooleanExtra(IS_ONE_OFF_EXTRA, false)
        val alarmId = intent.getIntExtra(ALARM_ID_EXTRA, 0)

        val alarmInfoTextView = findViewById<TextView>(R.id.alarmInfoTextView) //  And this TextView
        alarmInfoTextView.text = "Level: $alarmLevel, Name: $alarmName, One-Off: $isOneOff, ID: $alarmId"
    }
}