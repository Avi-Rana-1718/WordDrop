package com.worddrop.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Difficulty tier of a word (PRD §5.2). Stored by name. */
enum class Difficulty { EVERYDAY, ADVANCED, RARE }

@Entity(tableName = "words")
data class WordEntity(
    /** Stable slug, e.g. "ephemeral". Survives a future sync layer (Tech §7). */
    @PrimaryKey val id: String,
    val word: String,
    val phonetic: String?,
    val partOfSpeech: String,
    val difficulty: Difficulty,
    /** "business", "science", "literature"; null = general. */
    val category: String?,
    val definition: String,
    val exampleSentence: String,
    /** Stored as a JSON array string via [Converters]. */
    val synonyms: List<String>,
    /** Epoch millis; null = never shown. Drives the no-repeat cycle (Tech §4.2). */
    val lastShownAt: Long? = null,
)

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey val wordId: String,
    val savedAt: Long,
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    val wasCorrect: Boolean,
    val answeredAt: Long,
)

/**
 * The word currently on display. One row (id [WidgetStateEntity.SHARED]) is shared by every
 * widget instance and the Today screen so they can never disagree (Tech §3.4).
 */
@Entity(tableName = "widget_state")
data class WidgetStateEntity(
    @PrimaryKey val widgetId: Int = SHARED,
    val currentWordId: String,
    val lastRefreshedAt: Long,
) {
    companion object {
        const val SHARED = 0
    }
}
