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
    providers.environmentVariable(environment).orNull
        ?.takeIf { it.isNotBlank() }
        ?: signingProperties.getProperty(property)?.takeIf { it.isNotBlank() }

// Credentials must come from env vars or local keystore.properties — never hardcoded.
val releaseStoreFilePath = signingValue("storeFile", "WISHFEST_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "WISHFEST_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "WISHFEST_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "WISHFEST_KEY_PASSWORD")

val keystoreFile = releaseStoreFilePath?.let { path ->
    listOf(rootProject.file(path), file(path))
        .firstOrNull { it.isFile }
}

val hasReleaseSigning = keystoreFile != null &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

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
        // CI can override: -Pwishfest.versionCode=<n> -Pwishfest.versionName=<name>
        versionCode = providers.gradleProperty("wishfest.versionCode")
            .map { raw -> raw.toIntOrNull()?.takeIf { it > 0 } ?: 2 }
            .orElse(2)
            .get()
        versionName = providers.gradleProperty("wishfest.versionName")
            .map { raw -> raw.takeIf { it.isNotBlank() } ?: "1.0" }
            .orElse("1.0")
            .get()

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
                storeFile = keystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    val playCredsFile = rootProject.file("play-service-account.json")
    if (playCredsFile.isFile) {
        serviceAccountCredentials.set(playCredsFile)
    }

    defaultToAppBundles.set(true)
    track.set(providers.gradleProperty("play.track").orElse("internal"))
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
    doLast {
        // Resolve at execution time so we never rely on a stale config-time snapshot.
        fun liveSigningValue(property: String, environment: String): String? {
            val fromEnv = System.getenv(environment)?.takeIf { it.isNotBlank() }
            if (fromEnv != null) return fromEnv
            val props = Properties().apply {
                val propertiesFile = rootProject.file("keystore.properties")
                if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
            }
            return props.getProperty(property)?.takeIf { it.isNotBlank() }
        }

        val storePath = liveSigningValue("storeFile", "WISHFEST_STORE_FILE")
        val storePassword = liveSigningValue("storePassword", "WISHFEST_STORE_PASSWORD")
        val keyAlias = liveSigningValue("keyAlias", "WISHFEST_KEY_ALIAS")
        val keyPassword = liveSigningValue("keyPassword", "WISHFEST_KEY_PASSWORD")
        val resolvedKeystore = storePath?.let { path ->
            listOf(rootProject.file(path), project.file(path))
                .firstOrNull { it.isFile }
        }

        check(!storePath.isNullOrBlank()) {
            "Release storeFile is missing. Set storeFile in keystore.properties or WISHFEST_STORE_FILE."
        }
        check(resolvedKeystore?.isFile == true) {
            "Release keystore file was not found at '$storePath'."
        }
        check(!storePassword.isNullOrBlank()) {
            "Release storePassword is missing. Use keystore.properties or WISHFEST_STORE_PASSWORD."
        }
        check(!keyAlias.isNullOrBlank()) {
            "Release keyAlias is missing. Use keystore.properties or WISHFEST_KEY_ALIAS."
        }
        check(!keyPassword.isNullOrBlank()) {
            "Release keyPassword is missing. Use keystore.properties or WISHFEST_KEY_PASSWORD."
        }
    }
}

tasks.matching {
    it.name == "bundleRelease" ||
        it.name == "assembleRelease" ||
        it.name == "publishReleaseBundle"
}.configureEach {
    dependsOn(validateReleaseSigning)
}
