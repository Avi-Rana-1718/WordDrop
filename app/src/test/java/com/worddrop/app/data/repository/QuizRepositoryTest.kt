package com.worddrop.app.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizRepositoryTest {

    private fun bank(n: Int, pos: String = "adjective") = (1..n).map { word("w$it", partOfSpeech = pos, lastShownAt = 1_000L * it) }

    @Test
    fun `needs at least four eligible words`() = runTest {
        val dao = FakeWordDao(bank(3))
        val repo = DefaultQuizRepository(dao, FakeQuizResultDao(), Random(1))
        assertTrue(repo.buildQuiz().isEmpty())
    }

    @Test
    fun `each question has four distinct options including the answer`() = runTest {
        val dao = FakeWordDao(bank(10))
        val repo = DefaultQuizRepository(dao, FakeQuizResultDao(), Random(1))
        val quiz = repo.buildQuiz(count = 8)

        assertEquals(8, quiz.size)
        quiz.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.map { it.id }.toSet().size)
            assertTrue(q.options.any { it.id == q.answer.id })
        }
        assertEquals("no answer asked twice", 8, quiz.map { it.answer.id }.toSet().size)
    }

    @Test
    fun `distractors fall back to other parts of speech when needed`() = runTest {
        val dao = FakeWordDao(bank(2, pos = "noun") + bank(4, pos = "verb").map { it.copy(id = it.id + "v", word = it.word + "v") })
        val repo = DefaultQuizRepository(dao, FakeQuizResultDao(), Random(1))
        val quiz = repo.buildQuiz(count = 6)
        assertTrue(quiz.all { it.options.size == 4 })
    }

    @Test
    fun `recently missed words come first`() = runTest {
        val dao = FakeWordDao(bank(10))
        val results = FakeQuizResultDao()
        val repo = DefaultQuizRepository(dao, results, Random(7))
        val now = 100_000L
        repo.record("w7", wasCorrect = false, at = now - 1_000)
        repo.record("w3", wasCorrect = false, at = now - 500)
        repo.record("w9", wasCorrect = true, at = now - 200)

        val quiz = repo.buildQuiz(count = 4, now = now)
        assertEquals(listOf("w3", "w7"), quiz.take(2).map { it.answer.id })
    }
}
