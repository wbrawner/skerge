package com.wbrawner.skerge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import kotlin.math.round

suspend fun Page.loadBitmap(renderMode: Int = PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY): Triple<Bitmap?, Int, Int> =
    withContext(Dispatchers.IO) {
        if (file == null) return@withContext Triple(null, 0, 0)
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
            .openPage(0)
            .use { page ->
                Triple(
                    Bitmap.createBitmap(
                        page.width.toPx(),
                        page.height.toPx(),
                        Bitmap.Config.ARGB_8888
                    ).apply {
                        page.render(this, null, null, renderMode)
                    },
                    page.width,
                    page.height
                )
            }
    }

fun File.buildShareIntent(context: Context): Intent =
    Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        val uri = FileProvider.getUriForFile(
            context,
            "com.wbrawner.skerge.pdfprovider",
            this@buildShareIntent
        )
        data = uri
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, name)

private const val SCALE_FACTOR = 0.25f

suspend fun List<Page>.merge(): File {
    val pdf = PdfDocument()
    var pageCount = 0
    var scansDirectory: File? = null
    for (page in this) {
        scansDirectory = page.file?.parentFile
        val (bitmap, width, height) = page.loadBitmap(PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        if (bitmap == null) continue
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageCount++).create()
        pdf.startPage(pageInfo).apply {
            canvas.drawBitmap(bitmap, Matrix().apply { setScale(SCALE_FACTOR, SCALE_FACTOR) }, null)
            bitmap.recycle()
            pdf.finishPage(this)
        }
    }
    val file = File(scansDirectory, "Scan ${Instant.now().toEpochMilli()}.pdf")
    file.outputStream().use {
        pdf.writeTo(it)
    }
    pdf.close()
    return file
}

/**
 * Convert an Int from PostScript Points to pixels
 */
fun Int.toPx() = round(this / SCALE_FACTOR).toInt()
