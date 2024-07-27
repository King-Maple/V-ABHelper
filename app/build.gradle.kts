import android.annotation.SuppressLint

plugins {
    id("com.android.application")
}

android {
    namespace = "com.flyme.update.helper"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.flyme.update.helper"
        minSdk = 30
        @SuppressLint("ExpiredTargetSdkVersion")
        targetSdk = 30
        versionCode = 225
        versionName = "2.2.5"

        renderscriptTargetApi = 21
        renderscriptSupportModeEnabled = true
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            multiDexEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        aidl =  true
        buildConfig = true
        compose = true
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

}

dependencies {
    compileOnly(fileTree("libs").include("*.jar"))

    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.4.0")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:DexKit:1.1.6")

    val libsuVersion = "6.0.0"
    implementation("com.github.topjohnwu.libsu:core:${libsuVersion}")
    implementation("com.github.topjohnwu.libsu:service:${libsuVersion}")
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")

    //全面屏适配
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")

    //高斯模糊控件
    implementation("com.github.mmin18:realtimeblurview:1.2.1")

    implementation("me.itangqi.waveloadingview:library:0.3.5")

    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    val dialogx_version = "0.0.50.beta17.1"
    implementation("com.github.kongzue.DialogX:DialogX:${dialogx_version}")
    implementation("com.github.kongzue.DialogX:DialogXKongzueStyle:${dialogx_version}")
    implementation("com.github.kongzue.DialogX:DialogXMaterialYou:${dialogx_version}")
    implementation("com.github.kongzue.DialogXSample:FileDialog:0.0.14")

    implementation("com.github.getActivity:XXPermissions:18.0")

    implementation("com.ejlchina:okhttps:3.5.3")

    implementation("commons-io:commons-io:2.11.0")
}