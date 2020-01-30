# Archivarius

[![](https://jitci.com/gh/asherepenko/android-archivarius/svg)](https://jitci.com/gh/asherepenko/android-archivarius)
[![](https://jitpack.io/v/asherepenko/android-archivarius.svg)](https://jitpack.io/#asherepenko/android-archivarius) 

Archivarius is responsible for storing, rotating, and uploading logs from an Android device to AWS S3.

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
Archivarius has to be initialized with a strategy

```kotlin
ArchivariusStrategy.init(object : ArchivariusStrategyImpl {
    override val isInDebugMode: Boolean = true

    override val isLogcatEnabled: Boolean = true

    override val authority: String = ""

    override val rotateFilePostfix: String = ""

    override val logName: String = "log"

    override val parentLogDir: File = File("/")

    override val logUploader: LogUploader = LogUploader()

    override val logUploadWorker: Class<out ListenableWorker> = ListenableWorker::class.java
})
```

## Usage examples

```kotlin
// Create archivarius instance
val archivarius = Archivarius.Builder(context).build()

// Log to archivarius with LogEntry
archivarius.log(JsonLogEntry("{\"message\":\"test log message\"}"))
```
