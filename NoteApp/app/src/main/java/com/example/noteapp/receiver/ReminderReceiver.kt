package com.example.noteapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.noteapp.R
import com.example.noteapp.database.NoteDatabase
import com.example.noteapp.model.Note
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val noteId = intent.getIntExtra("noteId", -1)
            val noteTitle = intent.getStringExtra("noteTitle")
            if (noteId == -1 || noteTitle.isNullOrEmpty()) return

            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val db = NoteDatabase.invoke(context)
            val noteDao = db.getNoteDao()

            CoroutineScope(Dispatchers.IO).launch {
                val note: Note? = noteDao.getNoteById(noteId)
                if (note == null) return@launch

                val currentUser = FirebaseAuth.getInstance().currentUser
                val currentUserId = currentUser?.uid

                if (note.userId != currentUserId) {
                    return@launch
                }

                val notification = NotificationCompat.Builder(context, "noteReminderChannel")
                    .setSmallIcon(R.drawable.baseline_notification_important_24)
                    .setContentTitle(noteTitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(noteId, notification)

                if (note.reminderTime != null) {
                    val updatedNote = note.copy(reminderTime = null)
                    noteDao.updateNote(updatedNote)
                }
            }

        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
