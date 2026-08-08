    plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.nachiket.connectra"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nachiket.connectra"
        minSdk = 23
        targetSdk = 36
        versionCode = 20
        versionName = "2.0.0"
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    ndkVersion = "28.0.13004108"
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))

    implementation(libs.glide)
    implementation(libs.circleimageview)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.okhttp)
    implementation(libs.play.services.auth)
    implementation (libs.photoview)
    implementation (libs.ucrop)
    implementation(libs.swiperefreshlayout)
    implementation(libs.shimmer)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.work.runtime)
    // WorkManager compiles against ListenableFuture; guava only reaches the runtime
    // classpath through Firebase, so it has to be declared explicitly here.
    implementation(libs.guava)
    annotationProcessor(libs.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}