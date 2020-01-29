package com.sherepenko.android.archivarius.interceptors

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer

class GzipRequestInterceptor : Interceptor {

    companion object {

        private const val HEADER_CONTENT_ENCODING = "Content-Encoding"

        private const val GZIP_ENCODING = "gzip"
    }

    override fun intercept(chain: Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.body == null ||
            originalRequest.header(HEADER_CONTENT_ENCODING) != null
        ) {
            return chain.proceed(originalRequest)
        }

        val compressedRequest = originalRequest.newBuilder()
            .header(HEADER_CONTENT_ENCODING, GZIP_ENCODING)
            .method(originalRequest.method, originalRequest.body.gzip())
            .build()

        return chain.proceed(compressedRequest)
    }

    private fun RequestBody?.gzip(): RequestBody =
        object : RequestBody() {

            private val buffer = Buffer()

            init {
                GzipSink(buffer).buffer().use { input ->
                    this@gzip?.writeTo(input)
                }
            }

            override fun contentType(): MediaType? =
                this@gzip?.contentType()

            override fun contentLength(): Long =
                buffer.size

            override fun writeTo(sink: BufferedSink) {
                sink.writeAll(buffer.clone())
            }
        }
}
