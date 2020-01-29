package com.sherepenko.android.archivarius.utils

import android.util.Log
import com.sherepenko.android.archivarius.ArchivariusStrategy

object LogUtils {

    const val TAG = "Archivarius"

    @JvmStatic
    fun debug(message: String) {
        if (ArchivariusStrategy.get().isInDebugMode && ArchivariusStrategy.get().isLogcatEnabled) {
            Log.d(TAG, message)
        }
    }

    @JvmStatic
    fun info(message: String) {
        if (ArchivariusStrategy.get().isLogcatEnabled) {
            Log.i(TAG, message)
        }
    }

    @JvmStatic
    fun warn(message: String) {
        if (ArchivariusStrategy.get().isLogcatEnabled) {
            Log.w(TAG, message)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun error(message: String, error: Throwable? = null) {
        if (ArchivariusStrategy.get().isLogcatEnabled) {
            if (error != null) {
                Log.e(TAG, message, error)
            } else {
                Log.e(TAG, message)
            }
        }
    }
}
