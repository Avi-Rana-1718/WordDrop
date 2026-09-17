package com.worddrop.app.data.repository

import com.worddrop.app.data.local.Difficulty
import com.worddrop.app.data.prefs.RefreshInterval
import com.worddrop.app.data.prefs.Settings
import com.worddrop.app.data.prefs.WordFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordRepositoryTest {

    private fun repo(
        dao: FakeWordDao,
        settings: Settings = Settings(filter = WordFilter(difficulties = Difficulty.entries.toSet())),
        notifier: RecordingNotifier = RecordingNotifier(),
        state: FakeWidgetStateDao = FakeWidgetStateDao(),
        seed: Int = 42,
    ) = DefaultWordRepository(dao, state, FakeSettings(settings), NoopSeeder, notifier, Random(seed))

    @Test
    fun `no word repeats until the whole filtered bank has been shown`() = runTest {
        val dao = FakeWordDao((1..12).map { word("w$it") })
        val repo = repo(dao)

        val firstCycle = (1..12).map { i -> repo.nextWord(now = 1_000L * i)!!.id }

        assertEquals("every word shown exactly once", 12, firstCycle.toSet().size)
    }

    @Test
    fun `after a full cycle the oldest-shown words come back first`() = runTest {
        val dao = FakeWordDao((1..6).map { word("w$it") })
        val repo = repo(dao)
        val first = (1..6).map { i -> repo.nextWord(now = 1_000L * i)!!.id }

        // Next pick must come from the 5 oldest-shown, never the one just shown.
        val next = repo.nextWord(now = 10_000L)!!.id
        assertNotEquals(first.last(), next)
        assertTrue(next in first.take(5))
    }

    @Test
    fun `difficulty filter is respected`() = runTest {
        val dao = FakeWordDao(
            listOf(
                word("easy1", Difficulty.EVERYDAY), word("easy2", Difficulty.EVERYDAY),
                word("hard1", Difficulty.RARE), word("hard2", Difficulty.RARE),
            ),
        )
        val repo = repo(dao, Settings(filter = WordFilter(difficulties = setOf(Difficulty.RARE))))

        repeat(6) { i ->
            val picked = repo.nextWord(now = 1_000L * (i + 1))!!
            assertEquals(Difficulty.RARE, picked.difficulty)
        }
    }

    @Test
    fun `category filter includes general words only when asked`() = runTest {
        val dao = FakeWordDao(
            listOf(
                word("biz", category = "business"), word("sci", category = "science"), word("gen", category = null),
            ),
        )
        val onlyBusiness = repo(dao, Settings(filter = WordFilter(difficulties = setOf(Difficulty.EVERYDAY), categories = setOf("business"))))
        assertEquals("biz", onlyBusiness.nextWord(now = 1)!!.id)
        assertEquals(1, onlyBusiness.countMatching(WordFilter(setOf(Difficulty.EVERYDAY), setOf("business"))))

        val bizAndGeneral = WordFilter(setOf(Difficulty.EVERYDAY), setOf("business", WordFilter.GENERAL))
        assertEquals(2, onlyBusiness.countMatching(bizAndGeneral))
    }

    @Test
    fun `filter that matches nothing yields null and leaves the current word alone`() = runTest {
        val dao = FakeWordDao(listOf(word("easy", Difficulty.EVERYDAY)))
        val state = FakeWidgetStateDao()
        val repo = repo(dao, Settings(filter = WordFilter(difficulties = setOf(Difficulty.RARE))), state = state)

        assertNull(repo.nextWord(now = 1))
        assertNull(state.get())
        assertNull(repo.currentWord())
    }

    @Test
    fun `refreshIfDue only advances once the interval has elapsed`() = runTest {
        val dao = FakeWordDao((1..5).map { word("w$it") })
        val notifier = RecordingNotifier()
        val settings = Settings(refreshInterval = RefreshInterval.H4, filter = WordFilter(difficulties = setOf(Difficulty.EVERYDAY)))
        val repo = repo(dao, settings, notifier)

        assertTrue("first call picks a word", repo.refreshIfDue(now = 0))
        val first = repo.currentWord()!!.id

        assertFalse(repo.refreshIfDue(now = RefreshInterval.H4.millis - 1))
        assertEquals(first, repo.currentWord()!!.id)

        assertTrue(repo.refreshIfDue(now = RefreshInterval.H4.millis))
        assertNotEquals(first, repo.currentWord()!!.id)
        assertEquals("widget poked once per change", 2, notifier.calls)
    }

    @Test
    fun `currentWord picks a word on first use and then stays stable`() = runTest {
        val dao = FakeWordDao((1..3).map { word("w$it") })
        val repo = repo(dao)
        val a = repo.currentWord()
        val b = repo.currentWord()
        assertNotNull(a)
        assertEquals(a!!.id, b!!.id)
    }
}
