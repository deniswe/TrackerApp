package com.example.trackerapp.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trackerapp.TrackerApplication
import com.example.trackerapp.data.network.BulkUploadResult
import com.example.trackerapp.data.network.EventDto
import kotlinx.coroutines.flow.first
import java.time.Instant

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TrackerApplication
        val entryDao = app.database.eventEntryDao()

        val unsynced = entryDao.getUnsynced()
        if (unsynced.isEmpty()) {
            return Result.success()
        }

        val baseUrl = app.settingsRepository.baseUrl.first()
        if (baseUrl.isBlank()) {
            Log.i(TAG, "No backend URL configured in Settings — skipping sync")
            return Result.success()
        }

        val dtos = unsynced.map { entry ->
            EventDto(
                clientEventId = entry.clientEventId,
                type = entry.typeKey,
                timestamp = Instant.ofEpochMilli(entry.epochMillis).toString(),
                source = entry.source,
                value = entry.value,
                note = entry.note
            )
        }

        return when (app.apiClient.postEventsBulk(baseUrl, dtos)) {
            BulkUploadResult.Success -> {
                entryDao.markSynced(unsynced.map { it.clientEventId })
                app.settingsRepository.recordSyncSuccess(System.currentTimeMillis())
                Log.i(TAG, "Synced ${unsynced.size} event(s) to $baseUrl")
                Result.success()
            }

            BulkUploadResult.Rejected -> {
                // 422: backend rejected the batch outright (e.g. a malformed
                // timestamp) — a permanent problem, not a transient one, so retrying
                // with backoff would just loop forever without ever succeeding.
                Log.e(TAG, "Backend rejected the batch (422) — not retrying")
                Result.failure()
            }

            BulkUploadResult.Failure -> {
                Log.w(TAG, "Sync failed (backend unreachable or errored) — will retry")
                Result.retry()
            }
        }
    }

    companion object {
        const val TAG = "SyncWorker"
    }
}
