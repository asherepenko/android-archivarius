package com.sherepenko.android.archivarius.uploaders

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.sherepenko.android.archivarius.Archivarius
import com.sherepenko.android.archivarius.ArchivariusAnalytics
import com.sherepenko.android.archivarius.utils.LogUtils
import io.reactivex.Single
import java.util.concurrent.ExecutionException

open class LogUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : RxWorker(context, workerParams) {

    companion object {
        private val LOCK = Any()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val archivarius: Archivarius by lazy { provideArchivarius() }

    override fun createWork(): Single<Result> {
        synchronized(LOCK) {
            LogUtils.info("=================================================")
            LogUtils.info("[WORKER] Log uploading started...")

            try {
                return archivarius.uploadLogs()
                    .doOnComplete {
                        // To have some statistics about successful upload.
                        LogUtils.info("-------------------------------------------------")
                        LogUtils.info("[WORKER] Log uploading completed")
                        LogUtils.info("=================================================")
                    }
                    .toSingleDefault(Result.success())
                    .onErrorResumeNext { error ->
                        LogUtils.info("-------------------------------------------------")
                        LogUtils.error(
                            "[WORKER] Unable to upload logs: ${error.cause!!.message}",
                            error
                        )
                        LogUtils.info("=================================================")
                        ArchivariusAnalytics.get().reportToCrashlytics(LogUtils.TAG, error)
                        Single.just(Result.failure())
                    }
            } catch (e: InterruptedException) {
                LogUtils.info("-------------------------------------------------")
                LogUtils.error("[WORKER] Log uploading interrupted", e)
                LogUtils.info("=================================================")
                ArchivariusAnalytics.get().reportToCrashlytics(LogUtils.TAG, e)
            } catch (e: ExecutionException) {
                // This must have been handled!
                throw AssertionError(e)
            }
        }

        return Single.just(Result.failure())
    }

    protected open fun provideArchivarius(): Archivarius =
        Archivarius.Builder(applicationContext).build()
}
