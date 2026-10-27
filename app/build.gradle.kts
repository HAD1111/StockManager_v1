// build.gradle (app level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.stockmaneger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.stockmaneger"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true  // lets you access XML views safely
        compose = true
    }

}

dependencies {
    // Material Design (cards, bottom nav, text fields)
    implementation("com.google.android.material:material:1.11.0")

    // Navigation Component (moves between fragments)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // PDF generation (Step 7)
    implementation("com.itextpdf:itextg:5.5.10")
    // PDF generation
    implementation("com.itextpdf:itextg:5.5.10")


    // Excel generation (Step 7)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Barcode scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation ("androidx.compose.ui:ui:1.5.1")
    implementation ("androidx.compose.material3:material3:1.1.2")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.5.1")
    implementation ("androidx.activity:activity-compose:1.8.0")
    implementation(libs.play.services.analytics.impl)

    debugImplementation ("androidx.compose.ui:ui-tooling:1.5.1")

    testImplementation(libs.junit)
}
