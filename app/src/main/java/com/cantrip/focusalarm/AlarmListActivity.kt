package com.cantrip.focusalarm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmListActivity : AppCompatActivity() {

    private lateinit var alarmRecyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private var alarmList: MutableList<AlarmItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_list)

        alarmRecyclerView = findViewById(R.id.alarmRecyclerView)
        alarmRecyclerView.layoutManager = LinearLayoutManager(this)

        alarmList = loadAlarms(this).toMutableList()
        alarmAdapter = AlarmAdapter(alarmList, this)
        alarmRecyclerView.adapter = alarmAdapter
    }

    data class AlarmItem(val time: String, val days: List<String>, var enabled: Boolean, val label: String? = "")

    companion object {
        private const val PREF_NAME = "AlarmPrefs"
        private const val KEY_ALARMS = "alarms"

        fun loadAlarms(context: Context): List<AlarmItem> {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = sharedPrefs.getString(KEY_ALARMS, null) ?: return emptyList()
            val type = object : TypeToken<List<AlarmItem>>() {}.type
            return Gson().fromJson(json, type)
        }

        fun saveAlarms(context: Context, alarms: List<AlarmItem>) {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(alarms)
            sharedPrefs.edit().putString(KEY_ALARMS, json).apply()
        }
    }

    class AlarmAdapter(private val alarms: MutableList<AlarmItem>, private val context: Context) :
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

        inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val timeTextView: TextView = itemView.findViewById(R.id.textTime)
            private val daysTextView: TextView = itemView.findViewById(R.id.textDays)
            private val switchEnabled: Switch = itemView.findViewById(R.id.switchEnabled)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)

            fun bind(alarm: AlarmItem) {
                timeTextView.text = if (!alarm.label.isNullOrBlank()) "${alarm.label} – ${alarm.time}" else alarm.time
                daysTextView.text = alarm.days.joinToString(", ")
                switchEnabled.isChecked = alarm.enabled

                switchEnabled.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                    alarm.enabled = isChecked
                }

                deleteButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        alarms.removeAt(position)
                        notifyItemRemoved(position)
                        saveAlarms(context, alarms)
                    }
                }
            }
        }
    }
}
