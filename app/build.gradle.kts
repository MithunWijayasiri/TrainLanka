plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "lk.trainlanka.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "lk.trainlanka.app"
        minSdk = 29
        targetSdk = 37
        // Release workflow passes both from the typed version.
        versionCode = providers.gradleProperty("appVersionCode").getOrElse("1").toInt()
        versionName = providers.gradleProperty("appVersionName").getOrElse("0.1.0-beta")
    }

    // Set only by the release workflow; local and PR builds fall back to the debug key.
    val releaseKeystore = providers.environmentVariable("RELEASE_KEYSTORE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

base {
    archivesName = "TrainLanka-${android.defaultConfig.versionName}"
}

ktlint {
    version = libs.versions.ktlint.get()
}

abstract class CopyTimetable : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        source.get().asFile.copyTo(outputDir.get().file("timetable.json").asFile, overwrite = true)
    }
}

val copyTimetable =
    tasks.register<CopyTimetable>("copyTimetable") {
        source.set(layout.projectDirectory.file("../data/timetable.json"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyTimetable, CopyTimetable::outputDir)
        // Font license must ship inside the APK (OFL).
        variant.sources.assets?.addStaticSourceDirectory("licenses")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.reorderable)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
