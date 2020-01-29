package com.sherepenko.android.archivarius

import androidx.work.ListenableWorker
import com.sherepenko.android.archivarius.uploaders.LogUploader
import java.io.File

object ArchivariusStrategy {

    private var implementation: ArchivariusStrategyImpl = NoOpArchivariusStrategy()

    @JvmStatic
    fun init(archivariusStrategy: ArchivariusStrategyImpl) {
        implementation = archivariusStrategy
    }

    @JvmStatic
    fun get(): ArchivariusStrategyImpl = implementation

    interface ArchivariusStrategyImpl {
        val isInDebugMode: Boolean

        val isLogcatEnabled: Boolean

        val authority: String

        val logName: String

        val rotateFilePostfix: String

        val parentLogDir: File

        val logUploader: LogUploader

        val logUploadWorker: Class<out ListenableWorker>
    }

    private class NoOpArchivariusStrategy : ArchivariusStrategyImpl {

        override val isInDebugMode: Boolean
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val isLogcatEnabled: Boolean
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val authority: String
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                    "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val logName: String
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                    "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val rotateFilePostfix: String
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val parentLogDir: File
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val logUploader: LogUploader
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                "'ArchivariusStrategy.init(...)' method before usage"
            )

        override val logUploadWorker: Class<out ListenableWorker>
            get() = throw UnsupportedOperationException("You must init ArchivariusStrategy with " +
                "'ArchivariusStrategy.init(...)' method before usage"
            )
    }
}
