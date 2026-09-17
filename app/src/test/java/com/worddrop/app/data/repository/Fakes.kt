package com.worddrop.app.data.repository

import com.worddrop.app.data.local.Difficulty
import com.worddrop.app.data.local.QuizResultDao
import com.worddrop.app.data.local.QuizResultEntity
import com.worddrop.app.data.local.WidgetStateDao
import com.worddrop.app.data.local.WidgetStateEntity
import com.worddrop.app.data.local.WordDao
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.prefs.Settings
import com.worddrop.app.data.prefs.SettingsProvider
import com.worddrop.app.data.seed.Seeder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun word(
    id: String,
    difficulty: Difficulty = Difficulty.EVERYDAY,
    category: String? = null,
    partOfSpeech: String = "adjective",
    lastShownAt: Long? = null,
) = WordEntity(
    id = id, word = id, phonetic = null, partOfSpeech = partOfSpeech, difficulty = difficulty,
    category = category, definition = "def $id", exampleSentence = "ex $id", synonyms = emptyList(),
    lastShownAt = lastShownAt,
)

/** In-memory WordDao that mirrors the SQL in Daos.kt closely enough for selection tests. */
class FakeWordDao(initial: List<WordEntity> = emptyList()) : WordDao {
    val rows = MutableStateFlow(initial.associateBy { it.id })
    private val all get() = rows.value.values

    override suspend fun count() = rows.value.size

    override suspend fun insertIgnore(words: List<WordEntity>) {
        rows.value = rows.value + words.filter { it.id !in rows.value }.associateBy { it.id }
    }

    override suspend fun updateContent(
        id: String, word: String, phonetic: String?, partOfSpeech: String, difficulty: Difficulty,
        category: String?, definition: String, exampleSentence: String, synonyms: List<String>,
    ) {
        rows.value[id]?.let { old ->
            rows.value = rows.value + (id to old.copy(word = word, phonetic = phonetic, partOfSpeech = partOfSpeech, difficulty = difficulty, category = category, definition = definition, exampleSentence = exampleSentence, synonyms = synonyms))
        }
    }

    override suspend fun getById(id: String) = rows.value[id]
    override fun observeById(id: String): Flow<WordEntity?> = rows.map { it[id] }
    override suspend fun getByIds(ids: List<String>) = ids.mapNotNull { rows.value[it] }
    override suspend fun findByWord(word: String) = all.firstOrNull { it.word.equals(word, ignoreCase = true) }

    private fun matching(difficulties: List<Difficulty>, allCategories: Boolean, categories: List<String>, includeGeneral: Boolean) =
        all.filter { w ->
            w.difficulty in difficulties &&
                (allCategories || w.category in categories || (includeGeneral && w.category == null))
        }

    override suspend fun leastRecentlyShown(difficulties: List<Difficulty>, allCategories: Boolean, categories: List<String>, includeGeneral: Boolean, limit: Int) =
        matching(difficulties, allCategories, categories, includeGeneral)
            .sortedWith(compareBy<WordEntity> { it.lastShownAt != null }.thenBy { it.lastShownAt ?: 0L })
            .take(limit)

    override suspend fun countMatching(difficulties: List<Difficulty>, allCategories: Boolean, categories: List<String>, includeGeneral: Boolean) =
        matching(difficulties, allCategories, categories, includeGeneral).size

    override suspend fun markShown(id: String, shownAt: Long) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(lastShownAt = shownAt)) }
    }

    override fun observeRecentlyShown(excludeId: String, limit: Int): Flow<List<WordEntity>> = rows.map { m ->
        m.values.filter { it.lastShownAt != null && it.id != excludeId }.sortedByDescending { it.lastShownAt }.take(limit)
    }

    override suspend fun seenOrSaved() = all.filter { it.lastShownAt != null }

    override suspend fun randomWithPartOfSpeech(partOfSpeech: String, excludeIds: List<String>, limit: Int) =
        all.filter { it.partOfSpeech == partOfSpeech && it.id !in excludeIds }.shuffled().take(limit)

    override suspend fun randomAny(excludeIds: List<String>, limit: Int) = all.filter { it.id !in excludeIds }.shuffled().take(limit)

    override fun observeCategories(): Flow<List<String>> = rows.map { m -> m.values.mapNotNull { it.category }.distinct().sorted() }
}

class FakeWidgetStateDao : WidgetStateDao {
    val state = MutableStateFlow<WidgetStateEntity?>(null)
    override suspend fun get() = state.value
    override fun observe(): Flow<WidgetStateEntity?> = state
    override suspend fun upsert(state: WidgetStateEntity) { this.state.value = state }
    override suspend fun delete(state: WidgetStateEntity) { this.state.value = null }
}

class FakeQuizResultDao : QuizResultDao {
    val results = mutableListOf<QuizResultEntity>()
    override suspend fun insert(result: QuizResultEntity) { results += result.copy(id = results.size + 1L) }
    override suspend fun recentlyMissedWordIds(since: Long): List<String> =
        results.filter { !it.wasCorrect && it.answeredAt > since }
            .groupBy { it.wordId }
            .entries.sortedByDescending { e -> e.value.maxOf { it.answeredAt } }
            .map { it.key }
    override fun observeAnsweredCount(): Flow<Int> = MutableStateFlow(results.size)
}

class FakeSettings(var settings: Settings = Settings()) : SettingsProvider {
    override suspend fun current() = settings
}

object NoopSeeder : Seeder {
    override suspend fun ensureSeeded() = true
}

class RecordingNotifier : WidgetNotifier {
    var calls = 0
    override suspend fun widgetContentChanged() { calls++ }
}
