# Publishing Guide: Zetrix Connect Wallet SDK (AAR)

This guide covers all methods for publishing the Zetrix Connect Wallet SDK Android library (AAR format).

## Table of Contents

- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Required Files](#required-files)
- [Publishing Methods](#publishing-methods)
  - [Method 1: Local Maven (Testing)](#method-1-local-maven-testing)
  - [Method 2: JitPack (Easiest)](#method-2-jitpack-easiest)
  - [Method 3: Maven Central (Professional)](#method-3-maven-central-professional)
  - [Method 4: GitHub Packages](#method-4-github-packages)
- [Recommended Progression](#recommended-progression)
- [Version Management](#version-management)
- [Troubleshooting](#troubleshooting)

---

## Project Structure

This is a multi-module Android project. **Only the library module is published as AAR:**

```
zetrix-connect-wallet-sdk-android/           ← Root project directory (NOT published)
│
├── zetrix-connect-wallet/                  ← LIBRARY MODULE (PUBLISHED as AAR) ✓
│   ├── src/
│   │   └── main/
│   │       ├── java/com/zetrix/connectwallet/
│   │       │   ├── ZetrixConnectWallet.java
│   │       │   ├── callbacks/
│   │       │   ├── models/
│   │       │   ├── network/
│   │       │   ├── ui/
│   │       │   └── utils/
│   │       ├── AndroidManifest.xml
│   │       └── res/
│   └── build.gradle.kts                     ← Library build config (publishing config here)
│
├── app/                                     ← Example app (NOT published)
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/exampledapp/
│   │       │   ├── MainActivity.kt
│   │       │   └── ui/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts                     ← App build config
│
├── settings.gradle.kts                      ← Includes both modules
├── build.gradle.kts                         ← Root project build config
├── gradle.properties                        ← Project-wide properties
├── PUBLISHING.md                            ← This guide
├── README.md                                ← Project documentation
└── LICENSE                                  ← License file (TODO: add if missing)
```

**Important:**
- The **`zetrix-connect-wallet/`** module contains the library code and is what gets published to Maven repositories
- The **`app/`** is a sample Android application (Jetpack Compose) that demonstrates how to use the library
- All publishing commands should be run from the **root directory** (`zetrix-connect-wallet-sdk-android/`)
- Gradle will automatically build and publish only the library module when configured correctly
- **Namespace:** `com.zetrix.connectwallet`
- **Min SDK:** 24 (Android 7.0)
- **Target/Compile SDK:** 36 (Android 14)

---

## Prerequisites

### Required Tools

- **JDK 11+** - Java Development Kit
- **Android SDK** - Latest stable version
- **Gradle 8.0+** - Build automation
- **Git** - Version control
- **GPG** - For signing (Maven Central only)

### Required Accounts (depending on method)

- **GitHub Account** - For JitPack or GitHub Packages
- **Sonatype JIRA Account** - For Maven Central
- **GPG Key Pair** - For Maven Central signing

---

## Required Files

Before publishing, ensure these files exist in your repository:

### 1. LICENSE

Choose an appropriate open-source license:

```
zetrix-connect-wallet-sdk-android/
└── LICENSE (Apache 2.0, MIT, or other)
```

**Recommended:** Apache License 2.0 or MIT License

### 2. README.md

Must include:
- Library description
- Installation instructions
- Basic usage examples
- Requirements (min SDK version, etc.)
- License information

### 3. CHANGELOG.md

Track version history:

```markdown
# Changelog

## [1.0.0] - 2024-XX-XX
### Added
- Initial release
- Wallet connection functionality
- Authentication and signing operations
```

### 4. gradle.properties (in library module)

```properties
# Library Information
GROUP_ID=com.zetrix
ARTIFACT_ID=wallet-sdk
VERSION_NAME=1.0.0

# Project Information
POM_NAME=Zetrix Connect Wallet SDK
POM_DESCRIPTION=Android SDK for Zetrix blockchain wallet integration
POM_URL=https://github.com/your-username/zetrix-connect-wallet-sdk-android
POM_SCM_URL=https://github.com/your-username/zetrix-connect-wallet-sdk-android
POM_SCM_CONNECTION=scm:git:git://github.com/your-username/zetrix-connect-wallet-sdk-android.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://github.com/your-username/zetrix-connect-wallet-sdk-android.git

# License
POM_LICENCE_NAME=The Apache Software License, Version 2.0
POM_LICENCE_URL=http://www.apache.org/licenses/LICENSE-2.0.txt
POM_LICENCE_DIST=repo

# Developer
POM_DEVELOPER_ID=your-id
POM_DEVELOPER_NAME=Your Name
POM_DEVELOPER_EMAIL=your-email@example.com
```

---

## Publishing Methods

## Method 1: Local Maven (Testing)

**Use Case:** Testing the library integration in your example-app before public release.

### Setup

**1. Configure zetrix-connect-wallet/build.gradle.kts:**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.zetrix.connectwallet"
    // ... your android config
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.zetrix"
                artifactId = "connect-wallet-sdk"
                version = "1.0.0-SNAPSHOT"
            }
        }
    }
}
```

### Publish Locally

```bash
# Navigate to root directory
cd zetrix-connect-wallet-sdk-android

# Publish to local Maven repository (~/.m2/repository)
./gradlew :zetrix-connect-wallet:publishToMavenLocal

# Or publish to custom local directory
./gradlew :zetrix-connect-wallet:publish
```

### Use in Example App

**app/build.gradle.kts:**
```kotlin
dependencies {
    // Option 1: Use project dependency (for local development)
    implementation(project(":zetrix-connect-wallet"))

    // Option 2: Use published artifact (after publishToMavenLocal)
    // implementation("com.zetrix:connect-wallet-sdk:1.0.0-SNAPSHOT")
}
```

**settings.gradle.kts (if using mavenLocal):**
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()  // Add this to use local Maven
        google()
        mavenCentral()
    }
}
```

---

## Method 2: JitPack (Easiest)

**Use Case:** Quick public release, beta testing, open-source projects.

**Pros:**
- No account signup required
- Builds directly from GitHub
- Free for public repositories
- Automatic versioning from Git tags

**Cons:**
- Requires public GitHub repository
- Less control over build process
- Not as "official" as Maven Central

### Setup

**1. Configure zetrix-connect-wallet/build.gradle.kts:**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.zetrix.connectwallet"
    // ... your android config
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.your-username"
                artifactId = "zetrix-connect-wallet-sdk-android"
                version = "1.0.0"
            }
        }
    }
}
```

**2. Add jitpack.yml (optional, in project root):**

```yaml
jdk:
  - openjdk17
before_install:
  - sdk install java 17.0.2-open
install:
  - ./gradlew :zetrix-connect-wallet:build :zetrix-connect-wallet:publishToMavenLocal
```

### Publishing Steps

1. **Commit and push to GitHub:**
   ```bash
   git add .
   git commit -m "Release version 1.0.0"
   git push origin main
   ```

2. **Create a Git tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **Verify on JitPack:**
   - Visit: `https://jitpack.io/#your-username/zetrix-connect-wallet-sdk-android`
   - Click "Get it" on your version
   - JitPack will build automatically

### User Installation

Users add to their **settings.gradle.kts**:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Add JitPack
    }
}
```

And in **build.gradle.kts**:
```kotlin
dependencies {
    implementation("com.github.your-username:zetrix-connect-wallet-sdk-android:1.0.0")
}
```

---

## Method 3: Maven Central (Professional)

**Use Case:** Official releases, enterprise adoption, maximum credibility.

**Pros:**
- Most trusted repository
- Best discoverability
- Standard for Android libraries

**Cons:**
- Complex initial setup
- Requires GPG signing
- 1-2 day approval for first release

### Prerequisites

1. **Create Sonatype JIRA Account:**
   - Visit: https://issues.sonatype.org/secure/Signup!default.jspa
   - Create account

2. **Request Namespace:**
   - Create new ticket: https://issues.sonatype.org/secure/CreateIssue.jspa
   - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
   - Issue Type: New Project
   - Group Id: `io.github.your-username` or `com.zetrix` (requires domain verification)
   - Project URL: Your GitHub repository
   - SCM URL: `https://github.com/your-username/zetrix-connect-wallet-sdk-android.git`
   - Wait for approval (1-2 business days)

