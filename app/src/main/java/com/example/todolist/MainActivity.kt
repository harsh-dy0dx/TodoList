package com.example.todolist

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private val taskDao by lazy { AppDatabase.getDatabase(this).taskDao() }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeTasks()
        askNotificationPermission()
        checkExactAlarmPermission()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        binding.settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
    }

    private fun updateGreeting() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("user_name", "User")
        binding.textViewGreeting.text = "Hello $name"
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Permission Required")
                    .setMessage("Our reminder feature needs a special permission to work reliably. Please tap 'Go to Settings' and enable the 'Alarms & Reminders' permission for this app.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        Intent().also { intent ->
                            intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            intent.data = Uri.fromParts("package", packageName, null)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClicked = { task ->
                val intent = Intent(this, AddTaskActivity::class.java)
                intent.putExtra("EXTRA_TASK", task)
                startActivity(intent)
            },
            onCheckClicked = { task ->
                lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    taskDao.updateTask(updatedTask)

                    if (updatedTask.isCompleted) {
                        cancelNotifications(updatedTask.id)
                    }
                }
            },
            onTaskLongClicked = { task ->
                showDeleteConfirmationDialog(task)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            taskDao.getAllTasks().collect { tasks ->
                val pendingTasks = tasks.count { !it.isCompleted }
                binding.textViewTaskCount.text = "$pendingTasks Tasks are pending"

                if (tasks.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                }
                taskAdapter.submitList(tasks)
            }
        }
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .show()
    }

    private fun deleteTask(task: Task) {
        lifecycleScope.launch {

            cancelNotifications(task.id)
            taskDao.deleteTask(task)
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
            taskId + 1000000, // Request code for the deadline
            deadlineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(deadlinePendingIntent)
    }
}
