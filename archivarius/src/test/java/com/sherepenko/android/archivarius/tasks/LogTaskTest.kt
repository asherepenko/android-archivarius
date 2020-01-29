package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.entries.TestLogEntry
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.tasks.BaseTask.Companion.midnight
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.readFrom
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import java.io.File
import java.io.RandomAccessFile
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogTaskTest {

    @get:Rule val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )
    private lateinit var logDir: File

    @Before
    fun setUp() {
        logDir = getApplicationContext<Context>().filesDir
    }

    @After
    fun tearDown() {
        logDir.listFiles()?.forEach {
            it.delete()
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldWriteLogsToFile() {
        val logFile = File(logDir, ArchivariusUtils.buildLogFileName("test"))
        assertThat(logFile.exists()).isFalse()

        val content =
            "{\"message\":\"test message\",\"timestamp\":\"2014-05-01T14:15:16.000+00:00\"}"

        LogTask(getApplicationContext<Context>(), logFile, TestLogEntry(content)).call()

        assertThat(logFile.exists()).isTrue()
        assertThat(readFrom(logFile)).isEqualTo("$content\n")
    }

    @Test
    @Throws(Exception::class)
    fun shouldRotateLogFilesBasedOnObsolescence() {
        val lastModified = Calendar.getInstance().apply {
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis - TimeUnit.HOURS.toMillis(25)

        val logFile = newLogFile(
            logDir,
            ArchivariusUtils.buildLogFileName("test"),
            "{\"message\":\"test message #1\"}\n",
            lastModified
        )
        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.lastModified()).isEqualTo(lastModified)

        LogTask(
            getApplicationContext<Context>(),
            logFile,
            TestLogEntry("{\"message\":\"test message #2\"}")
        ).call()

        val rotatedLastModified = Calendar.getInstance().apply {
            timeInMillis = lastModified
        }.midnight().timeInMillis

        val rotatedLogFile = File(
            logDir,
            logFile.name + ArchivariusUtils.getLogFileSuffix(rotatedLastModified)
        )

        assertThat(rotatedLogFile.exists()).isTrue()
        assertThat(rotatedLogFile.length()).isGreaterThan(0L)
        assertThat(readFrom(rotatedLogFile)).isEqualTo("{\"message\":\"test message #1\"}\n")

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isGreaterThan(0L)
        assertThat(readFrom(logFile)).isEqualTo("{\"message\":\"test message #2\"}\n")
    }

    @Test
    @Throws(Exception::class)
    fun shouldRotateLogFilesBasedOnFileSize() {
        val content = "{\"message\":\"test message\"}"

        val logFile = File(logDir, ArchivariusUtils.buildLogFileName("test"))
        assertThat(logFile.exists()).isFalse()

        RandomAccessFile(logFile, "rw").setLength(1025)

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isEqualTo(1025)

        val lastModified = logFile.lastModified()

        LogTask(getApplicationContext<Context>(), logFile, TestLogEntry(content), 1024).call()

        val rotatedLogFile = File(
            logDir,
            logFile.name + ArchivariusUtils.getLogFileSuffix(lastModified)
        )

        assertThat(rotatedLogFile.exists()).isTrue()
        assertThat(rotatedLogFile.length()).isEqualTo(1025)

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isGreaterThan(0L)
        assertThat(readFrom(logFile)).isEqualTo("$content\n")
    }
}
