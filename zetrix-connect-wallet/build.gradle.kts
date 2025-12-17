plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.zetrix.connectwallet"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    // Enable publishing for the release variant
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Security - EncryptedSharedPreferences for secure storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Networking - HTTP client and REST API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // JSON - Parsing and serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // QR Code - Generation and encoding
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ============================================================================
// Maven Publishing Configuration
// ============================================================================

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                // Read from gradle.properties
                groupId = project.findProperty("GROUP_ID")?.toString() ?: "com.github.shahrizalz"
                artifactId = project.findProperty("ARTIFACT_ID")?.toString() ?: "zetrix-connect-wallet-sdk"
                version = project.findProperty("VERSION_NAME")?.toString() ?: "1.0.0"

                pom {
                    name.set(project.findProperty("POM_NAME")?.toString() ?: "Zetrix Connect Wallet SDK")
                    description.set(project.findProperty("POM_DESCRIPTION")?.toString() ?: "Android SDK for Zetrix blockchain wallet integration")
                    url.set(project.findProperty("POM_URL")?.toString() ?: "https://github.com/zetrix-network/zetrix-connect-wallet-sdk-android")

                    licenses {
                        license {
                            name.set(project.findProperty("POM_LICENCE_NAME")?.toString() ?: "The Apache Software License, Version 2.0")
                            url.set(project.findProperty("POM_LICENCE_URL")?.toString() ?: "http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set(project.findProperty("POM_LICENCE_DIST")?.toString() ?: "repo")
                        }
                    }

                    developers {
                        developer {
                            id.set(project.findProperty("POM_DEVELOPER_ID")?.toString() ?: "zetrix-dev")
                            name.set(project.findProperty("POM_DEVELOPER_NAME")?.toString() ?: "Zetrix Development Team")
                            email.set(project.findProperty("POM_DEVELOPER_EMAIL")?.toString() ?: "dev@zetrix.com")
                        }
                    }

                    scm {
                        connection.set(project.findProperty("POM_SCM_CONNECTION")?.toString() ?: "scm:git:git://github.com/zetrix-network/zetrix-connect-wallet-sdk-android.git")
                        developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION")?.toString() ?: "scm:git:ssh://git@github.com/zetrix-network/zetrix-connect-wallet-sdk-android.git")
                        url.set(project.findProperty("POM_SCM_URL")?.toString() ?: "https://github.com/zetrix-network/zetrix-connect-wallet-sdk-android")
                    }
                }
            }
        }
    }
}
