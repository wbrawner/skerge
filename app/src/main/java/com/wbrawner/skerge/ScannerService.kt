package com.wbrawner.skerge

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import java.io.File
import java.io.IOException

interface ScannerService {
    suspend fun requestScan(scanSettings: ScanSettings = ScanSettings()): String
    suspend fun getScanStatus(scanId: String): ScannerStatus
    suspend fun downloadFile(
        uuid: String,
        destination: File,
        onProgress: (downloaded: Long, size: Long) -> Unit
    )
}

// TODO: It would be cool to be able to autodiscover the printer(s) or manually enter the details
const val SCANNER_URL = "http://brawner.print"

class HpScannerService(
    private val client: HttpClient = HttpClient(CIO) {
        buildHeaders {
            append("Content-Type", "text/xml")
        }
    }
) : ScannerService {
    override suspend fun requestScan(scanSettings: ScanSettings): String {
        val url = URLBuilder(SCANNER_URL).path("eSCL", "ScanJobs").build()
        val response: HttpResponse = client.post(url) {
            body = scanSettings.toXml()
        }
        val location = response.headers["Location"]
            ?: throw IOException("Scanner didn't return location")
        return location.replace("${url}/", "")
    }

    override suspend fun getScanStatus(scanId: String): ScannerStatus = ScannerStatus.fromXml(
        client.get(URLBuilder(SCANNER_URL).path("eSCL", "ScannerStatus").build())
    )

    override suspend fun downloadFile(
        uuid: String,
        destination: File,
        onProgress: (downloaded: Long, size: Long) -> Unit
    ) {
        val httpResponse: HttpResponse = client.get(
            URLBuilder(SCANNER_URL).path("eSCL", "ScanJobs", uuid, "NextDocument").build()
        ) {
            onDownload { bytesSentTotal, contentLength ->
                onProgress(bytesSentTotal, contentLength)
            }
        }
        val responseBody: ByteArray = httpResponse.receive()
        destination.writeBytes(responseBody)
    }
}