3. **Generate GPG Key:**
   ```bash
   # Generate key pair
   gpg --gen-key
   # Enter your name and email when prompted

   # List keys to get Key ID
   gpg --list-keys
   # Look for line like: pub   rsa3072 2024-01-01 [SC]
   #                              ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234

   # Export public key to key server
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

   # Export secret key for Gradle
   gpg --export-secret-keys -o secring.gpg
   ```

### Setup

**1. Configure zetrix-connect-wallet/build.gradle.kts:**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.zetrix.connectwallet"
    // ... your android config

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "io.github.your-username"
                artifactId = "zetrix-connect-wallet-sdk"
                version = "1.0.0"

                pom {
                    name.set("Zetrix Connect Wallet SDK")
                    description.set("Android SDK for Zetrix blockchain wallet integration")
                    url.set("https://github.com/your-username/zetrix-connect-wallet-sdk-android")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("your-id")
                            name.set("Your Name")
                            email.set("your-email@example.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/your-username/zetrix-connect-wallet-sdk-android.git")
                        developerConnection.set("scm:git:ssh://github.com/your-username/zetrix-connect-wallet-sdk-android.git")
                        url.set("https://github.com/your-username/zetrix-connect-wallet-sdk-android")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                credentials {
                    username = project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME")
                    password = project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}
```

**2. Add credentials to ~/.gradle/gradle.properties:**

```properties
ossrhUsername=your-jira-username
ossrhPassword=your-jira-password

signing.keyId=YOUR_KEY_ID_LAST_8_CHARS
signing.password=your-gpg-password
signing.secretKeyRingFile=/absolute/path/to/secring.gpg
```

**IMPORTANT:** Never commit credentials to Git!

### Publishing Steps

1. **Build and publish:**
   ```bash
   # From root directory
   cd zetrix-connect-wallet-sdk-android
   ./gradlew :zetrix-connect-wallet:publishReleasePublicationToOSSRHRepository
   ```

2. **Close and release staging repository:**
   - Login to: https://s01.oss.sonatype.org/
   - Go to "Staging Repositories"
   - Find your repository (iogithebyour-username-XXXX)
   - Click "Close" (validation will run)
   - If validation passes, click "Release"

3. **Wait for sync:**
   - Takes 2-4 hours to sync to Maven Central
   - Check: https://repo1.maven.org/maven2/io/github/your-username/

### User Installation

Users can now use standard Maven Central:

```kotlin
dependencies {
    implementation("io.github.your-username:zetrix-connect-wallet-sdk:1.0.0")
}
```

---

## Method 4: GitHub Packages

**Use Case:** Private repositories, GitHub-centric workflows.

**Pros:**
- Integrated with GitHub
- Good for private packages
- Free for public repos

**Cons:**
- Requires authentication even for public packages
- Less convenient for users than Maven Central

### Setup

**1. Configure library/build.gradle:**

```gradle
plugins {
    id 'com.android.library'
    id 'maven-publish'
}

android {
    // ... your android config
}

afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release

                groupId = 'com.zetrix'
                artifactId = 'wallet-sdk'
                version = '1.0.0'
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/your-username/zetrix-connect-wallet-sdk-android")
                credentials {
                    username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
```

**2. Add credentials to ~/.gradle/gradle.properties:**

```properties
gpr.user=your-github-username
gpr.key=your-github-personal-access-token
```

Generate token at: https://github.com/settings/tokens (need `write:packages` scope)

### Publishing Steps

```bash
./gradlew publish
```

### User Installation

Users need authentication in **settings.gradle**:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/your-username/zetrix-connect-wallet-sdk-android")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

---

## Recommended Progression

Follow this path for a smooth release cycle:

### Phase 1: Development (Local Maven)
```bash
# Version: 1.0.0-SNAPSHOT
./gradlew publishToMavenLocal
```
- Test integration in example-app
- Iterate quickly
- No external dependencies

### Phase 2: Beta Testing (JitPack)
```bash
# Version: 1.0.0-beta01
git tag v1.0.0-beta01
git push origin v1.0.0-beta01
```
- Share with early adopters
- Gather feedback
- Easy to update

### Phase 3: Official Release (Maven Central)
```bash
# Version: 1.0.0
./gradlew publishReleasePublicationToOSSRHRepository
```
- Stable, production-ready
- Maximum reach
- Professional distribution

---

## Version Management

### Semantic Versioning (SemVer)

Follow **MAJOR.MINOR.PATCH** format:

- **MAJOR**: Breaking changes (2.0.0)
- **MINOR**: New features, backward-compatible (1.1.0)
- **PATCH**: Bug fixes (1.0.1)

### Version Suffixes

- `-SNAPSHOT`: Development builds (1.0.0-SNAPSHOT)
- `-alpha01`: Early testing (1.0.0-alpha01)
- `-beta01`: Feature-complete testing (1.0.0-beta01)
- `-rc01`: Release candidate (1.0.0-rc01)
- No suffix: Stable release (1.0.0)

### Updating Version

**Method 1: gradle.properties**
```properties
VERSION_NAME=1.0.1
```

**Method 2: build.gradle**
```gradle
version = '1.0.1'
```

**Method 3: Git tags (JitPack)**
```bash
git tag v1.0.1
git push origin v1.0.1
```

---

## Troubleshooting

### Common Issues

#### 1. "Could not find artifact"
- **Cause:** Package not published or wrong coordinates
- **Fix:** Verify groupId, artifactId, version match exactly

#### 2. "Signing failed"
- **Cause:** GPG key issues
- **Fix:**
  ```bash
  gpg --list-keys  # Verify key exists
  echo "test" | gpg --clearsign  # Test signing
  ```

#### 3. "401 Unauthorized" (Maven Central)
- **Cause:** Wrong credentials
- **Fix:** Check `ossrhUsername` and `ossrhPassword` in gradle.properties

#### 4. JitPack build fails
- **Cause:** Missing JDK or build config
- **Fix:** Add `jitpack.yml` with correct JDK version

#### 5. "Task not found: publish"
- **Cause:** Maven publish plugin not applied
- **Fix:** Add `id 'maven-publish'` to plugins block

### Validation Checklist

Before publishing, verify:

- [ ] All source files have proper package names
- [ ] LICENSE file exists
- [ ] README.md has installation instructions
- [ ] CHANGELOG.md is updated
- [ ] Version number is correct
- [ ] Dependencies are declared properly
- [ ] ProGuard rules included (if needed)
- [ ] Library compiles without errors
- [ ] Example app successfully integrates library
- [ ] Git tag created (for JitPack)
- [ ] Credentials configured (for Maven Central)

---

## Security Best Practices

1. **Never commit credentials:**
   - Add `gradle.properties` to `.gitignore`
   - Use environment variables for CI/CD

2. **Protect GPG private key:**
   - Store securely
   - Use strong passphrase
   - Backup in safe location

3. **Use GitHub tokens carefully:**
   - Set minimal required scopes
   - Rotate regularly
   - Don't share in public repos

4. **Sign your releases:**
   - Always sign Maven Central releases
   - Verify signatures after upload

---

## Additional Resources

- [Maven Central Guide](https://central.sonatype.org/publish/publish-guide/)
- [JitPack Documentation](https://jitpack.io/docs/)
- [GitHub Packages Docs](https://docs.github.com/en/packages)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Android Library Publishing](https://developer.android.com/studio/projects/android-library)

---

## Support

For issues with:
- **JitPack:** https://github.com/jitpack/jitpack.io/issues
- **Maven Central:** https://issues.sonatype.org
- **This Library:** [Create issue on GitHub]

---

**Last Updated:** 2025-12-10
**SDK Version:** 1.0.0
**Min SDK:** 24
**Target SDK:** 36
**Namespace:** `com.zetrix.connectwallet`