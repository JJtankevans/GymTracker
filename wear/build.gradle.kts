plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace  = "com.vitoroliveira.gymtracker.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vitoroliveira.gymtracker"   // mesmo id do celular (padrão Wear)
        minSdk        = 30                                // Wear OS 3+ (Galaxy Watch 6 = Wear OS 5)
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0"
    }

    buildTypes { release { isMinifyEnabled = false } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
