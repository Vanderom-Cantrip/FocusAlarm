package com.cantrip.focusalarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlarmListActivity : AppCompatActivity() {

    private lateinit var alarmRecyclerView: RecyclerView
    private val alarms = listOf(
        AlarmItem("8:30 AM", listOf("Mon", "Tue", "Wed", "Thu", "Fri"), true),
        AlarmItem("9:00 AM", listOf("Sun", "Sat"), false)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_list)

        alarmRecyclerView = findViewById(R.id.alarmRecyclerView)
        alarmRecyclerView.layoutManager = LinearLayoutManager(this)
        alarmRecyclerView.adapter = AlarmAdapter(alarms)
    }

    data class AlarmItem(val time: String, val days: List<String>, var enabled: Boolean)

    class AlarmAdapter(private val alarms: List<AlarmItem>) :
        RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_alarm, parent, false)
            return AlarmViewHolder(view)
        }

        override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
            val alarm = alarms[position]
            holder.bind(alarm)
        }

        override fun getItemCount(): Int = alarms.size

        class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val timeTextView: TextView = itemView.findViewById(R.id.textTime)
            private val daysTextView: TextView = itemView.findViewById(R.id.textDays)
            private val switchEnabled: Switch = itemView.findViewById(R.id.switchEnabled)

            fun bind(alarm: AlarmItem) {
                timeTextView.text = alarm.time
                daysTextView.text = alarm.days.joinToString(", ")
                switchEnabled.isChecked = alarm.enabled

                switchEnabled.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                    alarm.enabled = isChecked
                }
            }
        }
    }
}
