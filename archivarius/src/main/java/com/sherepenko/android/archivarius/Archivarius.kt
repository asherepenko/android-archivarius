package com.sherepenko.android.archivarius

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.entries.LogEntry
import com.sherepenko.android.archivarius.tasks.CleanupTask
import com.sherepenko.android.archivarius.tasks.ExportTask
import com.sherepenko.android.archivarius.tasks.LogTask
import com.sherepenko.android.archivarius.tasks.PrepareUploadTask
import com.sherepenko.android.archivarius.tasks.UploadTask
import com.sherepenko.android.archivarius.tasks.WriteTask
import com.sherepenko.android.archivarius.uploaders.LogUploader
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.CURRENT_LOG_FILES_FILTER
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.buildLogFileName
import com.sherepenko.android.archivarius.utils.LogUtils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FilenameFilter
import java.util.EnumMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Writes crucial logs.  */
open class Archivarius @JvmOverloads protected constructor(
    /** Application context.  */
    protected val context: Context,
    /** Maximum size of logs directory.  */
    protected val maxSize: Long,
    parentLogDir: File,
    protected val logName: String,
    /** Log files uploader. */
    protected val logUploader: LogUploader,
    protected val logUploadWorkerClass: Class<out ListenableWorker>,
    /** Tasks executor.  */
    protected val executor: Executor = DEFAULT_EXECUTOR
) {

    companion object {

        /** Default log name: it will be wrapped with prefix and suffix.  */
        const val DEFAULT_LOG_NAME = "main"

        const val ONE_TIME_LOG_UPLOAD = "one-time-log-upload"

        const val PERIODIC_LOG_UPLOAD = "periodic-log-upload"

        protected const val LOG_UPLOAD_TAG = "log-upload-tag"

        /** Base path for logs.  */
        protected const val BASE_PATH = "logs"

        protected const val PERIODIC_LOG_UPLOAD_INTERVAL = 12L // hours

        /** Default max size for logs directory, 15MB.  */
        protected const val DEFAULT_MAX_SIZE = (15 * 1024 * 1024).toLong()

        /** Separate queue for logs uploading.  */
        protected val LOG_UPLOAD_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()

        /** Default executor for logging and upload preparation.  */
        protected val DEFAULT_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val logDirs: MutableMap<LogType, File> = EnumMap(LogType::class.java)

    /** Predefined network constraints.  */
    protected open val constraints: Constraints
        get() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    protected val baseLogDir: File

    init {
        this.baseLogDir = createBaseLogDir(parentLogDir)

        // Create separate directory per each log type
        for (logType in LogType.values()) {
            logDirs[logType] = File(this.baseLogDir, logType.name)
        }
    }

    fun log(logEntry: LogEntry?) {
        logEntry?.let {
            scheduleWrite(LogTask(context, getCurrentLogFile(it.logType), it))
            scheduleCleanup(
                CleanupTask(context, getLogDir(it.logType), System.currentTimeMillis(), maxSize)
            )
        }
    }

    fun uploadLogs(): Completable =
        Observable
            .fromIterable(getLogFiles(LogType.JSON, CURRENT_LOG_FILES_FILTER))
            .subscribeOn(Schedulers.from(executor))
            .flatMap { logFile ->
                Observable.fromCallable(PrepareUploadTask(context, logFile))
            }
            .ignoreElements()
            .andThen(Observable.fromIterable(LogType.values().toList())
                .subscribeOn(Schedulers.from(LOG_UPLOAD_EXECUTOR))
                .flatMap { logType ->
                    Observable.fromCallable(
                        UploadTask(context, getLogDir(logType), logUploader, logType)
                    )
                }
                .ignoreElements())

    fun exportLogs(): Observable<Uri> =
        Observable
            .fromCallable(ExportTask(context, baseLogDir))
            .subscribeOn(Schedulers.from(executor))

    fun scheduleLogsUpload(): Operation =
        WorkManager.getInstance(context)
            .beginUniqueWork(
                ONE_TIME_LOG_UPLOAD,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(logUploadWorkerClass)
                    .setConstraints(constraints)
                    .addTag(LOG_UPLOAD_TAG)
                    .build())
            .enqueue()

    @JvmOverloads
    fun schedulePeriodicLogsUpload(
        repeatInterval: Long = PERIODIC_LOG_UPLOAD_INTERVAL,
        repeatIntervalTimeUnit: TimeUnit = TimeUnit.HOURS
    ): Operation =
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_LOG_UPLOAD,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(
                    logUploadWorkerClass, repeatInterval, repeatIntervalTimeUnit
                )
                    .setConstraints(constraints)
                    .addTag(LOG_UPLOAD_TAG)
                    .build())

    fun cancelScheduledLogUploads(): Operation =
        WorkManager.getInstance(context).cancelAllWorkByTag(LOG_UPLOAD_TAG)

    /** Schedules write log task. Implementation might do write immediately  */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun scheduleWrite(task: WriteTask) {
        Completable.fromCallable(task)
            .subscribeOn(Schedulers.from(executor))
            .onErrorComplete { error ->
                ArchivariusAnalytics.get().reportToCrashlytics(LogUtils.TAG, error)
                true
            }
            .subscribe()
    }

    /** Schedule logs cleanup. Always must be done asynchronously.  */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun scheduleCleanup(task: CleanupTask) {
        Completable.fromCallable(task)
            .subscribeOn(Schedulers.from(executor))
            .onErrorComplete { error ->
                ArchivariusAnalytics.get().reportToCrashlytics(LogUtils.TAG, error)
                true
            }
            .subscribe()
    }

    protected open fun getLogDir(logType: LogType): File =
        logDirs.getValue(logType)

    protected open fun getCurrentLogFile(logType: LogType): File =
        File(getLogDir(logType), getLogFileName(logType))

    protected fun getLogFileName(logType: LogType): String =
        when (logType) {
            LogType.JSON -> logName
            LogType.RAW -> buildLogFileName(System.currentTimeMillis())
        }

    protected fun getLogFiles(logType: LogType, filter: FilenameFilter?): List<File> =
        when (logType) {
            LogType.JSON -> ArchivariusUtils.listFiles(getLogDir(logType), filter, false)
            LogType.RAW -> ArchivariusUtils.listFiles(getLogDir(logType), false)
        }

    private fun createBaseLogDir(parentLogDir: File): File =
        File(parentLogDir, BASE_PATH)

    /** Archivarius Builder instance.  */
    class Builder(context: Context) {

        private val context: Context = context.applicationContext

        private var maxSize: Long = DEFAULT_MAX_SIZE

        private var executor: ExecutorService = DEFAULT_EXECUTOR

        private var parentLogDir: File = ArchivariusStrategy.get().parentLogDir

        private var logName: String = buildLogFileName(ArchivariusStrategy.get().logName)

        private var logUploader: LogUploader = ArchivariusStrategy.get().logUploader

        private var logUploadWorkerClass: Class<out ListenableWorker> =
            ArchivariusStrategy.get().logUploadWorker

        private var immediate: Boolean = false

        fun withMaxSize(maxSize: Long): Builder {
            this.maxSize = maxSize
            return this
        }

        fun withParentLogDir(parentLogDir: File): Builder {
            this.parentLogDir = parentLogDir
            return this
        }

        fun withLogName(logName: String): Builder {
            this.logName = buildLogFileName(logName)
            return this
        }

        fun withLogUploader(logUploader: LogUploader): Builder {
            this.logUploader = logUploader
            return this
        }

        fun withLogUploadWorker(logUploadWorkerClass: Class<out ListenableWorker>): Builder {
            this.logUploadWorkerClass = logUploadWorkerClass
            return this
        }

        fun withExecutor(executor: Executor): Builder {
            this.executor = executor as ExecutorService
            return this
        }

        fun immediately(): Builder {
            this.immediate = true
            return this
        }

        fun build(): Archivarius {
            LogUtils.debug("Create new Archivarius instance with: \n" +
                "logDir = $parentLogDir;\n" +
                "logName = $logName;\n" +
                "maxSize = $maxSize bytes;\n" +
                "logUploader = ${logUploader.javaClass};\n" +
                "uploadWorker = $logUploadWorkerClass;\n" +
                "immediate = ${if (immediate) "true" else "false"}")

            return Archivarius(context, maxSize, parentLogDir, logName, logUploader,
                logUploadWorkerClass, if (immediate) Executor { it.run() } else executor
            )
        }
    }
}
