plugins {
    id("com.android.application")
    id("common-config")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.32"
}

android {
    defaultConfig {
        applicationId = "com.sample.PiFan"
    }
}

dependencies {
    implementations(Libs.AndroidX.main)
    implementations(Libs.Compose.main)
    implementation(project(":lib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("com.android.volley:volley:1.2.1")
}
