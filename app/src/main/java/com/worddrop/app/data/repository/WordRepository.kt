package com.worddrop.app.data.repository

import com.worddrop.app.data.local.WidgetStateDao
import com.worddrop.app.data.local.WidgetStateEntity
import com.worddrop.app.data.local.WordDao
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.prefs.SettingsProvider
import com.worddrop.app.data.prefs.WordFilter
import com.worddrop.app.data.seed.Seeder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Lets the data layer poke the home-screen widget without depending on Glance. */
fun interface WidgetNotifier {
    suspend fun widgetContentChanged()
}

/**
 * Single source of truth for "which word is showing right now" (Tech §3.4) and for picking the
 * next one (Tech §4.2). Used by the widget, the worker and the Today screen alike.
 */
interface WordRepository {
    /** The word on display. Emits null until the first word has been chosen. */
    fun observeCurrentWord(): Flow<WordEntity?>

    /** The word on display, choosing one if none has been chosen yet. Null only if the bank is empty for the filter. */
    suspend fun currentWord(): WordEntity?

    /** Advances to a new word if the user's refresh interval has elapsed. Returns true if it advanced. */
    suspend fun refreshIfDue(now: Long = System.currentTimeMillis()): Boolean

    /** Advances to a new word unconditionally (manual refresh / "Next word"). */
    suspend fun nextWord(now: Long = System.currentTimeMillis()): WordEntity?

    suspend fun getWord(id: String): WordEntity?
    fun observeWord(id: String): Flow<WordEntity?>
    suspend fun findByWord(word: String): WordEntity?

    fun observeRecentlyShown(limit: Int): Flow<List<WordEntity>>
    suspend fun countMatching(filter: WordFilter): Int
    fun observeCategories(): Flow<List<String>>
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultWordRepository @Inject constructor(
    private val wordDao: WordDao,
    private val widgetStateDao: WidgetStateDao,
    private val prefs: SettingsProvider,
    private val seedImporter: Seeder,
    private val widgetNotifier: WidgetNotifier,
    private val random: Random,
) : WordRepository {

    private val advanceMutex = Mutex()

    override fun observeCurrentWord(): Flow<WordEntity?> =
        widgetStateDao.observe().flatMapLatest { state ->
            if (state == null) flowOf(null) else wordDao.observeById(state.currentWordId)
        }

    override suspend fun currentWord(): WordEntity? {
        seedImporter.ensureSeeded()
        val state = widgetStateDao.get()
        val existing = state?.let { wordDao.getById(it.currentWordId) }
        if (existing != null) return existing
        return advance(System.currentTimeMillis())
    }

    override suspend fun refreshIfDue(now: Long): Boolean {
        seedImporter.ensureSeeded()
        val state = widgetStateDao.get() ?: run { advance(now); return true }
        val interval = prefs.current().refreshInterval.millis
        if (now - state.lastRefreshedAt < interval) return false
        return advance(now) != null
    }

    override suspend fun nextWord(now: Long): WordEntity? {
        seedImporter.ensureSeeded()
        return advance(now)
    }

    /**
     * Pick from the [CANDIDATE_WINDOW] least-recently-shown words so the order isn't strictly
     * deterministic, stamp it, and make it current. Never-shown words always win over shown ones,
     * so nothing repeats until the whole filtered bank has cycled; after that the same ordering
     * keeps going from the oldest — no explicit reset (Tech §4.2).
     */
    private suspend fun advance(now: Long): WordEntity? = advanceMutex.withLock {
        val filter = prefs.current().filter
        val currentId = widgetStateDao.get()?.currentWordId
        val candidates = filter.query { d, all, cats, general ->
            wordDao.leastRecentlyShown(d, all, cats, general, CANDIDATE_WINDOW)
        }
        if (candidates.isEmpty()) return@withLock null
        val unseen = candidates.filter { it.lastShownAt == null }
        val pool = unseen.ifEmpty { candidates.filter { it.id != currentId }.ifEmpty { candidates } }
        val pick = pool[random.nextInt(pool.size)]
        wordDao.markShown(pick.id, now)
        widgetStateDao.upsert(WidgetStateEntity(currentWordId = pick.id, lastRefreshedAt = now))
        widgetNotifier.widgetContentChanged()
        pick.copy(lastShownAt = now)
    }

    override suspend fun getWord(id: String): WordEntity? = wordDao.getById(id)

    override fun observeWord(id: String): Flow<WordEntity?> = wordDao.observeById(id)

    override suspend fun findByWord(word: String): WordEntity? = wordDao.findByWord(word.trim())

    override fun observeRecentlyShown(limit: Int): Flow<List<WordEntity>> =
        widgetStateDao.observe().flatMapLatest { state ->
            wordDao.observeRecentlyShown(excludeId = state?.currentWordId ?: "", limit = limit)
        }

    override suspend fun countMatching(filter: WordFilter): Int =
        filter.query { d, all, cats, general -> wordDao.countMatching(d, all, cats, general) }

    override fun observeCategories(): Flow<List<String>> = wordDao.observeCategories()

    companion object {
        const val CANDIDATE_WINDOW = 5
    }
}

/** Translates a [WordFilter] into the four DAO parameters used by the filtered queries. */
internal suspend inline fun <T> WordFilter.query(
    block: (
        difficulties: List<com.worddrop.app.data.local.Difficulty>,
        allCategories: Boolean,
        categories: List<String>,
        includeGeneral: Boolean,
    ) -> T,
): T {
    val all = categories.isEmpty()
    val named = categories.filter { it != WordFilter.GENERAL }
    val general = WordFilter.GENERAL in categories
    return block(difficulties.toList(), all, named.ifEmpty { listOf("") }, general)
}
