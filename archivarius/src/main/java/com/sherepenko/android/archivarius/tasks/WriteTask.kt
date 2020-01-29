package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.io.File
import java.util.LinkedHashMap

/** Base task for write operations.  */
abstract class WriteTask(
    context: Context,
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) val logFile: File
) : BaseTask(context) {

    companion object {

        const val MAX_ENTRIES = 1000

        /**
         * When Archivarius is created with `immediately` option, we get a possibility
         * of 2 concurrent modifications of files.
         * These locks guard our files.
         */
        private val LOCKS = object : LinkedHashMap<String, Any>() {
            override fun removeEldestEntry(eldest: Map.Entry<String, Any>): Boolean {
                return size > MAX_ENTRIES
            }
        }

        @Synchronized
        private fun getLock(filePath: String): Any {
            if (!LOCKS.contains(filePath)) {
                LOCKS[filePath] = Any()
            }

            return LOCKS.getValue(filePath)
        }
    }

    /** File lock.  */
    private val lock = getLock(this.logFile.absolutePath)

    @Throws(Exception::class)
    override fun call() {
        synchronized(lock) {
            super.call()
        }
    }
}
