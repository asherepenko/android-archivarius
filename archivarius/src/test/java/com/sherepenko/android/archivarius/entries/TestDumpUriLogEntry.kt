package com.sherepenko.android.archivarius.entries

import android.content.Context
import android.net.Uri
import okio.BufferedSink

class TestDumpUriLogEntry(
    eventTime: Long,
    eventUpTime: Long,
    uri: Uri
) : DumpUriLogEntry(eventTime, eventUpTime, uri) {

    override fun dumpUri(context: Context, output: BufferedSink) {
        output.writeUtf8("No content found at URI: $uri")
    }
}
