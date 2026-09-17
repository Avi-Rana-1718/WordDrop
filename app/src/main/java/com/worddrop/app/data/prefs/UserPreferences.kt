package com.worddrop.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.worddrop.app.data.local.Difficulty
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** How often the widget advances to a new word (PRD §5.1). */
enum class RefreshInterval(val hours: Long) {
    H4(4), H12(12), H24(24);

    val millis: Long get() = hours * 60L * 60L * 1000L
}

/** Which words are eligible to be shown (PRD §5.2). Empty [categories] = all categories. */
data class WordFilter(
    val difficulties: Set<Difficulty> = setOf(Difficulty.EVERYDAY, Difficulty.ADVANCED),
    val categories: Set<String> = emptySet(),
) {
    companion object {
        /** Pseudo-category id for words with a null category. */
        const val GENERAL = "general"
    }
}

data class Settings(
    val refreshInterval: RefreshInterval = RefreshInterval.H12,
    val filter: WordFilter = WordFilter(),
    val reminderEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val batteryTipDismissed: Boolean = false,
)

/** Read-only view of settings for the data layer; lets repositories be unit-tested without DataStore. */
interface SettingsProvider {
    suspend fun current(): Settings
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "worddrop_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) : SettingsProvider {

    private object Keys {
        val REFRESH = stringPreferencesKey("refresh_interval")
        val DIFFICULTIES = stringSetPreferencesKey("difficulties")
        val CATEGORIES = stringSetPreferencesKey("categories")
        val REMINDER = booleanPreferencesKey("reminder_enabled")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val BATTERY_TIP = booleanPreferencesKey("battery_tip_dismissed")
        val SEED_VERSION = intPreferencesKey("seed_version")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            refreshInterval = p[Keys.REFRESH]?.let { runCatching { RefreshInterval.valueOf(it) }.getOrNull() }
                ?: RefreshInterval.H12,
            filter = WordFilter(
                difficulties = p[Keys.DIFFICULTIES]
                    ?.mapNotNull { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                    ?.toSet()
                    ?.takeIf { it.isNotEmpty() }
                    ?: WordFilter().difficulties,
                categories = p[Keys.CATEGORIES] ?: emptySet(),
            ),
            reminderEnabled = p[Keys.REMINDER] ?: false,
            onboardingDone = p[Keys.ONBOARDED] ?: false,
            batteryTipDismissed = p[Keys.BATTERY_TIP] ?: false,
        )
    }

    override suspend fun current(): Settings = settings.first()

    suspend fun setRefreshInterval(interval: RefreshInterval) {
        context.dataStore.edit { it[Keys.REFRESH] = interval.name }
    }

    /** Ignores an empty set: at least one tier must stay on (UI/UX spec §10). */
    suspend fun setDifficulties(difficulties: Set<Difficulty>) {
        if (difficulties.isEmpty()) return
        context.dataStore.edit { it[Keys.DIFFICULTIES] = difficulties.map { d -> d.name }.toSet() }
    }

    suspend fun setCategories(categories: Set<String>) {
        context.dataStore.edit { it[Keys.CATEGORIES] = categories }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER] = enabled }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }

    suspend fun dismissBatteryTip() {
        context.dataStore.edit { it[Keys.BATTERY_TIP] = true }
    }

    suspend fun seedVersion(): Int = context.dataStore.data.first()[Keys.SEED_VERSION] ?: 0

    suspend fun setSeedVersion(version: Int) {
        context.dataStore.edit { it[Keys.SEED_VERSION] = version }
    }
}
