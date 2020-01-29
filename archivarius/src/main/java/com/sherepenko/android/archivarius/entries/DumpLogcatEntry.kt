package com.sherepenko.android.archivarius.entries

import android.content.Context
import java.io.IOException
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import okio.BufferedSink
import okio.source

/** Dump logcat.  */
open class DumpLogcatEntry(
    eventTime: Long,
    eventUpTime: Long
) : RawLogEntry(eventTime, eventUpTime) {

    companion object {

        private const val LOGCAT_COMMAND = "logcat -d 2>&1"

        private const val FINISH_PROCESS_TIMEOUT_MS = 500L

        private val DUMP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20)
    }

    @Throws(IOException::class)
    override fun writeTo(context: Context, output: BufferedSink) {
        super.writeTo(context, output)

        output.writeUtf8("Logcat - start\n")

        var exitValue: Int? = null
        val process = Runtime.getRuntime().exec(LOGCAT_COMMAND)

        try {
            process!!.inputStream.source().use { input ->
                val startTime = System.currentTimeMillis()

                while (
                    exitValue == null &&
                    System.currentTimeMillis() - startTime <= DUMP_TIMEOUT_MS
                ) {
                    output.writeAll(input)

                    try {
                        exitValue = process.exitValue()
                    } catch (e: IllegalThreadStateException) {
                        // Wait for process to finish.
                        Thread.sleep(FINISH_PROCESS_TIMEOUT_MS)
                    }
                }

                output.writeAll(input)
                output.writeUtf8("Logcat - end. Exit value = ${exitValue!!}\n")
            }
        } catch (e: Exception) {
            output.writeUtf8("Logcat - failed\n")
            val printStream = PrintStream(output.outputStream())
            e.printStackTrace(printStream)
            printStream.flush()
        } finally {
            process?.destroy()
        }
    }

    override fun toString(): String {
        return "DumpLogcatEntry"
    }
}
