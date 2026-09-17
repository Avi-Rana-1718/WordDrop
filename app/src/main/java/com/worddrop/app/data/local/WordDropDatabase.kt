package com.worddrop.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WordEntity::class, SavedWordEntity::class, QuizResultEntity::class, WidgetStateEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WordDropDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun savedWordDao(): SavedWordDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun widgetStateDao(): WidgetStateDao

    companion object {
        const val NAME = "worddrop.db"
    }
}
