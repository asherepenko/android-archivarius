package com.sherepenko.android.archivarius.entries

import android.content.Context
import android.net.Uri
import java.io.IOException
import okio.BufferedSink
import okio.source

/** Dump content from URI to log.  */
open class DumpUriLogEntry(
    eventTime: Long,
    eventUpTime: Long,
    /** Content URI.  */
    protected val uri: Uri
) : RawLogEntry(eventTime, eventUpTime) {

    @Throws(IOException::class)
    override fun writeTo(context: Context, output: BufferedSink) {
        super.writeTo(context, output)
        dumpUri(context, output)
    }

    protected open fun dumpUri(context: Context, output: BufferedSink) {
        context.contentResolver.openInputStream(uri)?.source().use { input ->
            if (input != null) {
                output.writeAll(input)
            } else {
                output.writeUtf8("No content found at URI: $uri")
            }
            return
        }
    }

    override fun toString(): String {
        return "DumpUriLogEntry"
    }
}
