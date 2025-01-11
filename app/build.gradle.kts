plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.todoapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.todoapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 从 local.properties 读取配置
        val localProperties = project.rootProject.file("local.properties")
        if (localProperties.exists()) {
            val properties = org.jetbrains.kotlin.konan.properties.Properties()
            properties.load(localProperties.inputStream())
            
            buildConfigField("String", "VOLC_APP_ID", "\"${properties.getProperty("VOLC_APP_ID", "")}\"")
            buildConfigField("String", "VOLC_ACCESS_KEY", "\"${properties.getProperty("VOLC_ACCESS_KEY", "")}\"")
            buildConfigField("String", "VOLC_SECRET_KEY", "\"${properties.getProperty("VOLC_SECRET_KEY", "")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.volcengine:speech:1.0.0") {
        version {
            strictly("1.0.0")
        }
    }
    implementation("com.volcengine:asr:1.0.0")
    implementation("com.bytedance.speechengine:speechengine_asr_tob:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}