package com.worddrop.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    /** Seed import: new rows only. Existing rows keep their lastShownAt. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(words: List<WordEntity>)

    /** Seed re-import on a version bump: refresh content without touching lastShownAt. */
    @Query(
        """
        UPDATE words SET word = :word, phonetic = :phonetic, partOfSpeech = :partOfSpeech,
            difficulty = :difficulty, category = :category, definition = :definition,
            exampleSentence = :exampleSentence, synonyms = :synonyms
        WHERE id = :id
        """,
    )
    suspend fun updateContent(
        id: String,
        word: String,
        phonetic: String?,
        partOfSpeech: String,
        difficulty: Difficulty,
        category: String?,
        definition: String,
        exampleSentence: String,
        synonyms: List<String>,
    )

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: String): WordEntity?

    @Query("SELECT * FROM words WHERE id = :id")
    fun observeById(id: String): Flow<WordEntity?>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<WordEntity>

    @Query("SELECT * FROM words WHERE word = :word COLLATE NOCASE LIMIT 1")
    suspend fun findByWord(word: String): WordEntity?

    /**
     * Least-recently-shown words matching the filter; never-shown first (Tech §4.2).
     * `:allCategories` short-circuits the category clause when the user has no category filter.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE difficulty IN (:difficulties)
          AND (:allCategories OR category IN (:categories) OR (:includeGeneral AND category IS NULL))
        ORDER BY (lastShownAt IS NOT NULL) ASC, lastShownAt ASC
        LIMIT :limit
        """,
    )
    suspend fun leastRecentlyShown(
        difficulties: List<Difficulty>,
        allCategories: Boolean,
        categories: List<String>,
        includeGeneral: Boolean,
        limit: Int,
    ): List<WordEntity>

    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE difficulty IN (:difficulties)
          AND (:allCategories OR category IN (:categories) OR (:includeGeneral AND category IS NULL))
        """,
    )
    suspend fun countMatching(
        difficulties: List<Difficulty>,
        allCategories: Boolean,
        categories: List<String>,
        includeGeneral: Boolean,
    ): Int

    @Query("UPDATE words SET lastShownAt = :shownAt WHERE id = :id")
    suspend fun markShown(id: String, shownAt: Long)

    @Query(
        """
        SELECT * FROM words WHERE lastShownAt IS NOT NULL AND id != :excludeId
        ORDER BY lastShownAt DESC LIMIT :limit
        """,
    )
    fun observeRecentlyShown(excludeId: String, limit: Int): Flow<List<WordEntity>>

    /** Quiz pool: everything the user has seen or saved. */
    @Query(
        """
        SELECT * FROM words
        WHERE lastShownAt IS NOT NULL OR id IN (SELECT wordId FROM saved_words)
        """,
    )
    suspend fun seenOrSaved(): List<WordEntity>

    @Query(
        """
        SELECT * FROM words WHERE partOfSpeech = :partOfSpeech AND id NOT IN (:excludeIds)
        ORDER BY RANDOM() LIMIT :limit
        """,
    )
    suspend fun randomWithPartOfSpeech(partOfSpeech: String, excludeIds: List<String>, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomAny(excludeIds: List<String>, limit: Int): List<WordEntity>

    @Query("SELECT DISTINCT category FROM words WHERE category IS NOT NULL ORDER BY category")
    fun observeCategories(): Flow<List<String>>
}

@Dao
interface SavedWordDao {

    @Query(
        """
        SELECT w.* FROM words w INNER JOIN saved_words s ON s.wordId = w.id
        ORDER BY s.savedAt DESC
        """,
    )
    fun observeSavedWords(): Flow<List<WordEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE wordId = :wordId)")
    fun observeIsSaved(wordId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM saved_words")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(saved: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE wordId = :wordId")
    suspend fun delete(wordId: String)
}

@Dao
interface QuizResultDao {

    @Insert
    suspend fun insert(result: QuizResultEntity)

    /** Words answered wrong since [since], most recent miss first. Feeds resurfacing (PRD §5.4). */
    @Query(
        """
        SELECT wordId FROM quiz_results WHERE wasCorrect = 0 AND answeredAt > :since
        GROUP BY wordId ORDER BY MAX(answeredAt) DESC
        """,
    )
    suspend fun recentlyMissedWordIds(since: Long): List<String>

    @Query("SELECT COUNT(*) FROM quiz_results")
    fun observeAnsweredCount(): Flow<Int>
}

@Dao
interface WidgetStateDao {

    @Query("SELECT * FROM widget_state WHERE widgetId = ${WidgetStateEntity.SHARED}")
    suspend fun get(): WidgetStateEntity?

    @Query("SELECT * FROM widget_state WHERE widgetId = ${WidgetStateEntity.SHARED}")
    fun observe(): Flow<WidgetStateEntity?>

    @Upsert
    suspend fun upsert(state: WidgetStateEntity)

    @Delete
    suspend fun delete(state: WidgetStateEntity)
}
