import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.firebase.appdistribution)
}

// google-services.json es un secreto que no vive en el repo: solo se aplica el plugin si el archivo existe
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.gms.google.services.get().pluginId)
}

android {
    namespace = "com.uniandes.campuseats"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.uniandes.campuseats"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        val apiBaseUrl = project.findProperty("apiBaseUrl")?.toString() ?: "https://backend-production-925d.up.railway.app"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            firebaseAppDistribution {
                // Se configura por propiedades de Gradle (-PdistributionTesters=a@x.com,b@y.com) para no dejar datos de ejemplo
                (project.findProperty("distributionTesters") as String?)?.let { testers = it }
                (project.findProperty("distributionReleaseNotes") as String?)?.let { releaseNotes = it }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}