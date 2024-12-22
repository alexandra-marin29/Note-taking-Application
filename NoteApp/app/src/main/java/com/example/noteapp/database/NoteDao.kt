package com.example.noteapp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.noteapp.model.Note

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND userId = :userId ORDER BY isPinned DESC, id DESC")
    fun getAllNotesByFolder(folderId: Int, userId: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE (noteTitle LIKE :query OR noteDesc LIKE :query) AND folderId = :folderId AND userId = :userId ORDER BY isPinned DESC, id DESC")
    fun searchNoteInFolder(query: String?, folderId: Int, userId: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND userId = :userId ORDER BY isPinned DESC, noteTitle ASC")
    fun getNotesSortedByTitle(folderId: Int, userId: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND userId = :userId ORDER BY isPinned DESC, dateCreated DESC")
    fun getNotesSortedByDateDesc(folderId: Int, userId: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND userId = :userId ORDER BY isPinned DESC, dateCreated ASC")
    fun getNotesSortedByDateAsc(folderId: Int, userId: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND userId = :userId")
    suspend fun getNotesListByFolderId(folderId: Int, userId: String): List<Note>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): Note?

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    fun getNoteByIdLiveData(noteId: Int): LiveData<Note?>

    @Query("DELETE FROM notes WHERE folderId = :folderId AND userId = :userId")
    suspend fun deleteNotesByFolder(folderId: Int, userId: String)
}
