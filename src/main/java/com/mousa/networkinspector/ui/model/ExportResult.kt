package com.mousa.networkinspector.ui.model

import android.net.Uri

data class ExportResult(
    val uri: Uri,
    val displayName: String,
    val locationHint: String
)
