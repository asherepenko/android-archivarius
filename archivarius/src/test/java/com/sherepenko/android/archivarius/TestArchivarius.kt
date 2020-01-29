package com.sherepenko.android.archivarius

import android.content.Context
import androidx.work.ListenableWorker
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.uploaders.LogUploadWorker
import com.sherepenko.android.archivarius.uploaders.LogUploader
import java.io.File
import java.util.concurrent.Executor
import org.mockito.Mockito.mock

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

        val PARENT_LOG_DIR: File = ArchivariusStrategy.get().parentLogDir
        val LOG_UPLOADER: LogUploader = mock(LogUploader::class.java)
        val LOG_UPLOAD_WORKER_CLASS: Class<out ListenableWorker> = LogUploadWorker::class.java
        val EXECUTOR: Executor = Executor { it.run() }
    }

    override fun getLogDir(logType: LogType): File = File(baseLogDir, logType.name)
}
