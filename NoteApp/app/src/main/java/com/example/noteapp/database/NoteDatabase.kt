package com.example.noteapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.noteapp.model.Folder
import com.example.noteapp.model.Note
import androidx.room.migration.Migration

@Database(entities = [Note::class, Folder::class], version = 7)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun getNoteDao(): NoteDao
    abstract fun getFolderDao(): FolderDao

    companion object {
        @Volatile
        private var instance: NoteDatabase? = null
        private val LOCK = Any()

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN reminderTime INTEGER")
            }
        }

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also { instance = it }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                "note_db"
            )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO folders (id, folderName) VALUES (1, 'Notes')")
                    }
                })
                .build()
    }
}
