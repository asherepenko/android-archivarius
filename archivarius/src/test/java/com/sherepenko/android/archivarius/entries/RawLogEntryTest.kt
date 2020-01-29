package com.sherepenko.android.archivarius.entries

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils
import com.sherepenko.android.archivarius.utils.DateTimeUtils
import java.io.ByteArrayOutputStream
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RawLogEntryTest {

    private lateinit var outputStream: ByteArrayOutputStream

    @Before
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        ArchivariusTestUtils.setDefaultTimeZone()
    }

    @Test
    @Throws(Exception::class)
    fun testFormat() {
        val date = DateTimeUtils.parse("2014-05-01T14:15:16.000+00:00")!!
        val entry = TestRawLogEntry(date.time, 3)

        ArchivariusTestUtils.writeTo(outputStream, entry)

        assertThat(String(outputStream.toByteArray()))
            .isEqualTo("TestRawLogEntry\n2014-05-01T14:15:16.000+00:00 3\n")
    }

    private class TestRawLogEntry internal constructor(
        eventTime: Long,
        eventUpTime: Long
    ) : RawLogEntry(eventTime, eventUpTime)
}
