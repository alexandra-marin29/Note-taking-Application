package com.example.noteapp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.noteapp.model.Folder

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Query("SELECT * FROM folders WHERE userId = :userId ORDER BY folderName ASC")
    fun getAllFoldersByUser(userId: String): LiveData<List<Folder>>

    @Query("SELECT * FROM folders WHERE folderName = :name AND userId = :userId LIMIT 1")
    suspend fun getFolderByName(name: String, userId: String): Folder?

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Int): Folder?

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun getFolderByIdLiveData(id: Int): LiveData<Folder?>
}
