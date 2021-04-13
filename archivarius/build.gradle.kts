import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("com.android.library")
    id("com.sherepenko.gradle.plugin-build-version") version "0.2.3"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    id("org.jetbrains.dokka") version "1.4.30"
    kotlin("android")
    kotlin("android.extensions")
}

val archivesBaseName = "android-archivarius"

group = "com.github.asherepenko"
version = buildVersion.versionName

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(30)
        versionCode = buildVersion.versionCode
        versionName = buildVersion.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        setProperty("archivesBaseName", archivesBaseName)
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    lintOptions {
        ignore("InvalidPackage")
        disable("InvalidPeriodicWorkRequestInterval")
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        getByName("release") {
            isZipAlignEnabled = true
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

ktlint {
    verbose.set(true)
    android.set(true)

    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
}

val okHttpVersion = "4.9.1"
val rxJavaVersion = "2.2.21"
val workVersion = "2.5.0"

dependencies {
    api("androidx.work:work-runtime-ktx:$workVersion")
    api("androidx.work:work-rxjava2:$workVersion")
    api("com.amazonaws:aws-android-sdk-s3:2.22.6")
    api("com.squareup.okhttp3:okhttp:$okHttpVersion")
    api("io.reactivex.rxjava2:rxjava:$rxJavaVersion")
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    testImplementation("junit:junit:4.13.1")
    testImplementation("androidx.test:core:1.3.0")
    testImplementation("androidx.test:runner:1.3.0")
    testImplementation("androidx.test.ext:junit:1.1.2")
    testImplementation("androidx.work:work-testing:$workVersion")
    testImplementation("com.google.truth:truth:1.1.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
    testImplementation("io.mockk:mockk:1.10.6")
    testImplementation("org.robolectric:robolectric:4.5.1")
}

tasks {
    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(dokkaHtml)
    }

    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }

    artifacts {
        archives(javadocJar)
        archives(sourcesJar)
    }

    afterEvaluate {
        val testJar by registering(Jar::class) {
            archiveClassifier.set("testing")
            from(named("compileDebugUnitTestKotlin").get() as KotlinCompile)
        }

        artifacts {
            archives(testJar)
        }
    }
}
