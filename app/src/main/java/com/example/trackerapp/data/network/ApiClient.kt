package com.example.trackerapp.data.network

import android.util.Log
import com.example.trackerapp.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

sealed interface BulkUploadResult {
    data object Success : BulkUploadResult
    data object Rejected : BulkUploadResult
    data object Failure : BulkUploadResult
}

sealed interface BulkDeleteResult {
    data object Success : BulkDeleteResult
    data object Failure : BulkDeleteResult
}

@Serializable
private data class BulkDeleteRequest(@SerialName("client_event_ids") val clientEventIds: List<String>)

class ApiClient {

    init {
        if (BuildConfig.HEALTH_API_TOKEN.isBlank()) {
            Log.w(
                TAG,
                "HEALTH_API_TOKEN is not set in local.properties — backend requests will be sent without an Authorization header and will get 401"
            )
        }
    }

    // Attached via defaultRequest so every current and future backend call carries the
    // token automatically, instead of needing to be added at each call site.
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            if (BuildConfig.HEALTH_API_TOKEN.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.HEALTH_API_TOKEN}")
            }
        }
    }

    suspend fun postEventsBulk(baseUrl: String, events: List<EventDto>): BulkUploadResult {
        return try {
            val response: HttpResponse = httpClient.post("${baseUrl.trimEnd('/')}/events/bulk") {
                contentType(ContentType.Application.Json)
                setBody(events)
            }
            when {
                response.status.isSuccess() -> BulkUploadResult.Success

                response.status.value == 401 -> {
                    Log.w(TAG, "Sync unauthorized (401) — check HEALTH_API_TOKEN in local.properties")
                    BulkUploadResult.Failure
                }

                // 422: backend rejected the batch outright (e.g. a malformed
                // timestamp) — a permanent problem, not a transient one.
                response.status.value == 422 -> BulkUploadResult.Rejected

                else -> BulkUploadResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BulkUploadResult.Failure
        }
    }

    suspend fun postEventsBulkDelete(baseUrl: String, clientEventIds: List<String>): BulkDeleteResult {
        return try {
            val response: HttpResponse = httpClient.post("${baseUrl.trimEnd('/')}/events/bulk-delete") {
                contentType(ContentType.Application.Json)
                setBody(BulkDeleteRequest(clientEventIds))
            }
            when {
                // Idempotent: per the backend contract, a 200 means every requested key
                // is now gone server-side, whether it just got deleted or was already
                // gone (never synced, or already deleted by a prior attempt).
                response.status.isSuccess() -> BulkDeleteResult.Success

                response.status.value == 401 -> {
                    Log.w(TAG, "Delete sync unauthorized (401) — check HEALTH_API_TOKEN in local.properties")
                    BulkDeleteResult.Failure
                }

                else -> BulkDeleteResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BulkDeleteResult.Failure
        }
    }

    companion object {
        private const val TAG = "ApiClient"
    }
}
