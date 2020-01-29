package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.readFrom
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.writeTo
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrepareUploadTaskTest {

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
    fun shouldIgnoreEmptyFiles() {
        val logFile = newLogFile(logDir, ArchivariusUtils.buildLogFileName("test"), "")
        assertThat(logFile.exists()).isTrue()

        val task = PrepareUploadTask(getApplicationContext<Context>(), logFile)
        task.call()

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isEqualTo(0L)

        val newLogFile = File(
            logDir,
            logFile.name + ArchivariusUtils.getLogFileSuffix(logFile.lastModified()) + "_0"
        )
        assertThat(newLogFile.exists()).isFalse()
        assertThat(newLogFile.length()).isEqualTo(0L)
    }

    @Test
    @Throws(Exception::class)
    fun shouldIgnoreNonexistentFiles() {
        val logFile = File(logDir, ArchivariusUtils.buildLogFileName("test"))
        assertThat(logFile.exists()).isFalse()

        val task = PrepareUploadTask(getApplicationContext<Context>(), logFile)
        task.call()

        assertThat(logFile.exists()).isFalse()
        assertThat(logFile.length()).isEqualTo(0L)

        val newLogFile = File(
            logDir,
            logFile.name + ArchivariusUtils.getLogFileSuffix(logFile.lastModified()) + "_0"
        )
        assertThat(newLogFile.exists()).isFalse()
        assertThat(newLogFile.length()).isEqualTo(0L)
    }

    @Test
    @Throws(Exception::class)
    fun shouldPrepareLogsForUpload() {
        val logFile = newLogFile(
            logDir,
            ArchivariusUtils.buildLogFileName("test"),
            "Test log message #1\n"
        )
        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isGreaterThan(0L)
        assertThat(readFrom(logFile)).isEqualTo("Test log message #1\n")

        var lastModified = logFile.lastModified()

        val task = PrepareUploadTask(getApplicationContext<Context>(), logFile)
        task.call()

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isEqualTo(0L)

        var newLogFile = File(
            logDir,
            logFile.name + ArchivariusUtils.getLogFileSuffix(lastModified) + "_0"
        )
        assertThat(newLogFile.exists()).isTrue()
        assertThat(newLogFile.length()).isGreaterThan(0L)
        assertThat(readFrom(newLogFile)).isEqualTo("Test log message #1\n")

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isEqualTo(0L)

        writeTo(logFile, "Test log message #2\n")
        assertThat(logFile.length()).isGreaterThan(0L)
        assertThat(logFile.setLastModified(lastModified)).isTrue()
        assertThat(readFrom(logFile)).isEqualTo("Test log message #2\n")

        task.call()

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isEqualTo(0L)

        newLogFile = File(
            logDir,
            logFile.name + ArchivariusUtils.getLogFileSuffix(lastModified) + "_1"
        )
        assertThat(newLogFile.exists()).isTrue()
        assertThat(newLogFile.length()).isGreaterThan(0L)
        assertThat(readFrom(newLogFile)).isEqualTo("Test log message #2\n")

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isEqualTo(0L)
    }
}
