package com.example.pifan

import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun dateToEpoch(dateStr: String): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        date.toEpochSecond(ZoneOffset.UTC)
    } else {
        Long.MAX_VALUE
    }
}