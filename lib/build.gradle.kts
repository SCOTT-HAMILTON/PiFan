plugins {
    id("com.android.library")
    id("common-config")
    id("com.github.ben-manes.versions") version "0.39.0"
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementations(Libs.AndroidX.main)
    implementations(Libs.Compose.main)
    implementation(Libs.material)

    testImplementation(Libs.junit)
    testImplementation(Libs.mockk)
    testImplementation(Libs.hamcrest)

    androidTestImplementations(Libs.AndroidX.androidTest)
    androidTestImplementations(Libs.Compose.androidTest)
}