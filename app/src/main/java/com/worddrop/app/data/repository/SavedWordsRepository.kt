package com.worddrop.app.data.repository

import com.worddrop.app.data.local.SavedWordDao
import com.worddrop.app.data.local.SavedWordEntity
import com.worddrop.app.data.local.WordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SavedWordsRepository {
    fun observeSaved(): Flow<List<WordEntity>>
    fun observeIsSaved(wordId: String): Flow<Boolean>
    fun observeCount(): Flow<Int>
    suspend fun save(wordId: String, at: Long = System.currentTimeMillis())
    suspend fun remove(wordId: String)
}

@Singleton
class DefaultSavedWordsRepository @Inject constructor(
    private val dao: SavedWordDao,
) : SavedWordsRepository {
    override fun observeSaved(): Flow<List<WordEntity>> = dao.observeSavedWords()
    override fun observeIsSaved(wordId: String): Flow<Boolean> = dao.observeIsSaved(wordId)
    override fun observeCount(): Flow<Int> = dao.observeCount()
    override suspend fun save(wordId: String, at: Long) = dao.insert(SavedWordEntity(wordId, at))
    override suspend fun remove(wordId: String) = dao.delete(wordId)
}
