package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val taskTitle = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "A task is due."
        val taskId = intent.getIntExtra("EXTRA_TASK_ID", 0)
        val notificationType = intent.getStringExtra("EXTRA_NOTIFICATION_TYPE") ?: "DEADLINE"

        val contentTitle = if (notificationType == "REMINDER") "Reminder: Due in 5 mins" else "Deadline!"

        // --- FIX: Use IMPORTANCE_HIGH for the notification channel to make alerts more visible ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("task_reminder_channel", "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "task_reminder_channel")
            .setSmallIcon(R.drawable.ic_check_circle_filled)
            .setContentTitle(contentTitle)
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationId = if (notificationType == "REMINDER") taskId else taskId + 2000000
        notificationManager.notify(notificationId, notification)
    }
}
