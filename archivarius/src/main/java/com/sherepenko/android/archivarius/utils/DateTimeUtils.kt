package com.sherepenko.android.archivarius.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private const val TIMEZONE_SEMICOLON_INDEX = 26

    private const val ISO_8601_DATE_TIME_FMT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    @JvmStatic
    fun format(date: Long): String {
        return format(Date(date))
    }

    @JvmStatic
    fun format(date: Date): String {
        val formatted = SimpleDateFormat(ISO_8601_DATE_TIME_FMT, Locale.US).format(date)
        return formatted.substring(0, TIMEZONE_SEMICOLON_INDEX) + ":" +
            formatted.substring(TIMEZONE_SEMICOLON_INDEX)
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun parse(dateTime: String): Date? {
        var formatted = dateTime

        if (formatted[TIMEZONE_SEMICOLON_INDEX] == ':') {
            formatted = formatted.substring(0, TIMEZONE_SEMICOLON_INDEX) +
                formatted.substring(TIMEZONE_SEMICOLON_INDEX + 1)
        }

        return SimpleDateFormat(ISO_8601_DATE_TIME_FMT, Locale.US).parse(formatted)
    }
}
