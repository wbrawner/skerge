package com.wbrawner.skerge

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HpScannerServiceTest {
    @Test
    fun requestScanTest() {
        val service = HpScannerService(HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/eSCL/ScanJobs" -> respond(
                            "", status = HttpStatusCode.Created, headers = headersOf(
                                "Server" to listOf("nginx"),
                                "Date" to listOf("Thu, 27 Feb 2014 20:09:56 GMT"),
                                "Content-Length" to listOf("0"),
                                "Connection" to listOf("keep-alive"),
                                "Location" to listOf("http://brawner.print/eSCL/ScanJobs/799f6753-3ebe-40dc-8ffa-1dc83cc7da1d"),
                                "Cache-Control" to listOf("must-revalidate", "max-age=0"),
                                "Pragma" to listOf("no-cache"),
                            )
                        )
                        else -> error("Unhandled request ${request.url}")
                    }
                }
            }
        })
        val scanId = runBlocking { service.requestScan() }
        assertEquals("799f6753-3ebe-40dc-8ffa-1dc83cc7da1d", scanId)
    }
}