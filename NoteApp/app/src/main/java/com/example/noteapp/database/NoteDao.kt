package com.example.noteapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.noteapp.model.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note:Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY id DESC")
    fun getAllNotesByFolder(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM NOTES WHERE (noteTitle LIKE :query OR noteDesc LIKE :query) AND folderId = :folderId")
    fun searchNoteInFolder(query: String?, folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY noteTitle ASC")
    fun getNotesSortedByTitle(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY dateCreated DESC")
    fun getNotesSortedByDateDesc(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY dateCreated ASC")
    fun getNotesSortedByDateAsc(folderId: Int): LiveData<List<Note>>
}