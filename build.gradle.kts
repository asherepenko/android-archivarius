buildscript {
    repositories {
        jcenter()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.0")
        classpath(kotlin("gradle-plugin", version = "1.4.10"))
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

val clean by tasks.registering(Delete::class) {
    delete(rootProject.buildDir)
}
