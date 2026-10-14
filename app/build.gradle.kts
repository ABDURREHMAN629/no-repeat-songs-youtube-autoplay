plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.norepeat.songs"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.norepeat.songs"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Signed with the local debug key so a downloaded release installs as an
            // update over a version built from Android Studio on the same computer.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.org.json) // real org.json for parser tests (Android's is a stub on the JVM)
}
