package com.example.noteapp.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.noteapp.database.NoteDatabase
import com.example.noteapp.model.Note
import com.example.noteapp.receiver.ReminderReceiver
import com.example.noteapp.repository.NoteRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Scheduling active reminders.")

            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e("BootReceiver", "User is not authenticated. Alarms cannot be rescheduled.")
                return
            }

            val userId = currentUser.uid
            val currentTime = System.currentTimeMillis()

            val noteRepository = NoteRepository(NoteDatabase.invoke(context))

            CoroutineScope(Dispatchers.IO).launch {
                val activeReminders = noteRepository.getActiveReminders(currentTime, userId)
                Log.d("BootReceiver", "Found ${activeReminders.size} active reminders.")
                activeReminders.forEach { note ->
                    scheduleReminder(context, note)
                }
            }
        }
    }

    private fun scheduleReminder(context: Context, note: Note) {
        val reminderTimeInMillis = note.reminderTime ?: return

        if (reminderTimeInMillis < System.currentTimeMillis()) {
            Log.d("BootReceiver", "Alarm for note ID: ${note.id} is in the past. Not scheduling.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("BootReceiver", "Permission for exact alarms is not granted.")
                return
            }
        }

        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("noteId", note.id)
                putExtra("noteTitle", note.noteTitle)
                putExtra("noteDesc", note.noteDesc)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                note.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, pendingIntent)

            Log.d("BootReceiver", "Scheduled alarm for note ID: ${note.id} at $reminderTimeInMillis")
        } catch (e: SecurityException) {
            Log.e("BootReceiver", "SecurityException: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e("BootReceiver", "Exception: ${e.message}")
            e.printStackTrace()
        }
    }
}
