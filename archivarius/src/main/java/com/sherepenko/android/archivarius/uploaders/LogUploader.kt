package com.sherepenko.android.archivarius.uploaders

import com.sherepenko.android.archivarius.data.LogType
import java.io.File

interface LogUploader {

    fun uploadLog(logFile: File, logType: LogType)
}
