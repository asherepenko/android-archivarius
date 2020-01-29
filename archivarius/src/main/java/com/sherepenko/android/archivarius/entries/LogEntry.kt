package com.sherepenko.android.archivarius.entries

import android.content.Context
import com.sherepenko.android.archivarius.data.LogType
import java.io.IOException
import okio.BufferedSink

/** Something that can write itself to the log.  */
interface LogEntry {

    val logType: LogType

    @Throws(IOException::class)
    fun writeTo(context: Context, output: BufferedSink)
}
