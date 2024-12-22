package com.example.noteapp.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.Exclude

@Parcelize
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val folderName: String,
    val userId: String
) : Parcelable {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "folderName" to folderName,
            "userId" to userId
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Folder {
            return Folder(
                id = (map["id"] as Long?)?.toInt() ?: 0,
                folderName = map["folderName"] as String? ?: "Untitled",

                userId = map["userId"] as String? ?: ""
            )
        }
    }
}
