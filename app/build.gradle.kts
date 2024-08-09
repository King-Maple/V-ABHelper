plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.yowal.updatehelper"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.yowal.updatehelper"
        minSdk = 30
        targetSdk = 34
        versionCode = 302
        versionName = "3.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++20")
            }
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    buildTypes {
        release {
            multiDexEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
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
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
        aidl = true
    }
}

dependencies {
    compileOnly(fileTree("libs").include("*.jar"))


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    //libsu
    implementation(libs.libSuCore)
    implementation(libs.libSuio)
    implementation(libs.libSuNio)
    implementation(libs.libSuService)

    //dialogx
    implementation(libs.dialogX)
    implementation(libs.filedialog)

    implementation(libs.immersionbar)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}