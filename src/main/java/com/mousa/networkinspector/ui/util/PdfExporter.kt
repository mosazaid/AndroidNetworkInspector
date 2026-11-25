package com.mousa.networkinspector.ui.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.mousa.networkinspector.domain.NetworkCallDetails
import com.mousa.networkinspector.ui.model.ExportResult
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun buildApiNameFromPath(path: String): String {
    val withoutQuery = path.substringBefore('?')
    val segments = withoutQuery.split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return "request"

    val last = segments.last()
    val secondLast = segments.getOrNull(segments.size - 2)
    val raw = if (secondLast != null) {
        "${secondLast}_${last}"
    } else {
        last
    }

    return raw
        .replace(Regex("[^A-Za-z0-9_]"), "_")
        .lowercase()
}

@SuppressLint("InlinedApi")
fun exportNetworkCallToPdf(
    context: Context,
    details: NetworkCallDetails
): ExportResult? {
    return try {
        val apiName = buildApiNameFromPath(details.path)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${apiName}_$timeStamp.pdf"

        val appName = context.applicationInfo
            .loadLabel(context.packageManager)
            .toString()
            .replace(Regex("[^A-Za-z0-9]"), "_")
            .lowercase()
        val relativeFolder = "Download/${appName}_logs"

        val root = JSONObject().apply {
            put("method", details.method)
            put("url", details.url)
            put("statusCode", details.statusCode)
            put("durationMillis", details.durationMillis)

            put(
                "request",
                JSONObject().apply {
                    put("headers", details.requestHeaders)
                    put("body", parseJsonOrString(details.requestBody))
                }
            )

            put(
                "response",
                JSONObject().apply {
                    put("headers", details.responseHeaders)
                    put("body", parseJsonOrString(details.responseBody))
                }
            )
        }

        val jsonText = root.toString(2)

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val paint = Paint().apply {
            textSize = 10f
            typeface = Typeface.MONOSPACE
        }
        val lineHeight = paint.textSize * 1.3f
        val maxTextWidth = pageWidth - margin * 2

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val x = margin
        var y = margin

        val lines = jsonText.split('\n')
        for (line in lines) {
            var start = 0
            val lineLength = line.length

            while (start < lineLength) {
                val count = paint.breakText(
                    line, start, lineLength, true, maxTextWidth, null
                )
                if (count <= 0) break

                if (y + lineHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo =
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin
                }

                val chunk = line.substring(start, start + count)
                canvas.drawText(chunk, x, y, paint)
                y += lineHeight
                start += count
            }
        }
        pdfDocument.finishPage(page)

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: run {
                Log.e("NetworkInspector", "Failed to insert PDF into MediaStore")
                pdfDocument.close()
                return null
            }

        resolver.openOutputStream(uri)?.use { out ->
            pdfDocument.writeTo(out)
        } ?: run {
            Log.e("NetworkInspector", "Failed to open output stream for $uri")
            pdfDocument.close()
            return null
        }

        pdfDocument.close()

        ExportResult(
            uri = uri,
            displayName = fileName,
            locationHint = "$relativeFolder/$fileName"
        )
    } catch (e: Exception) {
        Log.e("NetworkInspector", "Failed to export network call to PDF", e)
        null
    }
}
