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
//    implementation("androidx.datastore:datastore-core:1.0.0")
//    implementation("androidx.datastore:datastore-preferences:1.0.0")
//    implementation("androidx.navigation:navigation-compose:2.4.0-alpha08")
    implementation("androidx.navigation:navigation-compose:2.4.0-alpha01")
//    implementation("com.chargemap.compose:numberpicker:0.0.2")


    implementations(Libs.AndroidX.main)
    implementations(Libs.Compose.main)
    implementation(project(":lib"))
    implementation(project(":Compose-NumberPicker"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
    implementation("com.android.volley:volley:1.2.1")

}
