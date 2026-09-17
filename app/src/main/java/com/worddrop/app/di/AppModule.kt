package com.worddrop.app.di

import android.content.Context
import androidx.room.Room
import com.worddrop.app.data.local.QuizResultDao
import com.worddrop.app.data.local.SavedWordDao
import com.worddrop.app.data.local.WidgetStateDao
import com.worddrop.app.data.local.WordDao
import com.worddrop.app.data.local.WordDropDatabase
import com.worddrop.app.data.repository.DefaultQuizRepository
import com.worddrop.app.data.repository.DefaultSavedWordsRepository
import com.worddrop.app.data.repository.DefaultWordRepository
import com.worddrop.app.data.repository.QuizRepository
import com.worddrop.app.data.repository.SavedWordsRepository
import com.worddrop.app.data.repository.WidgetNotifier
import com.worddrop.app.data.repository.WordRepository
import com.worddrop.app.data.prefs.SettingsProvider
import com.worddrop.app.data.prefs.UserPreferences
import com.worddrop.app.data.seed.SeedImporter
import com.worddrop.app.data.seed.Seeder
import com.worddrop.app.widget.GlanceWidgetNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): WordDropDatabase =
        Room.databaseBuilder(context, WordDropDatabase::class.java, WordDropDatabase.NAME).build()

    @Provides fun wordDao(db: WordDropDatabase): WordDao = db.wordDao()
    @Provides fun savedWordDao(db: WordDropDatabase): SavedWordDao = db.savedWordDao()
    @Provides fun quizResultDao(db: WordDropDatabase): QuizResultDao = db.quizResultDao()
    @Provides fun widgetStateDao(db: WordDropDatabase): WidgetStateDao = db.widgetStateDao()

    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    fun random(): Random = Random.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun wordRepository(impl: DefaultWordRepository): WordRepository
    @Binds abstract fun savedWordsRepository(impl: DefaultSavedWordsRepository): SavedWordsRepository
    @Binds abstract fun quizRepository(impl: DefaultQuizRepository): QuizRepository
    @Binds abstract fun widgetNotifier(impl: GlanceWidgetNotifier): WidgetNotifier
    @Binds abstract fun settingsProvider(impl: UserPreferences): SettingsProvider
    @Binds abstract fun seeder(impl: SeedImporter): Seeder
}
