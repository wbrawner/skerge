package com.wbrawner.skerge

import android.content.SharedPreferences
import androidx.core.content.edit
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

interface ScannerService {
    var url: String
    suspend fun requestScan(scanSettings: ScanSettings = ScanSettings()): String
    suspend fun getScanStatus(scanId: String): ScannerStatus
    suspend fun downloadFile(
        uuid: String,
        destination: File,
        onProgress: (downloaded: Long, size: Long) -> Unit
    )
}

private const val KEY_SCANNER_URL = "scannerUrl"

class HpScannerService(
    private val client: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = TimeUnit.MINUTES.toMillis(2)
        }
        buildHeaders {
            append("Content-Type", "text/xml")
        }
    },
    private val sharedPreferences: SharedPreferences
) : ScannerService {
    override var url: String
        get() = sharedPreferences.getString(KEY_SCANNER_URL, null).orEmpty()
        set(value) = sharedPreferences.edit { putString(KEY_SCANNER_URL, value) }

    override suspend fun requestScan(scanSettings: ScanSettings): String {
        val url = URLBuilder(url).run {
            path("eSCL", "ScanJobs")
            build()
        }
        val response: HttpResponse = client.post(url) {
            setBody(scanSettings.toXml())
        }
        val location = response.headers["Location"]
            ?: throw IOException("Scanner didn't return location")
        return location.replace("${url}/", "")
    }

    override suspend fun getScanStatus(scanId: String): ScannerStatus = ScannerStatus.fromXml(
        client.get(
            URLBuilder(url)
                .run {
                    path("eSCL", "ScannerStatus")
                    build()
                }
        ).body()
    )

    override suspend fun downloadFile(
        uuid: String,
        destination: File,
        onProgress: (downloaded: Long, size: Long) -> Unit
    ) {
        val httpResponse: HttpResponse = client.get(
            URLBuilder(url).run {
                path("eSCL", "ScanJobs", uuid, "NextDocument")
                build()
            }
        ) {
            onDownload { bytesSentTotal, contentLength ->
                onProgress(bytesSentTotal, contentLength)
            }
        }
        val responseBody: ByteArray = httpResponse.body()
        destination.writeBytes(responseBody)
    }
}