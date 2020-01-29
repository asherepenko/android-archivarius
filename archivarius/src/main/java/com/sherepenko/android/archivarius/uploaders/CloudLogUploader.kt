package com.sherepenko.android.archivarius.uploaders

import androidx.annotation.VisibleForTesting
import com.sherepenko.android.archivarius.ArchivariusStrategy
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.data.LogType.JSON
import com.sherepenko.android.archivarius.data.LogType.RAW
import com.sherepenko.android.archivarius.interceptors.GzipRequestInterceptor
import com.sherepenko.android.archivarius.utils.LogUtils
import java.io.File
import java.io.IOException
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import okhttp3.logging.HttpLoggingInterceptor.Logger
import okio.BufferedSink
import okio.buffer
import okio.source
import org.json.JSONException
import org.json.JSONObject

open class CloudLogUploader @JvmOverloads constructor(
    private val api: LogUrlGeneratorApi,
    client: OkHttpClient = OkHttpClient()
) : LogUploader {

    companion object {
        @VisibleForTesting
        val MEDIA_TYPE_PLAIN_TEXT = "text/plain; charset=utf-8".toMediaType()

        @VisibleForTesting
        const val TGZ_FILE_EXT = ".txt.gz"
    }

    interface LogUrlGeneratorApi {
        fun generateLogUrl(logName: String, logType: LogType): LogUrl

        data class LogUrl(
            /** URL that will be used for uploading */
            val uploadUrl: String,
            /** URL for log file downloading */
            val downloadUrl: String
        )
    }

    private val httpClient = client.newBuilder()
        .addInterceptor(GzipRequestInterceptor())
        .addInterceptor(HttpLoggingInterceptor(object : Logger {

            override fun log(message: String) =
                LogUtils.info(message)
        }).apply {
            level = if (ArchivariusStrategy.get().isInDebugMode) {
                Level.BODY
            } else {
                Level.BASIC
            }
        })
        .build()

    override fun uploadLog(logFile: File, logType: LogType) {
        val endpoint = api.generateLogUrl(logFile.name + TGZ_FILE_EXT, logType)

        LogUtils.info("-------------------------------------------------")
        LogUtils.info("[UPLOADER] Upload URL: ${endpoint.uploadUrl}. " +
            "Download URL: ${endpoint.downloadUrl}"
        )

        val requestBody = object : RequestBody() {

            override fun contentType(): MediaType =
                MEDIA_TYPE_PLAIN_TEXT

            override fun writeTo(sink: BufferedSink) {
                when (logType) {
                    JSON -> sink.writeJsonLogs(logFile)
                    RAW -> sink.writeRawLogs(logFile)
                }
            }
        }

        val request = Request.Builder()
            .url(endpoint.uploadUrl)
            .put(requestBody)
            .build()

        LogUtils.info("[UPLOADER] File ${logFile.name} " +
            "(${logFile.length()} bytes) is uploading to ${endpoint.uploadUrl}"
        )

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                LogUtils.info(
                    "[UPLOADER] File ${logFile.name} successfully uploaded to ${endpoint.uploadUrl}"
                )
            } else {
                val error = RuntimeException("HTTP ${response.code}: ${response.message}")
                LogUtils.error(
                    "[UPLOADER] Cannot upload file ${logFile.name} to ${endpoint.uploadUrl}",
                    error
                )

                throw error
            }
        }

        LogUtils.info("-------------------------------------------------")
    }

    private fun BufferedSink.writeJsonLogs(logFile: File) {
        logFile.source().buffer().use { input ->
            try {
                while (!input.exhausted()) {
                    val line = input.readUtf8LineStrict()

                    if (line.isValidJson()) {
                        this.writeUtf8("$line\n")
                    }
                }
            } catch (e: IOException) {
                LogUtils.error("[UPLOADER] JSON logs are incomplete. Skipping them...", e)
            }
        }
    }

    private fun BufferedSink.writeRawLogs(logFile: File) {
        logFile.source().buffer().use { input ->
            this.writeAll(input)
        }
    }

    private fun String.isValidJson(): Boolean {
        try {
            JSONObject(this)
        } catch (e: JSONException) {
            return false
        }

        return true
    }
}
