package com.worddrop.app.data.repository

import com.worddrop.app.data.local.QuizResultDao
import com.worddrop.app.data.local.QuizResultEntity
import com.worddrop.app.data.local.WordDao
import com.worddrop.app.data.local.WordEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** One multiple-choice question: the definition is shown, the user picks the word. */
data class QuizQuestion(
    val answer: WordEntity,
    /** Four options including [answer], already shuffled. */
    val options: List<WordEntity>,
)

interface QuizRepository {
    /**
     * Builds up to [count] questions. Returns an empty list when fewer than [MIN_POOL] words are
     * eligible (UI/UX spec §10). Words missed in the last [MISS_WINDOW_DAYS] days come first
     * (PRD §5.4 "resurface sooner"), then saved, then recently shown.
     */
    suspend fun buildQuiz(count: Int = DEFAULT_COUNT, now: Long = System.currentTimeMillis()): List<QuizQuestion>

    suspend fun record(wordId: String, wasCorrect: Boolean, at: Long = System.currentTimeMillis())

    companion object {
        const val DEFAULT_COUNT = 8
        const val OPTIONS = 4
        const val MIN_POOL = 4
        const val MISS_WINDOW_DAYS = 14
    }
}

@Singleton
class DefaultQuizRepository @Inject constructor(
    private val wordDao: WordDao,
    private val quizResultDao: QuizResultDao,
    private val random: Random,
) : QuizRepository {

    override suspend fun buildQuiz(count: Int, now: Long): List<QuizQuestion> {
        val pool = wordDao.seenOrSaved()
        if (pool.size < QuizRepository.MIN_POOL) return emptyList()

        val since = now - QuizRepository.MISS_WINDOW_DAYS * 24L * 60L * 60L * 1000L
        val missed = quizResultDao.recentlyMissedWordIds(since).toSet()
        val byId = pool.associateBy { it.id }

        // Missed words first (in their recency order), then everything else shuffled.
        val ordered = buildList {
            missed.forEach { id -> byId[id]?.let(::add) }
            addAll(pool.filter { it.id !in missed }.shuffled(random))
        }

        return ordered.take(count).map { answer -> question(answer) }
    }

    private suspend fun question(answer: WordEntity): QuizQuestion {
        val needed = QuizRepository.OPTIONS - 1
        val samePos = wordDao.randomWithPartOfSpeech(answer.partOfSpeech, listOf(answer.id), needed)
        val distractors = if (samePos.size == needed) {
            samePos
        } else {
            samePos + wordDao.randomAny(listOf(answer.id) + samePos.map { it.id }, needed - samePos.size)
        }
        return QuizQuestion(answer = answer, options = (distractors + answer).shuffled(random))
    }

    override suspend fun record(wordId: String, wasCorrect: Boolean, at: Long) {
        quizResultDao.insert(QuizResultEntity(wordId = wordId, wasCorrect = wasCorrect, answeredAt = at))
    }
}
