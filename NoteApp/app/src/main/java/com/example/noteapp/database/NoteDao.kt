package com.example.noteapp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.noteapp.model.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note):Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, id DESC")
    fun getAllNotesByFolder(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM NOTES WHERE (noteTitle LIKE :query OR noteDesc LIKE :query) AND folderId = :folderId ORDER BY isPinned DESC, id DESC")
    fun searchNoteInFolder(query: String?, folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, noteTitle ASC")
    fun getNotesSortedByTitle(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, dateCreated DESC")
    fun getNotesSortedByDateDesc(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, dateCreated ASC")
    fun getNotesSortedByDateAsc(folderId: Int): LiveData<List<Note>>
}
