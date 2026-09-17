package com.worddrop.app.widget

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.worddrop.app.data.prefs.RefreshInterval
import com.worddrop.app.data.prefs.UserPreferences
import com.worddrop.app.data.repository.WordRepository
import com.worddrop.app.data.seed.SeedFormatException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Periodic job that advances the widget word when the user's interval has elapsed (Tech §3.3).
 * The interval check lives in the repository, so an OEM delaying this job only delays the
 * change — it never shows a broken widget.
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val wordRepository: WordRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val advanced = wordRepository.refreshIfDue()
        Log.d(TAG, "Periodic refresh ran; advanced=$advanced")
        Result.success()
    } catch (e: SeedFormatException) {
        Log.e(TAG, "Word bank unreadable; giving up", e)
        Result.failure()
    } catch (e: Exception) {
        Log.w(TAG, "Refresh failed; will retry", e)
        Result.retry()
    }

    companion object {
        const val TAG = "WidgetUpdateWorker"
        const val UNIQUE_NAME = "worddrop.widget.refresh"
    }
}

/** Owns the WorkManager schedule for [WidgetUpdateWorker]. */
@Singleton
class WidgetScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Enqueue with KEEP so a running schedule is untouched. Called from Application.onCreate. */
    fun ensureScheduled() {
        scope.launch { enqueue(prefs.current().refreshInterval, ExistingPeriodicWorkPolicy.KEEP) }
    }

    /** Re-enqueue with UPDATE when the user changes the interval in Settings. */
    fun reschedule(interval: RefreshInterval) {
        enqueue(interval, ExistingPeriodicWorkPolicy.UPDATE)
    }

    private fun enqueue(interval: RefreshInterval, policy: ExistingPeriodicWorkPolicy) {
        // The period matches the user's interval; a 30-minute flex window lets the OS batch it.
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            interval.hours, TimeUnit.HOURS,
            30, TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WidgetUpdateWorker.UNIQUE_NAME, policy, request)
    }
}
