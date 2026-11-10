import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// التوقيع: متغيرات البيئة (CI) ثم keystore.properties محلي غير مرفوع
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun secret(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(propName)

val releaseStoreFile = secret("SIGNING_STORE_FILE", "storeFile")?.let(rootProject::file)
val hasReleaseSigning = releaseStoreFile?.exists() == true

val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val baseVersionCode = (project.property("VERSION_CODE") as String).toInt()

// ملاحظات الإصدارات تُضمَّن في التطبيق لصفحة "التحديثات"
val releaseNotes by tasks.registering(Copy::class) {
    from(rootProject.file("../docs/releases"))
    into(layout.buildDirectory.dir("generated/relnotes/releases"))
}
tasks.named("preBuild") { dependsOn(releaseNotes) }

android {
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/relnotes"))
    namespace = "dev.saged.grcs"
    compileSdk = 36

    defaultConfig {
        applicationId = project.property("APP_ID") as String
        minSdk = 23
        targetSdk = 36
        versionCode = baseVersionCode + (ciRunNumber ?: 0)
        versionName = project.property("VERSION_NAME") as String
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storeType = secret("SIGNING_STORE_TYPE", "storeType") ?: "pkcs12"
                storePassword = secret("SIGNING_STORE_PASSWORD", "storePassword")
                keyAlias = secret("SIGNING_KEY_ALIAS", "keyAlias") ?: "exported_key"
                keyPassword = secret("SIGNING_KEY_PASSWORD", "keyPassword") ?: secret("SIGNING_STORE_PASSWORD", "storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
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
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.kotlin_module")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("robolectric.graphicsMode", "NATIVE"); it.systemProperty("roborazzi.test.record", "true"); it.maxHeapSize = "2g" }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
