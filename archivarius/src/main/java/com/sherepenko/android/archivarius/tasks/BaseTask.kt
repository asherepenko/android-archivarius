package com.sherepenko.android.archivarius.tasks

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.io.File
import java.util.Calendar
import java.util.concurrent.Callable

/** Base class for archivarius tasks.  */
abstract class BaseTask protected constructor(context: Context) : Callable<Any> {

    companion object {

        @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
        @JvmStatic
        fun Calendar.midnight(): Calendar {
            this.set(Calendar.HOUR_OF_DAY, 0)
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.SECOND, 0)
            this.set(Calendar.MILLISECOND, 0)
            return this
        }

        @JvmStatic
        protected fun File.isOldLogFile(): Boolean {
            val calendar = Calendar.getInstance()

            val today = calendar.midnight().timeInMillis

            calendar.timeInMillis = this.lastModified()

            val last = calendar.midnight().timeInMillis

            return today != last
        }
    }

    protected val context: Context = context.applicationContext

    @Throws(Exception::class)
    override fun call() {
        action()
    }

    @Throws(Exception::class)
    protected abstract fun action()
}
