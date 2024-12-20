package com.example.noteapp.repository

import androidx.lifecycle.LiveData
import com.example.noteapp.database.NoteDatabase
import com.example.noteapp.model.Folder
import com.example.noteapp.model.Note

class NoteRepository(private val db: NoteDatabase) {
    suspend fun insertNote(note: Note): Long = db.getNoteDao().insertNote(note)
    suspend fun deleteNote(note: Note) = db.getNoteDao().deleteNote(note)
    suspend fun updateNote(note: Note) = db.getNoteDao().updateNote(note)

    fun getAllFolders() = db.getFolderDao().getAllFolders()
    suspend fun insertFolder(folder: Folder) = db.getFolderDao().insertFolder(folder)
    suspend fun deleteFolder(folder: Folder) = db.getFolderDao().deleteFolder(folder)


    fun getFolderByIdLiveData(id: Int): LiveData<Folder?> = db.getFolderDao().getFolderByIdLiveData(id)

    suspend fun getFolderByName(name: String) = db.getFolderDao().getFolderByName(name)
    suspend fun getFolderById(id: Int) = db.getFolderDao().getFolderById(id)
    fun getAllNotesByFolder(folderId: Int) = db.getNoteDao().getAllNotesByFolder(folderId)
    fun searchNoteInFolder(query: String?, folderId: Int) = db.getNoteDao().searchNoteInFolder(query, folderId)
    fun getNotesSortedByTitle(folderId: Int) = db.getNoteDao().getNotesSortedByTitle(folderId)
    fun getNotesSortedByDateDesc(folderId: Int) = db.getNoteDao().getNotesSortedByDateDesc(folderId)
    fun getNotesSortedByDateAsc(folderId: Int) = db.getNoteDao().getNotesSortedByDateAsc(folderId)
}
