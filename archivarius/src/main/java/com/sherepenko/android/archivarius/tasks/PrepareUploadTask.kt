package com.sherepenko.android.archivarius.tasks

import android.content.Context
import com.sherepenko.android.archivarius.ArchivariusAnalytics
import com.sherepenko.android.archivarius.ArchivariusStrategy
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import com.sherepenko.android.archivarius.utils.LogUtils
import java.io.File
import java.io.IOException

/** Task that ensures that current logs are ready for upload.  */
class PrepareUploadTask(context: Context, logFile: File) : WriteTask(context, logFile) {

    override fun action() {
        if (!logFile.exists()) {
            LogUtils.error("[PREPARE] Cannot upload file $logFile because it does not exist")
            return
        }

        if (logFile.length() == 0L) {
            LogUtils.error("[PREPARE] Cannot upload file $logFile because it is empty")
            return
        }

        LogUtils.info("-------------------------------------------------")
        LogUtils.info("[PREPARE] Prepare file $logFile (${logFile.length()} bytes) for upload")

        val logDir = logFile.parentFile!!

        if (!logDir.isDirectory) {
            // Something strange. In prod we choose to retry later.
            LogUtils.error("[PREPARE] Not a directory $logDir")

            ArchivariusAnalytics.get()
                .reportToCrashlytics(
                    LogUtils.TAG,
                    IllegalStateException("Not a directory: $logDir")
                )
            return
        }

        // Choose a new file name and move data.
        val newLogFile = chooseNewLogFile(logFile)

        LogUtils.debug("[PREPARE] Moving current logs (${logFile.length()} bytes) " +
            "from ${logFile.name} to $newLogFile"
        )

        val renamed = logFile.renameTo(newLogFile)

        check(renamed) {
            "Cannot rename file $logFile to $newLogFile"
        }

        LogUtils.info(
            "[PREPARE] File ${newLogFile.name} (${newLogFile.length()} bytes) is ready for upload"
        )

        createLogFileIfNotExists(logFile)
    }

    private fun chooseNewLogFile(logFile: File): File {
        val logDir = logFile.parentFile!!

        // We access date format sequentially with LogTask, so it's safe.
        val logFileName =
            logFile.name + ArchivariusUtils.getLogFileSuffix(logFile.lastModified()) + "_"

        var index = 0
        var newLogFile: File

        do {
            check(index <= MAX_ENTRIES) {
                "Cannot get a new file for logs after $MAX_ENTRIES attempts. " +
                    "Log directory: ${listOf(logDir.list()!!)}"
            }

            // We should distinguish uploaded files from primary and guest users.
            val newLogFileName = logFileName + index + ArchivariusStrategy.get().rotateFilePostfix

            newLogFile = File(logDir, newLogFileName)
            index++
        } while (newLogFile.exists())

        return newLogFile
    }

    private fun createLogFileIfNotExists(logFile: File) {
        if (logFile.exists()) {
            return
        }

        try {
            if (!logFile.createNewFile()) {
                LogUtils.error("[PREPARE] Cannot create new file $logFile")
            }
        } catch (e: IOException) {
            LogUtils.error("[PREPARE] Cannot create new file $logFile", e)
            ArchivariusAnalytics.get()
                .reportToCrashlytics(
                    LogUtils.TAG,
                    IOException("Cannot create new file $logFile", e)
                )
        }
    }
}
