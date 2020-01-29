package com.sherepenko.android.archivarius.entries

import android.content.Context
import com.sherepenko.android.archivarius.data.LogType
import java.io.IOException
import okio.BufferedSink

class TestLogEntry(private val message: String) : LogEntry {
    override val logType: LogType
        get() = LogType.JSON

    @Throws(IOException::class)
    override fun writeTo(context: Context, output: BufferedSink) {
        output.writeUtf8("$message\n")
    }
}
