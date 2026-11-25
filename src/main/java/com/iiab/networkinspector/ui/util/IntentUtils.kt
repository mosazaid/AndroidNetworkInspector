package com.iiab.networkinspector.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

fun openPdf(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF with"))
    } catch (e: Exception) {
        Log.e("NetworkInspector", "Failed to open PDF: $uri", e)
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun openDownloadsFolder(context: Context) {
    val downloadsDocUri =
        Uri.parse("content://com.android.externalstorage.documents/document/primary:Download")
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = downloadsDocUri
        type = "vnd.android.document/directory"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("NetworkInspector", "Failed to open Downloads folder", e)
        Toast.makeText(
            context,
            "Open the Downloads folder in your Files app to see the exported PDF.",
            Toast.LENGTH_LONG
        ).show()
    }
}
