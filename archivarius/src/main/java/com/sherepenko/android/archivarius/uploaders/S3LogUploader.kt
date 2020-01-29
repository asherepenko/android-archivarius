package com.sherepenko.android.archivarius.uploaders

import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.sherepenko.android.archivarius.data.LogType
import com.sherepenko.android.archivarius.uploaders.CloudLogUploader.LogUrlGeneratorApi
import com.sherepenko.android.archivarius.uploaders.CloudLogUploader.LogUrlGeneratorApi.LogUrl
import com.sherepenko.android.archivarius.uploaders.S3LogUploader.LogBucketMeta
import java.util.Calendar
import java.util.Date
import okhttp3.OkHttpClient

class S3LogUploader(
    logBucketMeta: LogBucketMeta,
    api: LogUrlGeneratorApi = S3LogUrlGeneratorApi(logBucketMeta),
    client: OkHttpClient = OkHttpClient()
) : CloudLogUploader(api, client) {

    data class LogBucketMeta(
        val bucketName: String,
        val credentials: AWSCredentials,
        val region: Region
    )
}

open class S3LogUrlGeneratorApi(
    private val logBucketMeta: LogBucketMeta,
    private val s3Client: AmazonS3Client = createS3Client(logBucketMeta)
) : LogUrlGeneratorApi {

    companion object {

        fun createS3Client(logBucketMeta: LogBucketMeta): AmazonS3Client =
            AmazonS3Client(logBucketMeta.credentials, logBucketMeta.region)

        private fun getUploadUrlExpiration(currentTime: Date = Date()): Date =
            Calendar.getInstance().apply {
                time = currentTime
                add(Calendar.MINUTE, 15)
            }.time

        private fun getDownloadUrlExpiration(currentTime: Date = Date()): Date =
            Calendar.getInstance().apply {
                time = currentTime
                add(Calendar.HOUR, 12)
            }.time
    }

    override fun generateLogUrl(logName: String, logType: LogType): LogUrl {
        val logKey = generateLogKey(logName, logType)
        val currentTime = Date()

        return LogUrl(
            generateUploadUrl(logBucketMeta.bucketName, logKey, currentTime),
            generateDownloadUrl(logBucketMeta.bucketName, logKey, currentTime)
        )
    }

    open fun generateLogKey(logName: String, logType: LogType): String =
        "${logType.name}/$logName"

    private fun generateUploadUrl(bucketName: String, logKey: String, currentTime: Date): String =
        s3Client.generatePresignedUrl(
            bucketName,
            logKey,
            getUploadUrlExpiration(currentTime),
            HttpMethod.PUT
        ).toString()

    private fun generateDownloadUrl(bucketName: String, logKey: String, currentTime: Date): String =
        s3Client.generatePresignedUrl(
            bucketName,
            logKey,
            getDownloadUrlExpiration(currentTime),
            HttpMethod.GET
        ).toString()
}
