plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.lostandfound"
    compileSdk = 34 // Giữ ổn định ở mức 34

    defaultConfig {
        applicationId = "com.example.lostandfound"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // 1. Firebase (Dùng BOM để tự quản lý phiên bản chuẩn)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")

    // 2. Thư viện xử lý ảnh GLIDE
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // 3. Bản đồ và Vị trí
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // 4. Giao diện và Hệ thống Android (QUAN TRỌNG: Đã khóa version)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- KHẮC PHỤC LỖI TẠI ĐÂY (CHÌA KHÓA ĐỂ FIX 10 LỖI KIA) ---
    // Ép dùng bản cũ tương thích với SDK 34, không cho nó tự nhảy lên bản 1.12.0 hay 1.16.0
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.5.7")

    // 5. Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 6. Google Sign In cũ (Ổn định)
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // 7. Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // LƯU Ý: Tuyệt đối KHÔNG thêm các dòng libs... ở đây nếu không biết nó là bản nào
}