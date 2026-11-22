import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
}

android {
    namespace   = "page.kakkamvellytemple.app"
    compileSdk  = 36

    defaultConfig {
        applicationId = "page.kakkamvellytemple.app"
        minSdk        = 24
        targetSdk     = 36
        versionCode   = 2
        versionName   = "1.0.1"

        // Connected-backend credentials: injected from CI env vars or a local,
        // git-ignored backend.properties. Blank = unconfigured (JSON endpoint / inert).
        val bp = Properties()
        val bpFile = rootProject.file("backend.properties")
        if (bpFile.exists()) bpFile.inputStream().use { bp.load(it) }
        fun backendProp(name: String): String = System.getenv(name) ?: bp.getProperty(name) ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"${backendProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${backendProp("SUPABASE_ANON_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            // Signing credentials come ONLY from CI environment variables or a
            // local, git-ignored keystore.properties. Never commit files or values.
            val kpf = rootProject.file("keystore.properties")
            val kp = Properties()
            if (kpf.exists()) kpf.inputStream().use { kp.load(it) }
            fun prop(name: String): String? = System.getenv(name) ?: kp.getProperty(name)
            val storeFilePath = prop("SIGNING_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = prop("SIGNING_STORE_PASSWORD")
                keyAlias = prop("SIGNING_KEY_ALIAS")
                keyPassword = prop("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.takeIf { it.storeFile != null }?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    debugImplementation(libs.compose.ui.tooling)
}
