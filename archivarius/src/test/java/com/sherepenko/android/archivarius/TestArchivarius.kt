package com.sherepenko.android.archivarius

import android.content.Context
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.uploaders.LogUploadWorker
import com.sherepenko.android.archivarius.uploaders.LogUploader
import io.mockk.mockk
import java.io.File
import java.util.concurrent.Executor

open class TestArchivarius(context: Context) : Archivarius(
    context,
    LOG_DIR_MAX_SIZE,
    PARENT_LOG_DIR,
    DEFAULT_LOG_NAME,
    LOG_UPLOADER,
    LOG_UPLOAD_WORKER_CLASS,
    EXECUTOR
) {

    companion object {

        const val LOG_DIR_MAX_SIZE = 100L

        val PARENT_LOG_DIR = ArchivariusStrategy.get().parentLogDir
        val LOG_UPLOADER = mockk<LogUploader>()
        val LOG_UPLOAD_WORKER_CLASS = LogUploadWorker::class.java
        val EXECUTOR = Executor { it.run() }
    }

    override fun getLogDir(logType: LogType): File =
        File(baseLogDir, logType.name)
}
