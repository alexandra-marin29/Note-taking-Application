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


    suspend fun deleteFolder(folder: Folder) {
        db.getFolderDao().deleteFolder(folder)

        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .collection("folders").document(folder.id.toString())
            .delete()
            .await()
    }

    fun getAllFolders(): LiveData<List<Folder>> = db.getFolderDao().getAllFolders()
    suspend fun getFolderByName(name: String): Folder? = db.getFolderDao().getFolderByName(name)
    suspend fun getFolderById(id: Int): Folder? = db.getFolderDao().getFolderById(id)
    fun getFolderByIdLiveData(id: Int): LiveData<Folder?> = db.getFolderDao().getFolderByIdLiveData(id)
    fun getAllNotesByFolder(folderId: Int): LiveData<List<Note>> = db.getNoteDao().getAllNotesByFolder(folderId)
    fun searchNoteInFolder(query: String?, folderId: Int): LiveData<List<Note>> =
        db.getNoteDao().searchNoteInFolder(query, folderId)

    fun getNotesSortedByTitle(folderId: Int): LiveData<List<Note>> =
        db.getNoteDao().getNotesSortedByTitle(folderId)

    fun getNotesSortedByDateDesc(folderId: Int): LiveData<List<Note>> =
        db.getNoteDao().getNotesSortedByDateDesc(folderId)

    fun getNotesSortedByDateAsc(folderId: Int): LiveData<List<Note>> =
        db.getNoteDao().getNotesSortedByDateAsc(folderId)

    suspend fun getNotesListByFolderId(folderId: Int): List<Note> =
        db.getNoteDao().getNotesListByFolderId(folderId)

    fun getNoteByIdLiveData(id: Int): LiveData<Note?> = db.getNoteDao().getNoteByIdLiveData(id)

    suspend fun deleteNotesByFolder(folderId: Int) = db.getNoteDao().deleteNotesByFolder(folderId)

    suspend fun syncLocalWithFirestore() {
        val userId = auth.currentUser?.uid ?: return

        val foldersSnapshot = firestore.collection("users").document(userId)
            .collection("folders").get().await()

        for (folderDoc in foldersSnapshot.documents) {
            val folder = Folder.fromMap(folderDoc.data ?: emptyMap())
            db.getFolderDao().insertFolder(folder)

            val notesSnapshot = firestore.collection("users").document(userId)
                .collection("folders").document(folder.id.toString())
                .collection("notes").get().await()

            for (noteDoc in notesSnapshot.documents) {
                val note = Note.fromMap(noteDoc.data ?: emptyMap())
                db.getNoteDao().insertNote(note)
            }
        }
    }
}
