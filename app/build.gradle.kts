import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

val signingProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}
fun signingValue(property: String, environment: String): String? =
    providers.environmentVariable(environment).orNull ?: signingProperties.getProperty(property)
val releaseStoreFile = signingValue("storeFile", "WISHFEST_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "WISHFEST_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "WISHFEST_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "WISHFEST_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.nipu.wishfest"
    ndkVersion = "28.2.13676358"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nipu.wishfest"
        minSdk = 29
        targetSdk = 37
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++20")
                arguments("-DANDROID_STL=c++_shared")
            }
        }
        ndk {
            abiFilters.addAll(setOf("arm64-v8a", "x86_64"))
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

play {
    // Prefer ANDROID_PUBLISHER_CREDENTIALS (JSON string) in CI.
    // Locally you can place play-service-account.json at the repo root.
    val playCredsFile = rootProject.file("play-service-account.json")
    if (playCredsFile.isFile) {
        serviceAccountCredentials.set(playCredsFile)
    }

    defaultToAppBundles.set(true)
    track.set(providers.gradleProperty("play.track").orElse("internal"))
    // Safer default: upload as draft; promote in Play Console when ready.
    // Override with -Pplay.releaseStatus=COMPLETED
    val status = providers.gradleProperty("play.releaseStatus").orElse("DRAFT")
    releaseStatus.set(
        status.map {
            when (it.uppercase()) {
                "COMPLETED" -> ReleaseStatus.COMPLETED
                "HALTED" -> ReleaseStatus.HALTED
                "IN_PROGRESS" -> ReleaseStatus.IN_PROGRESS
                else -> ReleaseStatus.DRAFT
            }
        }
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20231013")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    val configured = hasReleaseSigning
    val keystoreFile = releaseStoreFile?.let { rootProject.file(it) }
    doLast {
        check(configured) {
            "Release signing is missing. Configure keystore.properties or the WISHFEST_STORE_* / WISHFEST_KEY_* environment variables."
        }
        check(keystoreFile?.isFile == true) { "Release keystore file does not exist." }
    }
}
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
}
