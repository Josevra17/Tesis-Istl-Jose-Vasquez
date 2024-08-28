plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.firebase_v3"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.firebase_v3"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    aaptOptions {
        noCompress("tflite")
    }
    buildFeatures {
        viewBinding = true
        mlModelBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.espresso.intents)
    implementation ("com.github.jose-jhr:Library-CameraX:1.0.8")
    implementation ("org.tensorflow:tensorflow-lite-support:+")
    implementation ("org.tensorflow:tensorflow-lite:+")
    implementation ("org.tensorflow:tensorflow-lite-metadata:0.1.0")
    implementation(libs.android.gif.drawable)
    implementation (libs.core.ktx)
    implementation (libs.material)
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation (libs.constraintlayout)
    implementation (libs.core)
    implementation ("androidx.core:core:1.13.1")

}