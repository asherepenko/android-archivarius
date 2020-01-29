package com.sherepenko.android.archivarius.entries

import android.content.Context
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.utils.DateTimeUtils
import java.io.IOException
import okio.BufferedSink

/** Something that can write itself to the log.  */
abstract class RawLogEntry protected constructor(
    /** Event time.  */
    private val eventTime: Long,
    /** Up time.  */
    private val eventUpTime: Long
) : LogEntry {

    override val logType: LogType = LogType.RAW

    @Throws(IOException::class)
    override fun writeTo(context: Context, output: BufferedSink) {
        output
            .writeUtf8("${javaClass.simpleName}\n")
            .writeUtf8(DateTimeUtils.format(eventTime))
            .writeUtf8(" ")
            .writeUtf8("$eventUpTime\n")
    }
}
