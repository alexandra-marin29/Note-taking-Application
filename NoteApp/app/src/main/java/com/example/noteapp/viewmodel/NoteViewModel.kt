package com.example.noteapp.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.noteapp.model.Folder
import com.example.noteapp.model.Note
import com.example.noteapp.receiver.ReminderReceiver
import com.example.noteapp.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(app: Application, private val noteRepository: NoteRepository) : AndroidViewModel(app) {

    init {
        viewModelScope.launch {
            noteRepository.syncLocalWithFirestore()
        }
    }

    fun addNote(note: Note, callback: (Long) -> Unit) = viewModelScope.launch {
        noteRepository.insertNote(note).also { newId ->
            callback(newId)
        }
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        if (note.reminderTime != null) {
            cancelReminder(note)
        }
        noteRepository.deleteNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }


    fun addFolder(folderName: String, userId: String) = viewModelScope.launch {
        val folder = Folder(folderName = folderName, userId = userId)
        noteRepository.insertFolder(folder)
    }

    fun getAllFoldersByUserId(userId: String) = noteRepository.getAllFoldersByUserId(userId)

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        val notes = noteRepository.getNotesListByFolderId(folder.id, folder.userId)
        notes.forEach { n ->
            if (n.reminderTime != null) cancelReminder(n)
        }
        noteRepository.deleteFolder(folder)
    }

    fun getAllNotesByFolder(folderId: Int, userId: String) = noteRepository.getAllNotesByFolder(folderId, userId)

    fun searchNoteInFolder(query: String?, folderId: Int, userId: String) = noteRepository.searchNoteInFolder(query, folderId, userId)
    fun getNotesSortedByTitle(folderId: Int, userId: String) = noteRepository.getNotesSortedByTitle(folderId, userId)
    fun getNotesSortedByDateDesc(folderId: Int, userId: String) = noteRepository.getNotesSortedByDateDesc(folderId, userId)
    fun getNotesSortedByDateAsc(folderId: Int, userId: String) = noteRepository.getNotesSortedByDateAsc(folderId, userId)

    fun togglePinStatus(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isPinned = !note.isPinned)
        noteRepository.updateNote(updatedNote)
    }

    suspend fun getFolderById(id: Int) = noteRepository.getFolderById(id)
    fun getFolderByIdLiveData(id: Int) = noteRepository.getFolderByIdLiveData(id)
    fun getNoteByIdLiveData(id: Int) = noteRepository.getNoteByIdLiveData(id)

    fun getActiveReminders(currentTime: Long, userId: String, callback: (List<Note>) -> Unit) = viewModelScope.launch {
        val reminders = noteRepository.getActiveReminders(currentTime, userId)
        callback(reminders)
    }


    private fun cancelReminder(note: Note) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", note.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
