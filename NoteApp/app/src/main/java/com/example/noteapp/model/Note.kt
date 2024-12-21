package com.example.noteapp.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

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
@Parcelize
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val noteTitle: String,
    val noteDesc: String,
    val noteColor: String = "#FFFFFFFF",
    val dateCreated: Long,
    val folderId: Int,
    val isPinned: Boolean = false,
    val reminderTime: Long? = null,
    val imageUris: String = "[]",
    val urls: String = "[]"
) : Parcelable
