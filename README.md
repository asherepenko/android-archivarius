# Archivarius

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![](https://jitci.com/gh/asherepenko/android-archivarius/svg)](https://jitci.com/gh/asherepenko/android-archivarius)
[![](https://jitpack.io/v/asherepenko/android-archivarius.svg)](https://jitpack.io/#asherepenko/android-archivarius) 

Archivarius is responsible for storing, rotating, and uploading logs from an Android device to AWS S3.

## How to

**Step 1.** Add the JitPack repository to your build file

Add it in your root `build.gradle.kts` at the end of repositories:

```kotlin
allprojects {
    repositories {
        maven(url = "https://jitpack.io")
    }
}
```

**Step 2.** Add the dependency

```kotlin
dependencies {
    implementation("com.github.asherepenko:android-archivarius:x.y.z")
}
```

## Features

- `JSON` log validation
- Compress (`GZIP`) files before upload

## Initial setup

Archivarius has to be initialized with two strategies before any interaction:

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

ArchivariusAnalytics.init(object : ArchivariusAnalyticsImpl {
    override fun reportToCrashlytics(tag: String, e: Throwable) {
    }
})
```

## Usage examples

```kotlin
// Create archivarius instance
val archivarius = Archivarius.Builder(context).build()

// Log to archivarius with LogEntry
archivarius.log(JsonLogEntry("{\"message\":\"test log message\"}"))
```
