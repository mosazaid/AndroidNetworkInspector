package com.mousa.networkinspector.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val requestTimestampFormat =
    SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SSS", Locale.getDefault())

fun formatRequestTimestamp(millis: Long): String =
    requestTimestampFormat.format(Date(millis))
