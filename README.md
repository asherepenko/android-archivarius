# Archivarius

[![](https://jitci.com/gh/asherepenko/android-archivarius/svg)](https://jitci.com/gh/asherepenko/android-archivarius)
[![](https://jitpack.io/v/asherepenko/android-archivarius.svg)](https://jitpack.io/#asherepenko/android-archivarius) 

Archivarius is responsible for storing, rotating, and uploading logs from an Android device to S3.

## Installation

```gradle
repositories { 
     maven(url = "https://jitpack.io")
}
dependencies {
    implementation("com.github.asherepenko:android-archivarius:x.y.z")
}
```

## Initial setup
Archivarius has to be initialized with proper strategy

```kotlin
ArchivariusStrategy.init(object : ArchivariusStrategyImpl {
    override val isInDebugMode: Boolean = true

    override val isLogcatEnabled: Boolean = true

    override val authority: String = ""

    override val rotateFilePostfix: String = ""

    override val logName: String = Archivarius.DEFAULT_LOG_NAME

    override val parentLogDir: File = context.filesDir

    override val logUploader: LogUploader =
        object : LogUploader {
            override fun uploadLog(logFile: File, logType: LogType) = Unit
        }

    override val logUploadWorker: Class<out ListenableWorker> = LogUploadWorker::class.java
})
```

## Usage examples

```kotlin
// Create archivarius instance
val archivarius = Archivarius.Builder(context).build()

// Log to archivarius with LogEntry
archivarius.log(JsonLogEntry("{\"message\":\"test log message\"}"))
```
