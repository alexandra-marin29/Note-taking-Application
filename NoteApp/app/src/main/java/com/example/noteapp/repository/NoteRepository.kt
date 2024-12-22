package com.example.noteapp.repository

import androidx.lifecycle.LiveData
import com.example.noteapp.database.NoteDatabase
import com.example.noteapp.model.Folder
import com.example.noteapp.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NoteRepository(private val db: NoteDatabase) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val noteDao = db.getNoteDao()

    suspend fun insertNote(note: Note): Long {
        val localId = db.getNoteDao().insertNote(note)

        val userId = auth.currentUser?.uid ?: return localId
        val noteWithId = note.copy(id = localId.toInt())
        val noteMap = noteWithId.toMap()

        firestore.collection("users").document(userId)
            .collection("folders").document(note.folderId.toString())
            .collection("notes").document(localId.toString())
            .set(noteMap)
            .await()

        return localId
    }

    suspend fun updateNote(note: Note) {
        db.getNoteDao().updateNote(note)

        val userId = auth.currentUser?.uid ?: return
        val noteMap = note.toMap()

        firestore.collection("users").document(userId)
            .collection("folders").document(note.folderId.toString())
            .collection("notes").document(note.id.toString())
            .set(noteMap)
            .await()
    }

    suspend fun deleteNote(note: Note) {
        db.getNoteDao().deleteNote(note)

        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("folders").document(note.folderId.toString())
            .collection("notes").document(note.id.toString())
            .delete()
            .await()
    }

    suspend fun insertFolder(folder: Folder) {
        val localId = db.getFolderDao().insertFolder(folder)

        val userId = auth.currentUser?.uid ?: return
        val folderWithId = folder.copy(id = localId.toInt())
        val folderMap = folderWithId.toMap()

        firestore.collection("users").document(userId)
            .collection("folders").document(localId.toString())
            .set(folderMap)
            .await()
    }

    suspend fun getActiveReminders(currentTime: Long, userId: String): List<Note> {
        return noteDao.getActiveReminders(currentTime, userId)
    }

    suspend fun deleteFolder(folder: Folder) {
        db.getFolderDao().deleteFolder(folder)

        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("folders").document(folder.id.toString())
            .delete()
            .await()
    }

    fun getAllFoldersByUserId(userId: String): LiveData<List<Folder>> {
        return db.getFolderDao().getAllFoldersByUser(userId)
    }

    suspend fun getFolderByName(name: String, userId: String): Folder? {
        return db.getFolderDao().getFolderByName(name, userId)
    }

    suspend fun getFolderById(id: Int): Folder? = db.getFolderDao().getFolderById(id)
    fun getFolderByIdLiveData(id: Int): LiveData<Folder?> = db.getFolderDao().getFolderByIdLiveData(id)

    fun getAllNotesByFolder(folderId: Int, userId: String): LiveData<List<Note>> {
        return db.getNoteDao().getAllNotesByFolder(folderId, userId)
    }

    fun searchNoteInFolder(query: String?, folderId: Int, userId: String): LiveData<List<Note>> {
        return db.getNoteDao().searchNoteInFolder(query, folderId, userId)
    }

    fun getNotesSortedByTitle(folderId: Int, userId: String): LiveData<List<Note>> {
        return db.getNoteDao().getNotesSortedByTitle(folderId, userId)
    }

    fun getNotesSortedByDateDesc(folderId: Int, userId: String): LiveData<List<Note>> {
        return db.getNoteDao().getNotesSortedByDateDesc(folderId, userId)
    }

    fun getNotesSortedByDateAsc(folderId: Int, userId: String): LiveData<List<Note>> {
        return db.getNoteDao().getNotesSortedByDateAsc(folderId, userId)
    }

    suspend fun getNotesListByFolderId(folderId: Int, userId: String): List<Note> {
        return db.getNoteDao().getNotesListByFolderId(folderId, userId)
    }

    fun getNoteByIdLiveData(noteId: Int): LiveData<Note?> = db.getNoteDao().getNoteByIdLiveData(noteId)

    suspend fun deleteNotesByFolder(folderId: Int, userId: String) {
        db.getNoteDao().deleteNotesByFolder(folderId, userId)
    }

    suspend fun syncLocalWithFirestore() {
        val userId = auth.currentUser?.uid ?: return

        val foldersSnapshot = firestore.collection("users").document(userId)
            .collection("folders").get().await()

        for (folderDoc in foldersSnapshot.documents) {
            val folder = Folder.fromMap(folderDoc.data ?: emptyMap())

            val folderToInsert = folder.copy(userId = userId)

            db.getFolderDao().insertFolder(folderToInsert)

            val notesSnapshot = firestore.collection("users").document(userId)
                .collection("folders").document(folder.id.toString())
                .collection("notes").get().await()

            for (noteDoc in notesSnapshot.documents) {
                val note = Note.fromMap(noteDoc.data ?: emptyMap())

                val noteToInsert = note.copy(userId = userId)
                db.getNoteDao().insertNote(noteToInsert)
            }
        }
    }
}
