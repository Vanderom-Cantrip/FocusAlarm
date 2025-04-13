package com.cantrip.focusalarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AlarmItem(
    val hour: Int,
    val minute: Int,
    val days: List<String>,
    var isEnabled: Boolean
)

class AlarmAdapter(private val alarms: List<AlarmItem>) :
    RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alarmTimeText)
        val daysText: TextView = view.findViewById(R.id.alarmDaysText)
        val toggle: Switch = view.findViewById(R.id.alarmToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm_card, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        val formattedTime = String.format("%d:%02d %s",
            if (alarm.hour == 0 || alarm.hour == 12) 12 else alarm.hour % 12,
            alarm.minute,
            if (alarm.hour < 12) "AM" else "PM"
        )
        holder.timeText.text = formattedTime
        holder.daysText.text = alarm.days.joinToString(", ")
        holder.toggle.isChecked = alarm.isEnabled

        // Optional: Toggle click logic
        holder.toggle.setOnCheckedChangeListener { _, isChecked ->
            alarm.isEnabled = isChecked
            // You could persist changes here
        }
    }

    override fun getItemCount(): Int = alarms.size
}
