package com.sherepenko.android.archivarius.uploaders

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.uploaders.CloudLogUploader.Companion.MEDIA_TYPE_PLAIN_TEXT
import com.sherepenko.android.archivarius.uploaders.CloudLogUploader.LogUrlGeneratorApi
import com.sherepenko.android.archivarius.uploaders.CloudLogUploader.LogUrlGeneratorApi.LogUrl
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.GzipSource
import okio.Source
import okio.buffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudLogUploaderTest {

    companion object {

        private const val TEST_UPLOAD_PATH = "/upload"

        private const val TEST_DOWNLOAD_PATH = "/download"

        private const val TEST_LOG_FILE_NAME = "test.log"
    }

    @get:Rule val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )

    private lateinit var logDir: File

    private lateinit var server: MockWebServer

    private lateinit var logUploader: CloudLogUploader

    @Before
    @Throws(IOException::class)
    fun setUp() {
        logDir = getApplicationContext<Context>().filesDir

        server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            start()
        }

        val logUrlGeneratorApi = mock<LogUrlGeneratorApi>()
        whenever(logUrlGeneratorApi.generateLogUrl(any(), any()))
            .thenReturn(
                LogUrl(
                    server.url(TEST_UPLOAD_PATH).toString(),
                    server.url(TEST_DOWNLOAD_PATH).toString()
                )
            )

        logUploader = CloudLogUploader(logUrlGeneratorApi)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        logDir.listFiles()?.forEach {
            it.delete()
        }

        server.shutdown()
    }

    @Test
    fun testRawLogsUpload() {
        val content = "This is a test raw log message"
        val logFile = newLogFile(logDir, TEST_LOG_FILE_NAME, content)

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isGreaterThan(0L)

        logUploader.uploadLog(logFile, LogType.RAW)

        val request = server.takeRequest(1, TimeUnit.MINUTES)

        assertThat(request?.method).isEqualTo("PUT")
        assertThat(request?.path).isEqualTo(TEST_UPLOAD_PATH)
        assertThat(request?.getHeader("Content-Encoding")).isEqualTo("gzip")
        assertThat(request?.getHeader("Content-Type"))
            .isEqualTo(MEDIA_TYPE_PLAIN_TEXT.toString())
        assertThat(request?.bodySize).isGreaterThan(0L)

        GzipSource(request?.body as Source).buffer().use { input ->
            assertThat(input.readUtf8()).isEqualTo(content)
        }
    }

    @Test
    fun testJsonLogsUpload() {
        val content =
            "{\"message\":\"first message\",\"timestamp\":\"2014-05-01T14:15:16.000+00:00\"}\n" +
                "{\"message\":\"second message\",\"timestamp\":\"2014-05-01T15:16:17.000+00:00\"}\n"
        val logFile = newLogFile(logDir, TEST_LOG_FILE_NAME, content)

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isGreaterThan(0L)

        logUploader.uploadLog(logFile, LogType.JSON)

        val request = server.takeRequest(1, TimeUnit.MINUTES)

        assertThat(request?.method).isEqualTo("PUT")
        assertThat(request?.path).isEqualTo(TEST_UPLOAD_PATH)
        assertThat(request?.getHeader("Content-Encoding")).isEqualTo("gzip")
        assertThat(request?.getHeader("Content-Type"))
            .isEqualTo(MEDIA_TYPE_PLAIN_TEXT.toString())
        assertThat(request?.bodySize).isGreaterThan(0L)

        GzipSource(request?.body as Source).buffer().use { input ->
            assertThat(input.readUtf8()).isEqualTo(content)
        }
    }

    @Test
    fun shouldValidateJsonLogsBeforeUpload() {
        val validContent =
            "{\"message\":\"first message\",\"timestamp\":\"2014-05-01T14:15:16.000+00:00\"}\n" +
                "{\"message\":\"second message\",\"timestamp\":\"2014-05-01T15:16:17.000+00:00\"}\n"

        val content = validContent +
            "{\"message\":\"third message\",,\"timestamp\":\"2014-05-01T18:19:20.000+00:00\"}\n"

        val logFile = newLogFile(logDir, TEST_LOG_FILE_NAME, content)

        assertThat(logFile.exists()).isTrue()
        assertThat(logFile.length()).isGreaterThan(0L)

        logUploader.uploadLog(logFile, LogType.JSON)

        val request = server.takeRequest(1, TimeUnit.MINUTES)

        assertThat(request?.method).isEqualTo("PUT")
        assertThat(request?.path).isEqualTo(TEST_UPLOAD_PATH)
        assertThat(request?.getHeader("Content-Encoding")).isEqualTo("gzip")
        assertThat(request?.getHeader("Content-Type"))
            .isEqualTo(MEDIA_TYPE_PLAIN_TEXT.toString())
        assertThat(request?.bodySize).isGreaterThan(0L)

        GzipSource(request?.body as Source).buffer().use { input ->
            assertThat(input.readUtf8()).isEqualTo(validContent)
        }
    }
}
