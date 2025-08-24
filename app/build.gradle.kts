plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.google.gms.google.services)
	alias(libs.plugins.google.firebase.crashlytics)
}

android {
	namespace = "com.example.triviaappv2"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.example.triviaappv2"
		minSdk = 27
		targetSdk = 33
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildFeatures {
		viewBinding = true
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions {
		jvmTarget = "11"
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.firebase.auth.ktx)
	implementation(libs.firebase.auth)
	implementation(libs.androidx.espresso.core)
	implementation(libs.firebase.firestore)
	implementation(libs.firebase.storage)
	implementation(libs.firebase.storage.ktx)
	implementation(libs.glide)
	implementation(libs.firebase.crashlytics)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	implementation(libs.ion)
	implementation(libs.androidx.work.runtime.ktx)
}