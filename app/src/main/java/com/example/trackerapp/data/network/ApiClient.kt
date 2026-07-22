package com.example.trackerapp.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

sealed interface BulkUploadResult {
    data object Success : BulkUploadResult
    data object Rejected : BulkUploadResult
    data object Failure : BulkUploadResult
}

class ApiClient {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
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
                response.status.value == 422 -> BulkUploadResult.Rejected
                else -> BulkUploadResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BulkUploadResult.Failure
        }
    }
}
