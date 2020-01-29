package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.uploaders.LogUploader
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadTaskTest {

    @get:Rule val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )
    private lateinit var logDir: File

    private lateinit var logUploader: LogUploader

    @Before
    fun setUp() {
        logDir = getApplicationContext<Context>().filesDir
        logUploader = mock()
    }

    @After
    fun tearDown() {
        logDir.listFiles()?.forEach {
            it.delete()
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldSkipActiveJsonLogs() {
        val logFile = newLogFile(logDir, ArchivariusUtils.buildLogFileName("test"), "{}")
        assertThat(logFile.exists()).isTrue()

        UploadTask(getApplicationContext<Context>(), logDir, logUploader, LogType.JSON).call()

        verify(logUploader, never()).uploadLog(any(), any())
    }

    @Test
    @Throws(Exception::class)
    fun shouldUploadArchivedJsonLogs() {
        val logFile = newLogFile(
            logDir,
            ArchivariusUtils.buildLogFileName("test") +
                ArchivariusUtils.getLogFileSuffix(System.currentTimeMillis()),
            "{}"
        )
        assertThat(logFile.exists()).isTrue()

        UploadTask(getApplicationContext<Context>(), logDir, logUploader, LogType.JSON).call()

        verify(logUploader).uploadLog(eq(logFile), eq(LogType.JSON))
    }

    @Test
    @Throws(Exception::class)
    fun shouldUploadAllRawLogs() {
        val logFile = newLogFile(
            logDir,
            ArchivariusUtils.buildLogFileName(System.currentTimeMillis()),
            "Test log message content"
        )
        assertThat(logFile.exists()).isTrue()

        UploadTask(getApplicationContext<Context>(), logDir, logUploader, LogType.RAW).call()

        verify(logUploader).uploadLog(eq(logFile), eq(LogType.RAW))
    }

    @Test
    @Throws(Exception::class)
    fun shouldSkipEmptyFiles() {
        val logFile = newLogFile(
            logDir,
            ArchivariusUtils.buildLogFileName(System.currentTimeMillis()),
            ""
        )
        assertThat(logFile.exists()).isTrue()

        UploadTask(getApplicationContext<Context>(), logDir, logUploader, LogType.RAW).call()

        verify(logUploader, never()).uploadLog(any(), any())
    }
}
