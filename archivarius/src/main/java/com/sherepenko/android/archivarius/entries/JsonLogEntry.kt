package com.sherepenko.android.archivarius.entries

import android.content.Context
import com.sherepenko.android.archivarius.data.LogType
import java.io.IOException
import okio.BufferedSink
import org.json.JSONObject

open class JsonLogEntry protected constructor(
    /** Message to write.  */
    private val message: String
) : LogEntry {

    override val logType: LogType = LogType.JSON

    constructor(message: Map<String, String>) : this(JSONObject(message).toString())

    @Throws(IOException::class)
    override fun writeTo(context: Context, output: BufferedSink) {
        output.writeUtf8("$message\n")
    }

    override fun toString(): String {
        return "JsonLogEntry"
    }
}
