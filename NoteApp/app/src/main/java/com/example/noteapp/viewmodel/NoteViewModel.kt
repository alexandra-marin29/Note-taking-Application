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
            val notesFolder = noteRepository.getFolderByName("Notes")
            if (notesFolder == null) {
                noteRepository.insertFolder(Folder(id = 1, folderName = "Notes"))
            }
            noteRepository.syncLocalWithFirestore()
        }
    }

    fun addNote(note: Note, callback: (Long) -> Unit) =
        viewModelScope.launch {
            val newId = noteRepository.insertNote(note)
            callback(newId)
        }

    fun deleteNote(note: Note) = viewModelScope.launch {
        if (note.reminderTime != null) {
            cancelReminder(note)
        }
        noteRepository.deleteNote(note)
    }

    fun updateNote(note: Note) =
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }

    fun getAllFolders() = noteRepository.getAllFolders()

    fun addFolder(folderName: String) = viewModelScope.launch {
        noteRepository.insertFolder(Folder(id = 0, folderName = folderName))
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        val notes = noteRepository.getNotesListByFolderId(folder.id)
        notes.forEach { note ->
            if (note.reminderTime != null) {
                cancelReminder(note)
            }
        }
        noteRepository.deleteFolder(folder)
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

    suspend fun getFolderById(id: Int): Folder? = noteRepository.getFolderById(id)

    fun getFolderByIdLiveData(id: Int): LiveData<Folder?> {
        return noteRepository.getFolderByIdLiveData(id)
    }

    fun getAllNotesByFolder(folderId: Int) = noteRepository.getAllNotesByFolder(folderId)
    fun searchNoteInFolder(query: String?, folderId: Int) = noteRepository.searchNoteInFolder(query, folderId)
    fun getNotesSortedByTitle(folderId: Int) = noteRepository.getNotesSortedByTitle(folderId)
    fun getNotesSortedByDateDesc(folderId: Int) = noteRepository.getNotesSortedByDateDesc(folderId)
    fun getNotesSortedByDateAsc(folderId: Int) = noteRepository.getNotesSortedByDateAsc(folderId)
    fun getNoteByIdLiveData(id: Int): LiveData<Note?> = noteRepository.getNoteByIdLiveData(id)

    fun togglePinStatus(note: Note) = viewModelScope.launch {
        val updatedNote = note.copy(isPinned = !note.isPinned)
        noteRepository.updateNote(updatedNote)
    }
}
