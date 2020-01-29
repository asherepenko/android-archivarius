package com.sherepenko.android.archivarius.rules

import android.content.Context
import androidx.work.ListenableWorker
import com.sherepenko.android.archivarius.Archivarius
import com.sherepenko.android.archivarius.ArchivariusAnalytics
import com.sherepenko.android.archivarius.ArchivariusStrategy
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.uploaders.LogUploadWorker
import com.sherepenko.android.archivarius.uploaders.LogUploader
import java.io.File
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ArchivariusTestRule(private val context: Context, private val mode: Mode) : TestRule {

    companion object {

        const val AUTHORITY = "com.sherepenko.android.archivarius"
    }

    enum class Mode {
        THROW,
        RECORD
    }

    private val errors: MutableList<Throwable> = mutableListOf()

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {

            @Throws(Throwable::class)
            override fun evaluate() {
                ArchivariusAnalytics.init(TestArchivariusAnalytics())
                ArchivariusStrategy.init(TestArchivariusStrategy())
                base.evaluate()
            }
        }

    fun getErrors(): List<Throwable> =
        errors

    private inner class TestArchivariusAnalytics : ArchivariusAnalytics.ArchivariusAnalyticsImpl {

        override fun reportToCrashlytics(tag: String, e: Throwable) {
            when (mode) {
                Mode.THROW -> throw AssertionError(e)
                Mode.RECORD -> errors.add(e)
            }
        }
    }

    private inner class TestArchivariusStrategy : ArchivariusStrategy.ArchivariusStrategyImpl {

        override val isInDebugMode: Boolean = true

        override val isLogcatEnabled: Boolean = true

        override val authority: String = AUTHORITY

        override val rotateFilePostfix: String = ""

        override val logName: String = Archivarius.DEFAULT_LOG_NAME

        override val parentLogDir: File = context.filesDir

        override val logUploader: LogUploader =
            object : LogUploader {
                override fun uploadLog(logFile: File, logType: LogType) = Unit
            }

        override val logUploadWorker: Class<out ListenableWorker> = LogUploadWorker::class.java
    }
}
