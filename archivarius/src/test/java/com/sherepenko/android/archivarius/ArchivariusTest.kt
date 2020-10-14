package com.sherepenko.android.archivarius

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.entries.TestLogEntry
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.uploaders.LogUploadWorker
import com.sherepenko.android.archivarius.uploaders.LogUploader
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.readFrom
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.lang.Boolean.TRUE
import java.util.IdentityHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okio.buffer
import okio.sink
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class ArchivariusTest {

    companion object {

        private fun cleanup(dir: File): Boolean {
            dir.listFiles()?.forEach { file ->
                cleanup(file)
            }

            return dir.delete()
        }
    }

    @JvmField
    @Rule
    val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )

    @Before
    fun setUp() {
        WorkManagerImpl.setDelegate(mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        cleanup(ArchivariusStrategy.get().parentLogDir)
    }

    @Test
    fun shouldLogAndCleanup() {
        val archivarius = spyk(Archivarius.Builder(getApplicationContext()).build())

        archivarius.log(TestLogEntry("message"))

        // one for writing a log
        verify { archivarius.scheduleWrite(any()) }
        // another for cleanup
        verify { archivarius.scheduleCleanup(any()) }
    }

    @Test
    fun shouldScheduleLogsUpload() {
        val archivarius = Archivarius.Builder(getApplicationContext()).build()

        archivarius.scheduleLogsUpload()

        val workRequestSlot = slot<OneTimeWorkRequest>()

        verify {
            WorkManager.getInstance(getApplicationContext())
                .beginUniqueWork(
                    eq(Archivarius.ONE_TIME_LOG_UPLOAD),
                    eq(ExistingWorkPolicy.REPLACE),
                    capture(workRequestSlot)
                )
        }

        workRequestSlot.captured.apply {
            assertThat(workSpec.workerClassName).isEqualTo(LogUploadWorker::class.java.name)
        }
    }

    @Test
    fun shouldSchedulePeriodicLogsUpload() {
        val archivarius = Archivarius.Builder(getApplicationContext()).build()

        archivarius.schedulePeriodicLogsUpload()

        val workRequestSlot = slot<PeriodicWorkRequest>()

        verify {
            WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                    eq(Archivarius.PERIODIC_LOG_UPLOAD),
                    eq(ExistingPeriodicWorkPolicy.KEEP),
                    capture(workRequestSlot)
                )
        }

        workRequestSlot.captured.apply {
            assertThat(workSpec.workerClassName).isEqualTo(LogUploadWorker::class.java.name)
        }
    }

    @Test
    @Throws(IOException::class)
    fun shouldUploadLogs() {
        val logUploader = mockk<LogUploader>(relaxed = true)

        val archivarius = Archivarius.Builder(getApplicationContext())
            .withLogUploader(logUploader)
            .build()

        // For preparation task we need the log to exist.
        archivarius.logDirs.values.forEach { logDir ->
            logDir.mkdirs()
        }

        val jsonLogFile = File(
            archivarius.logDirs[LogType.JSON],
            ArchivariusUtils.buildLogFileName("test")
        )
        assertThat(jsonLogFile.createNewFile()).isTrue()

        jsonLogFile.sink().buffer().use { output ->
            output.writeUtf8(
                "{\"message\":\"test message\",\"timestamp\":\"2014-05-01T14:15:16.000+00:00\"}\n"
            )
        }

        val rawLogFile = File(
            archivarius.logDirs[LogType.RAW],
            ArchivariusUtils.buildLogFileName(System.currentTimeMillis())
        )
        assertThat(rawLogFile.createNewFile()).isTrue()

        rawLogFile.sink().buffer().use { output ->
            output.writeUtf8("[TAG] Raw log message.")
        }

        val observer = TestObserver.create<Any>()

        archivarius.uploadLogs().subscribe(observer)

        observer.await(1, TimeUnit.MINUTES)
        observer.assertComplete()

        val fileSlot = mutableListOf<File>()

        verify(exactly = 2) { logUploader.uploadLog(capture(fileSlot), any()) }

        fileSlot.apply {
            assertThat(this[0].absolutePath).startsWith(jsonLogFile.absolutePath)
            assertThat(this[1].absolutePath).startsWith(rawLogFile.absolutePath)
        }
    }

    @Test
    @Throws(IOException::class)
    fun shouldWriteToFileIfImmediate() {
        val archivarius = Archivarius.Builder(getApplicationContext())
            .immediately()
            .build()

        archivarius.log(TestLogEntry("reboot"))

        val logFile = File(
            archivarius.logDirs[LogType.JSON],
            ArchivariusUtils.buildLogFileName("main")
        )

        assertThat(logFile.exists()).isTrue()

        logFile.source().buffer().use { input ->
            assertThat(input.readUtf8()).contains("reboot")
        }
    }

    @Test
    fun shouldExportLogs() {
        val archivarius = Archivarius.Builder(getApplicationContext())
            .build()

        archivarius.log(TestLogEntry("test message"))

        val observer = TestObserver.create(
            object : Observer<Uri> {
                override fun onNext(uri: Uri) {
                    assertThat(uri).isNotNull()

                    try {
                        assertThat(readFrom(uri)).contains("test message")
                    } catch (e: IOException) {
                        throw AssertionError(e)
                    }
                }

                override fun onError(error: Throwable) {
                    throw AssertionError(error)
                }

                override fun onComplete() = Unit

                override fun onSubscribe(disposable: Disposable) = Unit
            }
        )

        archivarius.exportLogs().subscribe(observer)

        observer.await(1, TimeUnit.MINUTES)
        observer.assertNoErrors()
        observer.assertComplete()
    }

    @Test
    fun immediateArchivariusDoesCleanupAsynchronously() {
        val application = getApplicationContext<Application>()
        val archivarius = spyk(
            Archivarius.Builder(application)
                .immediately()
                .build()
        )

        archivarius.log(TestLogEntry("message"))
        // Cleanup task scheduled.
        verify { archivarius.scheduleCleanup(any()) }
        // Only.
        assertThat(Shadows.shadowOf(application).nextStartedService).isNull()
    }

    @Test
    fun checkUploadLogsThreads() {
        val threads = IdentityHashMap<Thread, Boolean>()
        val prepareLogs = Executors.newSingleThreadExecutor()
        val uploadLogs = Executors.newSingleThreadExecutor()

        val observer = TestObserver.create<Any>()

        Completable
            .fromCallable {
                assertThat(threads.put(Thread.currentThread(), TRUE)).isNull()
                null
            }
            .subscribeOn(Schedulers.from(prepareLogs))
            .andThen(
                Completable
                    .fromCallable {
                        assertThat(threads.put(Thread.currentThread(), TRUE)).isNull()
                        null
                    }
                    .subscribeOn(Schedulers.from(uploadLogs))
            )
            .subscribe(observer)

        observer.await(1, TimeUnit.MINUTES)
        observer.assertNoErrors()
        observer.assertComplete()
    }
}
