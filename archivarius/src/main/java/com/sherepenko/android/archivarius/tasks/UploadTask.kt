package com.sherepenko.android.archivarius.tasks

import android.content.Context
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.uploaders.LogUploader
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.OLD_LOG_FILES_FILTER
import com.sherepenko.android.archivarius.utils.LogUtils
import java.io.File

/** Upload logs to cloud. */
class UploadTask(
    context: Context,
    private val logDir: File,
    private val logUploader: LogUploader,
    private val logType: LogType
) : BaseTask(context) {

    @Throws(Exception::class)
    override fun action() {
        val logFiles = ArchivariusUtils.listFiles(logDir, OLD_LOG_FILES_FILTER, false)

        if (logFiles.isNotEmpty()) {
            LogUtils.info("-------------------------------------------------")
            LogUtils.info("[UPLOAD] Start log uploading from: $logDir")
            LogUtils.info("[UPLOAD] ${logDir.name} logs waiting for upload: ${logFiles.size}")

            var uploadedLogs = 0

            logFiles.forEach { logFile ->
                // Skip empty files
                if (logFile.length() > 0L) {
                    LogUtils.debug(
                        "[UPLOAD] File ${logFile.name} (${logFile.length()} bytes) is uploading..."
                    )

                    try {
                        logUploader.uploadLog(logFile, logType)

                        uploadedLogs++

                        LogUtils.debug("[UPLOAD] File ${logFile.name} " +
                            "(${logFile.length()} bytes) successfully uploaded"
                        )

                        if (!logFile.delete()) {
                            LogUtils.debug("[UPLOAD] Cannot remove file: $logFile")
                        }
                    } catch (e: Throwable) {
                        LogUtils.error("[UPLOAD] Error during file upload", e)
                        throw e
                    }
                } else {
                    LogUtils.debug("[UPLOAD] File ${logFile.name} is empty. Skipping...")
                }
            }

            LogUtils.info("[UPLOAD] $uploadedLogs ${logType.name} log(s) uploaded")
        }
    }
}
