package com.sherepenko.android.archivarius.tasks

import android.content.Context
import com.sherepenko.android.archivarius.ArchivariusAnalytics
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import com.sherepenko.android.archivarius.utils.LogUtils
import java.io.File
import java.util.concurrent.TimeUnit

/** Removes older files if we exceed some max size.  */
class CleanupTask(
    context: Context,
    /** Directory.  */
    private val logDir: File,
    /** Current time in ms.  */
    private val now: Long,
    /** Allowed size.  */
    private val maxSize: Long
) : BaseTask(context) {

    override fun action() {
        if (!logDir.exists()) {
            LogUtils.warn("[CLEANUP] Cleanup called, but log directory does not exist: $logDir")
            return
        }

        if (!logDir.isDirectory) {
            // Something strange. In prod we choose to retry later.
            LogUtils.error("[CLEANUP] Not a directory $logDir")

            ArchivariusAnalytics.get()
                .reportToCrashlytics(
                    LogUtils.TAG,
                    IllegalStateException("Not a directory: $logDir")
                )
            return
        }

        val logFiles = ArchivariusUtils.listFiles(logDir, true)

        if (logFiles.isEmpty()) {
            LogUtils.warn("[CLEANUP] Cleanup called, but logs do not exist in $logDir; " +
                "Content: ${logDir.list()?.toList()}")
            return
        }

        val currentSize = ArchivariusUtils.getFilesSize(logFiles)
        val eldestLogBarrier = now - TimeUnit.HOURS.toMillis(24)

        if (currentSize > maxSize) {
            logFiles.forEach { logFile ->
                if (logFile.lastModified() < eldestLogBarrier) {
                    if (!logFile.delete()) {
                        LogUtils.error("[CLEANUP] Cannot remove file: $logFile")
                    }
                }
            }
        }
    }
}
