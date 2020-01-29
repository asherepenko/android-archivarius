package com.sherepenko.android.archivarius

object ArchivariusAnalytics {

    private var implementation: ArchivariusAnalyticsImpl = NoOpArchivariusAnalytics()

    @JvmStatic
    fun init(archivariusAnalytics: ArchivariusAnalyticsImpl) {
        implementation = archivariusAnalytics
    }

    @JvmStatic
    fun get(): ArchivariusAnalyticsImpl = implementation

    interface ArchivariusAnalyticsImpl {

        fun reportToCrashlytics(tag: String, e: Throwable)
    }

    private class NoOpArchivariusAnalytics : ArchivariusAnalyticsImpl {

        override fun reportToCrashlytics(tag: String, e: Throwable) {
            throw UnsupportedOperationException("You must init ArchivariusAnalytics with " +
                "'ArchivariusAnalytics.init(...)' method before usage"
            )
        }
    }
}
