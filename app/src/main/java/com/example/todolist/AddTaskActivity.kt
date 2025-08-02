package com.example.todolist

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.todolist.databinding.ActivityAddTaskBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTaskBinding
    private val taskDao by lazy { AppDatabase.getDatabase(this).taskDao() }
    private var currentTask: Task? = null
    private var dueDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        currentTask = intent.getSerializableExtra("EXTRA_TASK") as? Task
        setupView()

        binding.buttonSetDueDate.setOnClickListener { showDateTimePicker() }
        binding.buttonSave.setOnClickListener { saveTask() }
    }

    private fun setupView() {
        if (currentTask != null) {
            binding.toolbar.title = "Edit Task"
            binding.editTextTitle.setText(currentTask?.title)
            binding.editTextDescription.setText(currentTask?.description)
            when (currentTask?.priority) {
                1 -> binding.chipLow.isChecked = true
                2 -> binding.chipMedium.isChecked = true
                3 -> binding.chipHigh.isChecked = true
            }
            currentTask?.dueDate?.let { dueDate.timeInMillis = it }
            binding.buttonSave.text = "Update Task"
        } else {
            binding.toolbar.title = "Add New Task"
            binding.chipMedium.isChecked = true
        }
        updateDueDateButtonText()
    }

    private fun showDateTimePicker() {
        val currentCal = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            dueDate.set(Calendar.YEAR, year)
            dueDate.set(Calendar.MONTH, month)
            dueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(this, { _, hourOfDay, minute ->
                dueDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                dueDate.set(Calendar.MINUTE, minute)
                updateDueDateButtonText()
            }, currentCal.get(Calendar.HOUR_OF_DAY), currentCal.get(Calendar.MINUTE), false).show()
        }, currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDueDateButtonText() {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        binding.buttonSetDueDate.text = sdf.format(dueDate.time)
    }

    private fun saveTask() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val priority = when (binding.chipGroupPriority.checkedChipId) {
            R.id.chipLow -> 1
            R.id.chipMedium -> 2
            R.id.chipHigh -> 3
            else -> 2
        }
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (dueDate.timeInMillis < System.currentTimeMillis()) {
            Toast.makeText(this, "Cannot set a deadline in the past.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (currentTask == null) {
                val newTask = Task(title = title, description = description, priority = priority, dueDate = dueDate.timeInMillis)
                val newId = taskDao.insertTask(newTask)
                scheduleNotifications(newId.toInt(), title, dueDate.timeInMillis)
                Toast.makeText(this@AddTaskActivity, "Task Saved", Toast.LENGTH_SHORT).show()
            } else {
                val updatedTask = currentTask!!.copy(title = title, description = description, priority = priority, dueDate = dueDate.timeInMillis)
                cancelNotifications(updatedTask.id)
                taskDao.updateTask(updatedTask)
                scheduleNotifications(updatedTask.id, updatedTask.title, updatedTask.dueDate!!)
                Toast.makeText(this@AddTaskActivity, "Task Updated", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun scheduleNotifications(taskId: Int, taskTitle: String, timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminderTime = timeInMillis - (5 * 60 * 1000)
        if (reminderTime > System.currentTimeMillis()) {
            val reminderIntent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_TASK_ID", taskId)
                putExtra("EXTRA_TASK_TITLE", taskTitle)
                putExtra("EXTRA_NOTIFICATION_TYPE", "REMINDER")
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                this,
                taskId,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // --- FIX: Use setExactAndAllowWhileIdle for more reliable alarms that work even when the phone is sleeping ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, reminderPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, reminderPendingIntent)
            }
        }

        val deadlineIntent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_TITLE", taskTitle)
            putExtra("EXTRA_NOTIFICATION_TYPE", "DEADLINE")
        }
        val deadlinePendingIntent = PendingIntent.getBroadcast(
            this,
            taskId + 1000000,
            deadlineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, deadlinePendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, deadlinePendingIntent)
        }
    }

    private fun cancelNotifications(taskId: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminderIntent = Intent(this, NotificationReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            this,
            taskId,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)

        val deadlineIntent = Intent(this, NotificationReceiver::class.java)
        val deadlinePendingIntent = PendingIntent.getBroadcast(
            this,
            taskId + 1000000,
            deadlineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(deadlinePendingIntent)
    }
}
