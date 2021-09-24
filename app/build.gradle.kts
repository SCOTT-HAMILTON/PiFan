plugins {
    id("com.android.application")
    id("common-config")
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

android {
    signingConfigs {
        create("config") {
            storeFile = file("/home/scott/Android/upload-keystore.jks")
            storePassword = "test1234"
            keyAlias = "upload"
            keyPassword = "test1234"
        }
    }
    defaultConfig {
        applicationId = "com.sample.PiFan"
        signingConfig = signingConfigs.getByName("config")
    }
    buildTypes {
        release {
            isDebuggable = true
            isMinifyEnabled = true
            proguardFile(file("proguard-rules.pro"))
        }
    }
}

dependencies {
    val accompanist = "0.18.0"
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanist")
    implementation("com.google.accompanist:accompanist-swiperefresh:$accompanist")


    implementations(Libs.AndroidX.main)
    implementations(Libs.Compose.main)
    implementation(project(":lib"))
    implementation(project(":Compose-NumberPicker"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("com.android.volley:volley:1.2.1")

}
