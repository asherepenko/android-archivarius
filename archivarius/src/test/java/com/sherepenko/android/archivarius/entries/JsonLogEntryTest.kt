package com.sherepenko.android.archivarius.entries

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils
import java.io.ByteArrayOutputStream
import java.util.LinkedHashMap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonLogEntryTest {

    private lateinit var outputStream: ByteArrayOutputStream

    @Before
    fun setUp() {
        outputStream = ByteArrayOutputStream()
    }

    @Test
    @Throws(Exception::class)
    fun testFormat() {
        val json = LinkedHashMap<String, String>().apply {
            this["message"] = "test message"
            this["timestamp"] = "2014-05-01T14:15:16.000+00:00"
        }

        val entry = JsonLogEntry(json)

        ArchivariusTestUtils.writeTo(outputStream, entry)

        assertThat(String(outputStream.toByteArray())).isEqualTo(
            "{\"message\":\"test message\",\"timestamp\":\"2014-05-01T14:15:16.000+00:00\"}\n"
        )
    }
}
