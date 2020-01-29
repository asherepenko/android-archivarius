package com.sherepenko.android.archivarius.entries

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.utils.ArchivariusTestUtils
import com.sherepenko.android.archivarius.utils.DateTimeUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class DumpUriLogEntryTest {

    private lateinit var outputStream: ByteArrayOutputStream

    @Before
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        ArchivariusTestUtils.setDefaultTimeZone()
    }

    @Test
    @Throws(Exception::class)
    fun testFormat() {
        val message = "Test long\nmessage"
        val uri = Uri.parse("content:///test.log'")
        Shadows.shadowOf(getApplicationContext<Context>().contentResolver)
            .registerInputStream(uri, ByteArrayInputStream(message.toByteArray()))

        val date = DateTimeUtils.parse("2014-05-01T14:15:16.000+00:00")!!
        val entry = DumpUriLogEntry(date.time, 3, uri)

        ArchivariusTestUtils.writeTo(outputStream, entry)

        assertThat(String(outputStream.toByteArray()))
            .isEqualTo("DumpUriLogEntry\n2014-05-01T14:15:16.000+00:00 3\n$message")
    }
}
