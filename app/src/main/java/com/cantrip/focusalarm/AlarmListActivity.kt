package com.cantrip.focusalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import java.util.Calendar

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

        // Schedule all enabled alarms on activity creation.
        scheduleAllAlarms()
    }

    data class AlarmItem(val time: String, val days: List<String>, var enabled: Boolean, val label: String? = "")

    companion object {
        private const val PREF_NAME = "AlarmPrefs"
        private const val KEY_ALARMS = "alarms"
        const val ALARM_ID_EXTRA = "ALARM_ID_EXTRA"
        const val ALARM_NAME_EXTRA = "ALARM_NAME_EXTRA"
        const val IS_ONE_OFF_EXTRA = "IS_ONE_OFF_EXTRA"

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

    // Schedule all enabled alarms.
    private fun scheduleAllAlarms() {
        for ((index, alarm) in alarmList.withIndex()) {
            if (alarm.enabled) {
                // Use the index as a unique alarm ID.
                val alarmId = index
                val triggerTime = getTriggerTime(alarm.time)
                scheduleAlarm(this, triggerTime, alarmId, alarm.label ?: "Alarm")
            }
        }
    }

    // Converts a time string ("HH:mm") to the next occurrence (in ms) minus 15 minutes.
    fun getTriggerTime(timeString: String): Long {
        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return System.currentTimeMillis()
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // If the target time is in the past, move to tomorrow.
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            // Subtract 15 minutes.
            calendar.add(Calendar.MINUTE, -15)
            return calendar.timeInMillis
        } catch (e: Exception) {
            e.printStackTrace()
            return System.currentTimeMillis()
        }
    }

    // Schedules a single alarm using AlarmManager.
    fun scheduleAlarm(context: Context, triggerTime: Long, alarmId: Int, alarmLabel: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // On API level 31+ (Android 12+) check that the app can schedule exact alarms.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Log error and return. Alternatively, you might prompt the user to grant permission.
            android.util.Log.e("AlarmListActivity", "App does not have permission to schedule exact alarms.")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(ALARM_ID_EXTRA, alarmId)
            putExtra(ALARM_NAME_EXTRA, alarmLabel)
            putExtra(IS_ONE_OFF_EXTRA, true) // For now, treat these as one-off alarms.
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
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
                    saveAlarms(context, alarms)
                    if (isChecked) {
                        // Schedule the alarm if enabled.
                        val alarmId = adapterPosition
                        val triggerTime = (context as AlarmListActivity).getTriggerTime(alarm.time)
                        (context as AlarmListActivity).scheduleAlarm(context, triggerTime, alarmId, alarm.label ?: "Alarm")
                    } else {
                        // Optionally, implement cancellation logic here.
                    }
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
