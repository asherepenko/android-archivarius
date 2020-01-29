package com.sherepenko.android.archivarius.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.sherepenko.android.archivarius.ArchivariusStrategy
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ArchivariusUtils {

    const val LOG_FILE_NAME_PREFIX = "log-"

    const val LOG_FILE_NAME_SUFFIX = ".log"

    @VisibleForTesting
    const val LOG_FILE_NAME_DATE_TIME_FMT = "yyyy-MM-dd-HH-mm-ss-SSS"

    private const val POINT_SEPARATOR = "."

    @JvmStatic
    /** Accepts only 'archived' files.  */
    val OLD_LOG_FILES_FILTER = FilenameFilter { dir: File, fileName: String ->
        File(dir, fileName).isDirectory ||
            !(fileName.startsWith(LOG_FILE_NAME_PREFIX) && fileName.endsWith(LOG_FILE_NAME_SUFFIX))
    }

    @JvmStatic
    /** Accepts only 'active' files.  */
    val CURRENT_LOG_FILES_FILTER = FilenameFilter { dir: File, fileName: String ->
        File(dir, fileName).isDirectory ||
            (fileName.startsWith(LOG_FILE_NAME_PREFIX) && fileName.endsWith(LOG_FILE_NAME_SUFFIX))
    }

    /** Compares `lastModified` of files.  */
    @VisibleForTesting
    val LAST_MODIFIED_COMPARATOR = Comparator { lhs: File, rhs: File ->
        val diff = lhs.lastModified() - rhs.lastModified()
        when {
            diff > 0 -> 1
            diff < 0 -> -1
            else -> 0
        }
    }

    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun openFileOutput(context: Context, fileName: String): FileOutputStream =
        context.openFileOutput(LOG_FILE_NAME_PREFIX + fileName, Context.MODE_PRIVATE)

    @JvmStatic
    fun buildLogUri(fileName: String): Uri =
        Uri.parse("content://${ArchivariusStrategy.get().authority}/$LOG_FILE_NAME_PREFIX$fileName")

    @JvmStatic
    fun listFiles(dir: File, sorted: Boolean = true): List<File> =
        listFiles(dir, null, sorted)

    @JvmStatic
    @JvmOverloads
    fun listFiles(dir: File, filter: FilenameFilter?, sorted: Boolean = true): List<File> {
        val files = mutableListOf<File>()

        dir.listFiles(filter)?.let {
            it.forEach { file ->
                if (file.isFile) {
                    files.add(file)
                } else {
                    files.addAll(listFiles(file, filter, sorted))
                }
            }
        }

        if (sorted) {
            files.sortWith(LAST_MODIFIED_COMPARATOR)
        }

        return files
    }

    @JvmStatic
    @JvmOverloads
    fun getFilesSize(dir: File, filter: FilenameFilter? = null): Long =
        getFilesSize(listFiles(dir, filter, false))

    @JvmStatic
    fun getFilesSize(files: List<File>): Long {
        var size = 0L

        files.forEach {
            size += it.length()
        }

        return size
    }

    @JvmStatic
    fun buildLogFileName(date: Long): String =
        buildLogFileName(Date(date))

    @JvmStatic
    fun buildLogFileName(date: Date): String =
        SimpleDateFormat(LOG_FILE_NAME_DATE_TIME_FMT, Locale.US).format(date) + LOG_FILE_NAME_SUFFIX

    @JvmStatic
    fun buildLogFileName(logName: String): String =
        LOG_FILE_NAME_PREFIX + logName + LOG_FILE_NAME_SUFFIX

    @JvmStatic
    fun getLogFileSuffix(date: Long): String =
        getLogFileSuffix(Date(date))

    @JvmStatic
    fun getLogFileSuffix(date: Date): String =
        POINT_SEPARATOR + SimpleDateFormat(LOG_FILE_NAME_DATE_TIME_FMT, Locale.US).format(date)
}
