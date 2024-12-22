package com.example.noteapp.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.Exclude

@Parcelize
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteTitle: String,
    val noteDesc: String,
    val noteColor: String = "#FFFFFFFF",
    val dateCreated: Long,
    val folderId: Int,
    val isPinned: Boolean = false,
    val reminderTime: Long? = null,
    val imageUris: String = "[]",
    val urls: String = "[]"
) : Parcelable {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "noteTitle" to noteTitle,
            "noteDesc" to noteDesc,
            "noteColor" to noteColor,
            "dateCreated" to dateCreated,
            "folderId" to folderId,
            "isPinned" to isPinned,
            "reminderTime" to reminderTime,
            "imageUris" to imageUris,
            "urls" to urls
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Note {
            return Note(
                id = (map["id"] as Long?)?.toInt() ?: 0,
                noteTitle = map["noteTitle"] as String? ?: "",
                noteDesc = map["noteDesc"] as String? ?: "",
                noteColor = map["noteColor"] as String? ?: "#FFFFFFFF",
                dateCreated = map["dateCreated"] as Long? ?: System.currentTimeMillis(),
                folderId = (map["folderId"] as Long?)?.toInt() ?: 0,
                isPinned = map["isPinned"] as Boolean? ?: false,
                reminderTime = map["reminderTime"] as Long?,
                imageUris = map["imageUris"] as String? ?: "[]",
                urls = map["urls"] as String? ?: "[]"
            )
        }
    }
}
