package com.example.noteapp.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "folders")
@Parcelize
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val folderName: String
) : Parcelable
