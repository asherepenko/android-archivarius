package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.rules.ArchivariusTestRule
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.newLogFile
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils.readFrom
import java.io.File
import java.io.IOException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportTaskTest {

    @get:Rule val archivariusRule = ArchivariusTestRule(
        getApplicationContext(),
        ArchivariusTestRule.Mode.THROW
    )
    private lateinit var logDir: File

    @Before
    fun setUp() {
        logDir = File(getApplicationContext<Context>().filesDir, "logs")
        assertThat(logDir.mkdirs()).isTrue()
    }

    @Test
    @Throws(IOException::class)
    fun shouldConcatenateFiles() {
        assertThat(newLogFile(logDir, "1", "a", 20000).exists()).isTrue()
        assertThat(newLogFile(logDir, "2", "b", 30000).exists()).isTrue()
        assertThat(newLogFile(logDir, "3", "c", 10000).exists()).isTrue()

        val uri = ExportTask(getApplicationContext<Context>(), logDir, "result").call()

        assertThat(uri).isNotNull()
        assertThat(readFrom(uri)).contains("cab")
    }
}
