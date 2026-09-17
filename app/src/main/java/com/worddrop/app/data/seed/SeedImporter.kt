package com.worddrop.app.data.seed

import android.content.Context
import android.util.Log
import com.worddrop.app.data.local.Difficulty
import com.worddrop.app.data.local.WordDao
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SeedBank(val version: Int, val words: List<SeedWord>)

@Serializable
data class SeedWord(
    val id: String,
    val word: String,
    val phonetic: String? = null,
    val partOfSpeech: String,
    val difficulty: Difficulty,
    val category: String? = null,
    val definition: String,
    val example: String,
    val synonyms: List<String> = emptyList(),
)

/** Thrown when the bundled JSON is malformed. Fail loudly, never write a partial bank (Tech §9.3). */
class SeedFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface Seeder {
    /** Returns true when the word bank is present and up to date. Throws [SeedFormatException] on bad data. */
    suspend fun ensureSeeded(): Boolean
}

/**
 * Imports `assets/word_bank.json` into Room once, and again (content-only) when the asset's
 * version is higher than the one last imported. Safe to call from anywhere; concurrent callers
 * wait on the same import.
 */
@Singleton
class SeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordDao: WordDao,
    private val prefs: UserPreferences,
    private val json: Json,
) : Seeder {
    private val mutex = Mutex()

    @Volatile
    private var verifiedThisProcess = false

    override suspend fun ensureSeeded(): Boolean {
        if (verifiedThisProcess) return true
        return mutex.withLock {
            if (verifiedThisProcess) return@withLock true
            val importedVersion = prefs.seedVersion()
            val rowCount = wordDao.count()
            val bank = readBank()
            if (rowCount == 0 || bank.version > importedVersion) {
                importInto(bank, isUpdate = rowCount > 0)
                prefs.setSeedVersion(bank.version)
                Log.i(TAG, "Imported word bank v${bank.version} (${bank.words.size} words, update=${rowCount > 0})")
            }
            verifiedThisProcess = true
            true
        }
    }

    private suspend fun readBank(): SeedBank = withContext(Dispatchers.IO) {
        val text = try {
            context.assets.open(ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw SeedFormatException("Missing asset $ASSET", e)
        }
        val bank = try {
            json.decodeFromString(SeedBank.serializer(), text)
        } catch (e: Exception) {
            throw SeedFormatException("word_bank.json is not valid", e)
        }
        validate(bank)
        bank
    }

    private fun validate(bank: SeedBank) {
        if (bank.words.isEmpty()) throw SeedFormatException("word_bank.json has no words")
        val ids = HashSet<String>()
        bank.words.forEach { w ->
            if (w.id.isBlank() || w.word.isBlank() || w.definition.isBlank() || w.example.isBlank()) {
                throw SeedFormatException("Word '${w.id}' is missing required fields")
            }
            if (!ids.add(w.id)) throw SeedFormatException("Duplicate word id '${w.id}'")
        }
    }

    private suspend fun importInto(bank: SeedBank, isUpdate: Boolean) {
        val entities = bank.words.map { it.toEntity() }
        wordDao.insertIgnore(entities)
        if (isUpdate) {
            entities.forEach { e ->
                wordDao.updateContent(
                    id = e.id, word = e.word, phonetic = e.phonetic, partOfSpeech = e.partOfSpeech,
                    difficulty = e.difficulty, category = e.category, definition = e.definition,
                    exampleSentence = e.exampleSentence, synonyms = e.synonyms,
                )
            }
        }
    }

    private fun SeedWord.toEntity() = WordEntity(
        id = id,
        word = word,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        difficulty = difficulty,
        category = category,
        definition = definition,
        exampleSentence = example,
        synonyms = synonyms,
    )

    companion object {
        private const val TAG = "SeedImporter"
        const val ASSET = "word_bank.json"
    }
}
