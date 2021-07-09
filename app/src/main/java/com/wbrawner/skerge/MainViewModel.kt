package com.wbrawner.skerge

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.IOException
import java.util.*

class MainViewModel(private val scannerService: ScannerService = HpScannerService()) : ViewModel() {
    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages = _pages.asSharedFlow()

    fun requestScan(fileDirectory: File) {
        val page = Page()
        _pages.value = _pages.value.toMutableList().apply {
            add(page)
        }
        viewModelScope.launch {
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs()
            }
            val pageId = try {
                scannerService.requestScan()
            } catch (e: Exception) {
                updatePage(null) {
                    it.copy(id = UUID.randomUUID().toString(), error = e)
                }
                return@launch
            }
            updatePage(null) {
                it.copy(id = pageId)
            }
            val downloadJob = async {
                // We have to open the connection to download the file before it's even ready,
                // because otherwise the scanner will cancel the scan job since it thinks you no
                // longer want the scan
                val pdf = File(fileDirectory, "${pageId}.pdf")
                Log.v("MainViewModel", "Downloading file for job $pageId")
                scannerService.downloadFile(pageId, pdf) { downloaded, size ->
                    Log.v("MainViewModel", "Progress for job ${pageId}, $downloaded of $size")
                    updatePage(pageId) {
                        it.copy(downloaded = downloaded, size = size)
                    }
                }
                Log.v("MainViewModel", "Job $pageId complete")
                updatePage(pageId) {
                    it.copy(file = pdf)
                }
            }
            var timeElapsed = 0
            while (coroutineContext.isActive) {
                delay(3000)
                timeElapsed += 3000
                Log.v("MainViewModel", "Checking scan status for job $pageId")
                val scanStatus = scannerService.getScanStatus(pageId).jobs
                    .firstOrNull { it.jobUuid == pageId }
                    ?: continue
                Log.v(
                    "MainViewModel",
                    "Job $pageId status: ${scanStatus.jobState} Reasons: ${
                        scanStatus.jobStateReasons.joinToString(", ")
                    }"
                )
                if (scanStatus.completed) break
                if (scanStatus.aborted) {
                    updatePage(pageId) {
                        it.copy(error = Exception("Scan job aborted: ${scanStatus.jobStateReasons.first()}"))
                    }
                    downloadJob.cancel()
                    return@launch
                }
                if (timeElapsed >= 60_000) {
                    // If it's been more than a minute then something is wrong, abort
                    updatePage(pageId) {
                        it.copy(error = IOException("Scan timeout"))
                    }
                    downloadJob.cancel()
                    return@launch
                }
            }
            downloadJob.await()
        }
    }

    fun removePage(page: Page) {
        updatePage(page.id) { null }
    }

    /**
     * Update the page with the given id using the given function and publish the results
     * @param id The id of the page to update
     * @param updates A function to call on the page to update it. Return null to remove the page
     * from the list
     */
    private fun updatePage(id: String?, updates: (Page) -> Page?) {
        val updatedPages = _pages.value.toMutableList()
        val pageIndex = updatedPages.indexOfFirst { it.id == id }
        updates(updatedPages.removeAt(pageIndex))?.let {
            updatedPages.add(pageIndex, it)
        }
        _pages.value = updatedPages
    }
}

data class Page(
    val id: String? = null,
    val downloaded: Long = 0,
    val size: Long = 0,
    val file: File? = null,
    val error: Exception? = null
)
