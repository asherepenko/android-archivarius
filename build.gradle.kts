buildscript {
    repositories {
        jcenter()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.6.2")
        classpath(kotlin("gradle-plugin", version = "1.3.71"))
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
