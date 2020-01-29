package com.sherepenko.android.archivarius.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.CURRENT_LOG_FILES_FILTER
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.LAST_MODIFIED_COMPARATOR
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.LOG_FILE_NAME_DATE_TIME_FMT
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.LOG_FILE_NAME_PREFIX
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.LOG_FILE_NAME_SUFFIX
import com.sherepenko.android.archivarius.utils.ArchivariusUtils.OLD_LOG_FILES_FILTER
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchivariusUtilsTest {

    companion object {

        private const val TEST_LOG_SIZE = 1000

        private const val TEST_LOGS_COUNT = 10

        private val RANDOM = Random()

        @Throws(IOException::class)
        private fun createLogFiles(logDir: File, currentTime: Long): List<File> {
            val logFiles = mutableListOf<File>()

            val delay = TimeUnit.DAYS.toMillis(1)
            val content = ByteArray(TEST_LOG_SIZE)

            RANDOM.nextBytes(content)

            for (i in 0 until TEST_LOGS_COUNT) {
                val lastModified = currentTime + i * delay
                val logFileName = buildLogFileName(i)
                val logFile = newLogFile(logDir, logFileName, content, lastModified)

                assertThat(logFile.exists()).isTrue()
                assertThat(logFile.lastModified()).isEqualTo(lastModified)
                assertThat(logFile.length()).isEqualTo(TEST_LOG_SIZE)

                logFiles.add(logFile)
            }

            return logFiles
        }

        private fun buildLogFileName(index: Int): String =
            if (index % 2 == 0) {
                LOG_FILE_NAME_PREFIX + "test-$index" + LOG_FILE_NAME_SUFFIX
            } else {
                LOG_FILE_NAME_PREFIX + "test-$index" + LOG_FILE_NAME_SUFFIX +
                    ArchivariusUtils.getLogFileSuffix(System.currentTimeMillis())
            }
    }

    @get:Rule val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )

    private lateinit var logDir: File

    private lateinit var logFiles: List<File>

    @Before
    @Throws(IOException::class)
    fun setUp() {
        ArchivariusTestUtils.setDefaultTimeZone()

        logDir = getApplicationContext<Context>().filesDir

        logFiles = createLogFiles(logDir, 0L)
    }

    @After
    fun tearDown() {
        logDir.listFiles()?.forEach {
            it.delete()
        }
    }

    @Test
    fun testBuildLogUri() {
        val logName = "test"
        val logUri = ArchivariusUtils.buildLogUri(logName)
        assertThat(logUri.toString()).contains(LOG_FILE_NAME_PREFIX + logName)
    }

    @Test
    fun testBuildLogFileName() {
        val logName = "test"
        var logFileName = ArchivariusUtils.buildLogFileName("test")
        assertThat(logFileName)
            .isEqualTo(LOG_FILE_NAME_PREFIX + logName + LOG_FILE_NAME_SUFFIX)

        val date = Date()
        logFileName = ArchivariusUtils.buildLogFileName(date)
        assertThat(logFileName)
            .isEqualTo(SimpleDateFormat(LOG_FILE_NAME_DATE_TIME_FMT, Locale.US).format(date) +
                LOG_FILE_NAME_SUFFIX
            )
    }

    @Test
    fun testListFiles() {
        val files = ArchivariusUtils.listFiles(logDir, false)
        assertThat(files).hasSize(TEST_LOGS_COUNT)
        assertThat(files).containsAtLeastElementsIn(logFiles)
    }

    @Test
    fun testSortedListFiles() {
        Collections.sort(logFiles, LAST_MODIFIED_COMPARATOR)
        val files = ArchivariusUtils.listFiles(logDir, true)
        assertThat(files).hasSize(TEST_LOGS_COUNT)
        assertThat(files).containsExactlyElementsIn(logFiles)
    }

    @Test
    fun testFilteredListFiles() {
        val currentFiles =
            ArchivariusUtils.listFiles(logDir, CURRENT_LOG_FILES_FILTER)
        assertThat(currentFiles).hasSize(TEST_LOGS_COUNT / 2)

        val oldFiles = ArchivariusUtils.listFiles(logDir, OLD_LOG_FILES_FILTER)
        assertThat(oldFiles).hasSize(TEST_LOGS_COUNT / 2)

        assertThat(currentFiles).containsNoneIn(oldFiles)

        for (i in 0 until TEST_LOGS_COUNT) {
            if (i % 2 == 0) {
                assertThat(currentFiles).contains(logFiles[i])
                assertThat(oldFiles).doesNotContain(logFiles[i])
            } else {
                assertThat(currentFiles).doesNotContain(logFiles[i])
                assertThat(oldFiles).contains(logFiles[i])
            }
        }
    }

    @Test
    fun testFilesSize() {
        var filesSize = ArchivariusUtils.getFilesSize(logFiles)
        assertThat(filesSize).isEqualTo(TEST_LOG_SIZE * TEST_LOGS_COUNT)

        filesSize = ArchivariusUtils.getFilesSize(logDir)
        assertThat(filesSize).isEqualTo(TEST_LOG_SIZE * TEST_LOGS_COUNT)
    }
}
