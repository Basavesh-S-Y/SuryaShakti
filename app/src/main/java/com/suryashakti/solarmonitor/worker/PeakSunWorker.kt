package com.suryashakti.solarmonitor.worker

import android.content.Context
import androidx.work.*
import com.suryashakti.solarmonitor.repository.ForecastRepository
import com.suryashakti.solarmonitor.util.showPeakSunNotification
import java.util.concurrent.TimeUnit

/**
 * Background worker that fires the "Peak Sun" notification ONLY when the
 * live Open-Meteo direct_radiation value for the user's location exceeds
 * 500 W/m² — genuine solar irradiance, NOT a simple clock assumption.
 *
 * Runs every 30 minutes via WorkManager PeriodicWorkRequest.
 * Uses last-saved GPS coordinates (written by the UI) so no foreground
 * location permission is needed here.
 */
class PeakSunWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = ForecastRepository(context)

            // ── Real irradiance check via Open-Meteo ──────────────────────
            // Returns true only when W/m² > 500 at the current hour
            val isPeak = repository.isPeakSunRightNow()

            if (isPeak) {
                context.showPeakSunNotification()
            }

            Result.success()
        } catch (e: Exception) {
            // Retry once on transient network failure; then succeed silently
            if (runAttemptCount < 1) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "peak_sun_irradiance_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // needs internet for API
                .setRequiresBatteryNotLow(false)
                .build()

            val request = PeriodicWorkRequestBuilder<PeakSunWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
