package com.sherepenko.android.archivarius.tasks

import android.content.Context
import android.net.Uri
import com.sherepenko.android.archivarius.utils.ArchivariusUtils
import com.sherepenko.android.archivarius.utils.LogUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import okio.buffer
import okio.sink
import okio.source

class ExportTask @JvmOverloads constructor(
    context: Context,
    private val logDir: File,
    private val logFileName: String = DEFAULT_FILE_NAME
) : Callable<Uri> {

    companion object {

        private const val DEFAULT_FILE_NAME = "main"
    }

    private val context: Context = context.applicationContext

    @Throws(IOException::class)
    override fun call(): Uri {
        val logFiles = ArchivariusUtils.listFiles(logDir)

        LogUtils.info("-------------------------------------------------")

        if (logFiles.isNotEmpty()) {
            LogUtils.info("[EXPORT] Exporting logs from directory $logDir")

            ArchivariusUtils.openFileOutput(context, logFileName).sink().buffer().use { output ->
                logFiles.forEach {
                    it.source().buffer().use { input ->
                        output.writeAll(input)
                    }
                }
            }
        } else {
            LogUtils.warn("[EXPORT] Logs do not exist in $logDir")
        }

        val logUri = ArchivariusUtils.buildLogUri(logFileName)

        LogUtils.info("[EXPORT] Exported logs URI: $logUri")
        LogUtils.info("-------------------------------------------------")

        return logUri
    }
}
