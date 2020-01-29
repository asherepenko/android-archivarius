package com.sherepenko.android.archivarius.utils

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.sherepenko.android.archivarius.entries.LogEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.TimeZone
import okio.buffer
import okio.sink
import okio.source

object ArchivariusTestUtils {

    @JvmStatic
    fun setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newLogFile(logDir: File, fileName: String, content: String): File {
        val logFile = File(logDir, fileName)

        writeTo(logFile, content)

        return logFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newLogFile(logDir: File, fileName: String, content: String, lastModified: Long): File {
        val logFile = newLogFile(logDir, fileName, content)
        assertThat(logFile.setLastModified(lastModified)).isTrue()
        return logFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(logFile: File, content: String) {
        logFile.sink().buffer().use { output ->
            output.writeUtf8(content)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newLogFile(logDir: File, fileName: String, content: ByteArray): File {
        val logFile = File(logDir, fileName)

        writeTo(logFile, content)

        return logFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newLogFile(logDir: File, fileName: String, content: ByteArray, lastModified: Long): File {
        val logFile = newLogFile(logDir, fileName, content)
        assertThat(logFile.setLastModified(lastModified)).isTrue()
        return logFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(logFile: File, content: ByteArray) {
        logFile.sink().buffer().use { output ->
            output.write(content, 0, content.size)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readFrom(logFile: File): String =
        logFile.source().buffer().use { input ->
            input.readUtf8()
        }

    @JvmStatic
    @Throws(IOException::class)
    fun readFrom(uri: Uri): String {
        val fileName = uri.lastPathSegment

        if (fileName == null || !fileName.startsWith(ArchivariusUtils.LOG_FILE_NAME_PREFIX)) {
            throw FileNotFoundException("file " + fileName!!)
        }

        val logFile = getApplicationContext<Context>().getFileStreamPath(fileName)

        if (!logFile.exists()) {
            throw FileNotFoundException("file $fileName")
        }

        return readFrom(logFile)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(outputStream: ByteArrayOutputStream, logEntry: LogEntry) {
        outputStream.sink().buffer().use { output ->
            logEntry.writeTo(getApplicationContext<Context>(), output)
        }
    }
}
