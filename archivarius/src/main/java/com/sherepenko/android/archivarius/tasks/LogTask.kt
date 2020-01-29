package com.sherepenko.android.archivarius.tasks

import android.content.Context
import com.sherepenko.android.archivarius.ArchivariusAnalytics
import com.sherepenko.android.archivarius.ArchivariusStrategy
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.entries.LogEntry
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import com.sherepenko.android.archivarius.utils.LogUtils
import java.io.File
import java.io.IOException
import java.util.Calendar
import okio.appendingSink
import okio.buffer

/** Logging task.  */
class LogTask @JvmOverloads constructor(
    context: Context,
    logFile: File,
    /** What to log.  */
    private val logEntry: LogEntry,
    /** File name to write to.  */
    private val maxFileSize: Int = MAX_LOG_FILE_SIZE
) : WriteTask(context, logFile) {

    companion object {

        // Maximum log file size. If current log file exceeds it then file rotation is performed.
        private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5 MB

        private fun ensureMaxFileSizeRotation(logFile: File, maxFileSize: Long): File =
            if (logFile.length() > maxFileSize) {
                performFileRotation(logFile, logFile.lastModified())
            } else {
                logFile
            }

        private fun ensureDayRotation(logFile: File): File =
            if (logFile.isOldLogFile()) {
                performFileRotation(logFile,
                    Calendar.getInstance().apply {
                        timeInMillis = logFile.lastModified()
                    }
                        .midnight()
                        .timeInMillis
                )
            } else {
                logFile
            }

        private fun performFileRotation(logFile: File, lastModified: Long): File {
            val logFileName = logFile.name

            // We should distinguish uploaded files from primary and guest users.
            val newLogFileName = (logFileName +
                ArchivariusUtils.getLogFileSuffix(lastModified) +
                ArchivariusStrategy.get().rotateFilePostfix)

            val newLogFile = File(logFile.parentFile, newLogFileName)

            if (!logFile.renameTo(newLogFile)) {
                LogUtils.error("[LOG] Cannot rename file $logFile to $newLogFile")
            }

            return File(logFile.parentFile, logFileName)
        }
    }

    override fun action() {
        var curLogFile = logFile

        try {
            if (!curLogFile.exists()) {
                val logDir = curLogFile.parentFile!!

                if (!logDir.mkdirs() && !logDir.exists()) {
                    LogUtils.error("[LOG] Cannot create directory $logDir")
                    ArchivariusAnalytics.get()
                        .reportToCrashlytics(
                            LogUtils.TAG,
                            IllegalStateException(
                                "Cannot create directory $logDir. Entry: $logEntry"
                            )
                        )
                }
            } else if (logEntry.logType == LogType.JSON) {
                curLogFile = ensureDayRotation(curLogFile)
                curLogFile = ensureMaxFileSizeRotation(curLogFile, maxFileSize.toLong())
            }

            curLogFile.appendingSink().buffer().use { output ->
                logEntry.writeTo(context, output)
            }
        } catch (e: IOException) {
            ArchivariusAnalytics.get()
                .reportToCrashlytics(
                    LogUtils.TAG,
                    IOException("Cannot write to $curLogFile. Entry: $logEntry", e)
                )
        }
    }
}
