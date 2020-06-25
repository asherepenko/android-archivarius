import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("com.android.library")
    id("com.sherepenko.gradle.plugin-build-version") version "0.1.5"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("org.jetbrains.dokka") version "0.10.1"
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

val okHttpVersion = "4.7.2"
val rxJavaVersion = "2.2.19"
val workVersion = "2.3.4"

dependencies {
    api("androidx.work:work-runtime-ktx:$workVersion")
    api("androidx.work:work-rxjava2:$workVersion")
    api("com.amazonaws:aws-android-sdk-s3:2.16.12")
    api("com.squareup.okhttp3:okhttp:$okHttpVersion")
    api("io.reactivex.rxjava2:rxjava:$rxJavaVersion")
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    testImplementation("junit:junit:4.13")
    testImplementation("androidx.test:core:1.2.0")
    testImplementation("androidx.test:runner:1.2.0")
    testImplementation("androidx.test.ext:junit:1.1.1")
    testImplementation("androidx.work:work-testing:$workVersion")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
    testImplementation("org.robolectric:robolectric:4.3.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }

    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(dokka)
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
