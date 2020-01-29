package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.Random
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CleanupTaskTest {

    companion object {

        private const val TEST_LOG_SIZE = 1000

        private const val TEST_LOGS_COUNT = 3

        private val RANDOM = Random()

        @Throws(IOException::class)
        private fun createLogFiles(logDir: File, currentTime: Long): List<File> {
            val logFiles = mutableListOf<File>()

            val delay = TimeUnit.DAYS.toMillis(1)
            val content = ByteArray(TEST_LOG_SIZE)

            RANDOM.nextBytes(content)

            for (i in 0 until TEST_LOGS_COUNT) {
                val lastModified = currentTime - i * delay
                val logFileName = ArchivariusUtils.buildLogFileName("test-$i")
                val logFile = newLogFile(logDir, logFileName, content, lastModified)

                assertThat(logFile.exists()).isTrue()
                assertThat(logFile.lastModified()).isEqualTo(lastModified)
                assertThat(logFile.length()).isEqualTo(TEST_LOG_SIZE)

                logFiles.add(logFile)
            }

            return logFiles
        }
    }

    @get:Rule val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )

    private lateinit var logDir: File

    private var currentTime: Long = 0L

    @Before
    @Throws(IOException::class)
    fun setUp() {
        ArchivariusTestUtils.setDefaultTimeZone()

        logDir = getApplicationContext<Context>().filesDir

        currentTime = Calendar.getInstance().apply {
            this.set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        createLogFiles(logDir, currentTime)

        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT)
    }

    @After
    fun tearDown() {
        logDir.listFiles()?.forEach {
            it.delete()
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldCleanupOnlyWhenMaxSizeExceeded() {
        val maxAllowedSize = (TEST_LOG_SIZE * (TEST_LOGS_COUNT - 1)).toLong()
        val logFile = File(
            logDir,
            ArchivariusUtils.buildLogFileName("test-${(TEST_LOGS_COUNT - 1)}")
        )

        assertThat(logFile.exists()).isTrue()
        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT)

        val task = CleanupTask(
            getApplicationContext<Context>(),
            logDir,
            currentTime,
            maxAllowedSize
        )

        task.call()

        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT - 1)
        assertThat(logFile.exists()).isFalse()

        task.call()
        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT - 1)
    }

    @Test
    @Throws(Exception::class)
    fun shouldNotDeleteCurrentLog() {
        val logFile = File(logDir, ArchivariusUtils.buildLogFileName("test-0"))

        assertThat(logFile.exists()).isTrue()
        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT)

        CleanupTask(getApplicationContext<Context>(), logDir, System.currentTimeMillis(), 0L).call()

        assertThat(logDir.listFiles())
            .asList()
            .hasSize(1)
        assertThat(logFile.exists()).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun shouldCleanupAllOutdatedFiles() {
        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT)

        // Make all logs old enough for cleanup
        logDir.listFiles()!!.forEach {
            assertThat(it.setLastModified(0)).isTrue()
        }

        CleanupTask(getApplicationContext<Context>(), logDir, currentTime, 0L).call()

        assertThat(logDir.listFiles()).isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun shouldIgnoreFileAsParameter() {
        assertThat(logDir.listFiles())
            .asList()
            .hasSize(TEST_LOGS_COUNT)

        logDir.listFiles()!!.forEach {
            try {
                CleanupTask(getApplicationContext<Context>(), it, currentTime, 0L).call()
                fail("not reported")
            } catch (e: AssertionError) {
                assertThat(e.cause).isInstanceOf(IllegalStateException::class.java)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldIgnoreNonexistentDirs() {
        val dir = File(logDir, "device_logs")
        assertThat(dir.exists()).isFalse()

        CleanupTask(getApplicationContext<Context>(), dir, currentTime, 1).call()
        assertThat(dir.exists()).isFalse()
    }
}
