# AAR Library Development Tasks

Comprehensive task list for porting Zetrix Connect Wallet SDK from Flutter to Java Android Library (AAR).

**Project:** Zetrix Connect Wallet SDK - Android (for connecting applications to Zetrix wallets)
**Target:** AAR library + Example App
**Base:** Flutter Connect Wallet SDK (1,500+ lines, 15 core files)

**Important Note:** This is a **Connect Wallet SDK** for dApps/applications to connect to external Zetrix wallet applications (Zetrix Wallet, PIXA, MyID, MUMA). It is NOT a wallet SDK for creating wallets. The SDK enables:
- Connecting to external wallet apps via WebSocket
- Requesting authentication from wallets
- Requesting wallets to sign messages/transactions
- Requesting wallets to send transactions on behalf of the user

---

## Progress Overview

- [x] **Phase 1:** Project Setup (Tasks 1-2) ✅ **COMPLETED**
- [ ] **Phase 2:** Core Components (Tasks 3-5)
- [ ] **Phase 3:** Infrastructure (Tasks 6-8)
- [ ] **Phase 4:** Main SDK (Tasks 9-13)
- [ ] **Phase 5:** Example App (Tasks 14-16)
- [ ] **Phase 6:** Testing & Documentation (Tasks 17-18)

---

## Phase 1: Project Setup

### Task 1: Set up Android Library Project Structure
**Status:** ✅ Completed (2024-12-09)
**Priority:** High
**Estimated Effort:** 2-3 hours

**Description:**
Create the multi-module Gradle project structure with library and example-app modules.

**Subtasks:**
- [x] Create root `build.gradle` with Android Gradle Plugin
- [x] Create `settings.gradle` including both modules
- [x] Set up `zetrix-connect-wallet/` library module
  - [x] Create `build.gradle` (library plugin)
  - [x] Configure `AndroidManifest.xml`
  - [x] Set min/target SDK versions (min: 21, compileSdk: 36)
  - [x] Configure build types (debug/release)
- [x] Set up `app/` app module
  - [x] Create `build.gradle` (application plugin)
  - [x] Configure `AndroidManifest.xml` with permissions
  - [x] Add app icon and basic resources
- [x] Create `gradle.properties` for project-wide settings
- [x] Create `.gitignore` for Android projects
- [x] Create `gradle/wrapper/` with Gradle 8.13

**Acceptance Criteria:**
- ✅ Project builds successfully (`./gradlew build`)
- ✅ Both modules are recognized by Gradle
- ✅ No compilation errors
- ✅ Example app can reference library module

**Files Created:**
```
example-dapp-android-test-rename/
├── build.gradle.kts ✅
├── settings.gradle.kts ✅
├── gradle.properties ✅
├── .gitignore ✅
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar ✅
│       └── gradle-wrapper.properties ✅ (Gradle 8.13)
├── zetrix-connect-wallet/
│   ├── build.gradle.kts ✅
│   └── src/main/AndroidManifest.xml ✅
└── app/
    ├── build.gradle.kts ✅
    └── src/main/AndroidManifest.xml ✅
```

**Dependencies:**
- None (first task)

**References:**
- Flutter: N/A (project setup)
- Docs: [Android Library Guide](https://developer.android.com/studio/projects/android-library)

---

### Task 2: Define Java Package Structure
**Status:** ✅ Completed (2024-12-09)
**Priority:** High
**Estimated Effort:** 1 hour

**Description:**
Create the Java package structure mirroring Flutter's modular architecture.

**Subtasks:**
- [x] Create base package: `com.zetrix.connectwallet`
- [x] Create subpackages:
  - [x] `com.zetrix.connectwallet.core` (WalletSocket, main SDK)
  - [x] `com.zetrix.connectwallet.blockchain` (ChainSDK)
  - [x] `com.zetrix.connectwallet.utils` (utilities)
  - [x] `com.zetrix.connectwallet.helpers` (helpers)
  - [x] `com.zetrix.connectwallet.navigation` (deep linking to wallet apps)
  - [x] `com.zetrix.connectwallet.ui` (QR code - optional)
  - [x] `com.zetrix.connectwallet.constants` (constants)
  - [x] `com.zetrix.connectwallet.callbacks` (async callbacks)
  - [x] `com.zetrix.connectwallet.models` (data models)

**Acceptance Criteria:**
- ✅ All package directories exist
- ✅ Each package has a `.gitkeep` or placeholder class
- ✅ Package names follow Android conventions

**Files Created:**
```
zetrix-connect-wallet/src/main/java/com/zetrix/connectwallet/
├── ZetrixConnectWallet.java (placeholder) ✅
├── core/ ✅
├── blockchain/ ✅
├── utils/ ✅
├── helpers/ ✅
├── navigation/ ✅
├── ui/ ✅
├── constants/ ✅
├── callbacks/ ✅
└── models/ ✅
```

**Dependencies:**
- Task 1 (project structure)

**References:**
- Flutter: `lib/src/` directory structure
- Docs: [Java Package Naming](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html)

---

## Phase 2: Core Components

### Task 3: Port Constants Module
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 2 hours

**Description:**
Port `zetrix_constants.dart` to `ZetrixConstants.java` with all SDK configuration constants.

**Subtasks:**
- [ ] Create `ZetrixConstants.java`
- [ ] Port SDK operation types (AUTH, SIGN_MESSAGE, SIGN_BLOB, etc.)
- [ ] Port result codes and messages
- [ ] Port type info (storage names, source types)
- [ ] Port host parameters for wallet apps (Zetrix, PIXA, MyID, MUMA)
- [ ] Port app names and error codes
- [ ] Port WebSocket URLs (mainnet/testnet)
- [ ] Convert Dart maps to Java constants (static final)

**Acceptance Criteria:**
- ✓ All constants from Flutter are present
- ✓ Proper Java naming conventions (UPPER_CASE)
- ✓ Grouped logically with inner classes
- ✓ Well-documented with Javadoc
- ✓ Compiles without errors

**Files Created:**
- `com/zetrix/connectwallet/constants/ZetrixConstants.java`

**Dependencies:**
- Task 2 (package structure)

**References:**
- Flutter: `lib/src/constants/zetrix_constants.dart` (92 lines)
- Pattern: Static final constants, inner classes for grouping

**Example:**
```java
public class ZetrixConstants {
    // WebSocket URLs
    public static final String WSS_MAINNET = "wss://wscw.zetrix.com";
    public static final String WSS_TESTNET = "wss://test-wscw.zetrix.com";

    // SDK Operations
    public static class Operations {
        public static final String AUTH = "auth";
        public static final String SIGN_MESSAGE = "signMessage";
        // ... more
    }
}
```

---

### Task 4: Port Helper Classes
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 3-4 hours

**Description:**
Port SessionHelper, ValidationHelper, and SocketDataBuilder from Flutter.

**Subtasks:**

**4a. SessionHelper.java**
- [ ] Create `SessionHelper.java`
- [ ] Port `createSessionId()` - UUID v4 generation
- [ ] Port `warnIncorrectLog()` - logging helper
- [ ] Port `fixedRequestParameters()` - parameter formatting
- [ ] Port `resReturnedData()` - response data extraction

**4b. ValidationHelper.java**
- [ ] Create `ValidationHelper.java`
- [ ] Port `verifyBlobParam()` - blob parameter validation
- [ ] Port `verifyTransactionParam()` - transaction validation
- [ ] Add custom exception classes if needed

**4c. SocketDataBuilder.java**
- [ ] Create `SocketDataBuilder.java`
- [ ] Port `createQrSocketData()` - QR mode data builder
- [ ] Port `createSocketData()` - standard WebSocket data builder
- [ ] Include app name and package ID in messages

**Acceptance Criteria:**
- ✓ All three helper classes compile
- ✓ UUID generation works correctly
- ✓ Validation logic matches Flutter behavior
- ✓ Socket data format matches Flutter output
- ✓ Unit tests pass (basic validation)

**Files Created:**
- `com/zetrix/connectwallet/helpers/SessionHelper.java`
- `com/zetrix/connectwallet/helpers/ValidationHelper.java`
- `com/zetrix/connectwallet/helpers/SocketDataBuilder.java`

**Dependencies:**
- Task 3 (constants)

**References:**
- Flutter: `lib/src/helpers/session_helper.dart` (61 lines)
- Flutter: `lib/src/helpers/validation_helper.dart` (28 lines)
- Flutter: `lib/src/helpers/socket_data_builder.dart` (47 lines)

---

### Task 5: Port Utility Classes
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 4-5 hours

**Description:**
Port CryptoUtils, GeneralUtils, DeviceUtils, and Logger from Flutter.

**Subtasks:**

**5a. CryptoUtils.java**
- [ ] Create `CryptoUtils.java`
- [ ] Port `hmacStr()` - SHA256 hashing function
- [ ] Use `java.security.MessageDigest`
- [ ] Add unit tests for hash consistency

**5b. GeneralUtils.java**
- [ ] Create `GeneralUtils.java`
- [ ] Port `isJSON()` - JSON validation
- [ ] Add additional utility methods as needed

**5c. DeviceUtils.java**
- [ ] Create `DeviceUtils.java`
- [ ] Port `isMobile()` - platform detection (always true for Android)
- [ ] Port `getDeviceInfo()` - device model, OS version
- [ ] Port `getAppName()` - retrieve app name from PackageManager
- [ ] Port `getAppPackageId()` - get package name
- [ ] Port `getAppInfo()` - combined app metadata
- [ ] Use `android.os.Build` and `PackageManager`

**5d. ZetrixLogger.java**
- [ ] Create `ZetrixLogger.java`
- [ ] Port logging levels (ALL, FINE, INFO, WARNING, SEVERE, OFF)
- [ ] Integrate with Android Log or Timber
- [ ] Create per-module logger instances
- [ ] Add log level configuration

**Acceptance Criteria:**
- ✓ SHA256 hashing produces correct output
- ✓ JSON validation works correctly
- ✓ Device info retrieval works on real devices
- ✓ App name/package retrieval is correct
- ✓ Logger outputs to Android logcat
- ✓ All utilities compile and run

**Files Created:**
- `com/zetrix/connectwallet/utils/CryptoUtils.java`
- `com/zetrix/connectwallet/utils/GeneralUtils.java`
- `com/zetrix/connectwallet/utils/DeviceUtils.java`
- `com/zetrix/connectwallet/utils/ZetrixLogger.java`

**Dependencies:**
- Task 2 (package structure)
- Android SDK APIs (Build, PackageManager)

**References:**
- Flutter: `lib/src/utils/crypto_utils.dart` (14 lines)
- Flutter: `lib/src/utils/general_utils.dart` (15 lines)
- Flutter: `lib/src/utils/device_utils.dart` (96 lines)
- Flutter: `lib/src/utils/logger.dart` (64 lines)

---

## Phase 3: Infrastructure

### Task 6: Implement Secure Storage
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 4-5 hours

**Description:**
Replace `flutter_secure_storage` with Android Keystore + EncryptedSharedPreferences.

**Subtasks:**
- [ ] Create `StorageUtils.java`
- [ ] Set up Android Keystore integration
- [ ] Implement EncryptedSharedPreferences wrapper
- [ ] Port `getSessionId()` - retrieve session ID
- [ ] Port `setAuthData()` - store session + address
- [ ] Port `getAddress()` - retrieve wallet address
- [ ] Port `disconnectRemoveStorage()` - clear session data
- [ ] Port `setLocalStorage()` - generic key-value setter
- [ ] Port `getLocalStorage()` - generic key-value getter
- [ ] Add error handling for encryption failures
- [ ] Test on different Android versions (API 21+)

**Acceptance Criteria:**
- ✓ Data is encrypted at rest
- ✓ Survives app restarts
- ✓ Works on API 21-34+
- ✓ Session ID storage/retrieval works
- ✓ Address storage/retrieval works
- ✓ Clear storage works correctly
- ✓ No plaintext storage of sensitive data

**Files Created:**
- `com/zetrix/connectwallet/utils/StorageUtils.java`

**Dependencies:**
- Task 1 (project setup for AndroidX dependencies)
- AndroidX Security library

**References:**
- Flutter: `lib/src/utils/storage_utils.dart` (32 lines)
- Android: [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- Android: [Keystore System](https://developer.android.com/training/articles/keystore)

**Gradle Dependency:**
```gradle
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

---

### Task 7: Port ChainSDK with HTTP Client
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 3-4 hours

**Description:**
Port ChainSDK for blockchain API calls using OkHttp/Retrofit.

**Subtasks:**
- [ ] Create `ChainSDK.java`
- [ ] Set up Retrofit interface for blockchain API
- [ ] Port `getAccountNonce()` - retrieve account nonce
- [ ] Configure HTTP client (timeouts, interceptors)
- [ ] Add response models (JSON parsing with Gson/Moshi)
- [ ] Implement callback-based async handling
- [ ] Add error handling for network failures
- [ ] Support mainnet/testnet URL switching

**Acceptance Criteria:**
- ✓ HTTP requests succeed to Zetrix endpoints
- ✓ Account nonce retrieval works
- ✓ Proper error handling for network issues
- ✓ Callbacks execute on main thread
- ✓ Testnet/mainnet URLs configurable
- ✓ Response parsing works correctly

**Files Created:**
- `com/zetrix/connectwallet/blockchain/ChainSDK.java`
- `com/zetrix/connectwallet/blockchain/ApiService.java` (Retrofit interface)
- `com/zetrix/connectwallet/models/NonceResponse.java` (response model)

**Dependencies:**
- Task 3 (constants for URLs)
- Task 12 (callback interfaces)
- OkHttp, Retrofit, Gson dependencies

**References:**
- Flutter: `lib/src/blockchain/chain_sdk.dart` (41 lines)
- Endpoint: Wallet API for account queries

**Gradle Dependencies:**
```gradle
implementation 'com.squareup.okhttp3:okhttp:5.0.0-alpha.11'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.google.code.gson:gson:2.10.1'
```

---

### Task 8: Implement WebSocket Handler
**Status:** Not Started
**Priority:** Critical
**Estimated Effort:** 6-8 hours

**Description:**
Port WalletSocket for WebSocket communication using OkHttp WebSocket.

**Subtasks:**
- [ ] Create `WalletSocket.java`
- [ ] Implement OkHttp WebSocket client
- [ ] Port connection management (connect/disconnect)
- [ ] Port message sending/receiving
- [ ] Implement promise pool for async responses
- [ ] Add auto-reconnection logic
- [ ] Handle WebSocket lifecycle events
- [ ] Support mainnet/testnet WebSocket URLs
- [ ] Add connection timeout handling
- [ ] Implement heartbeat/ping-pong
- [ ] Thread-safe message queue
- [ ] Proper cleanup on disconnect

**Acceptance Criteria:**
- ✓ Successfully connects to Zetrix WebSocket servers
- ✓ Messages send and receive correctly
- ✓ Promise pool matches requests to responses
- ✓ Auto-reconnection works on connection drop
- ✓ No memory leaks on disconnect
- ✓ Thread-safe operations
- ✓ Callbacks execute on main thread
- ✓ Handles network changes gracefully

**Files Created:**
- `com/zetrix/connectwallet/core/WalletSocket.java`
- `com/zetrix/connectwallet/core/PromisePool.java` (optional helper)

**Dependencies:**
- Task 3 (constants for WebSocket URLs)
- Task 5d (logger)
- Task 12 (callback interfaces)
- OkHttp dependency

**References:**
- Flutter: `lib/src/core/wallet_socket.dart` (130 lines)
- WebSocket URLs: `wss://wscw.zetrix.com` (mainnet), `wss://test-wscw.zetrix.com` (testnet)

**Gradle Dependency:**
```gradle
implementation 'com.squareup.okhttp3:okhttp:5.0.0-alpha.11'
```

---

## Phase 4: Main SDK

### Task 9: Port Main ZetrixWalletConnect Class
**Status:** Not Started
**Priority:** Critical
**Estimated Effort:** 8-10 hours

**Description:**
Port the main ZetrixWalletConnect class as ZetrixConnectWallet with all core functionality for connecting applications to Zetrix wallets.

**Subtasks:**
- [ ] Create `ZetrixConnectWallet.java`
- [ ] Implement Builder pattern for configuration
- [ ] Implement singleton pattern (thread-safe)
- [ ] Port initialization logic
- [ ] Port `connect()` - establish WebSocket connection to wallet
- [ ] Port `auth()` - request authentication from wallet
- [ ] Port `signMessage()` - request wallet to sign text messages
- [ ] Port `signBlob()` - request wallet to sign binary data
- [ ] Port `authAndSignMessage()` - combined authentication and signing
- [ ] Port `sendTransaction()` - request wallet to sign and send transactions
- [ ] Port `verifyVC()` - verify credentials via wallet
- [ ] Port `getVP()` - get verifiable presentation from wallet
- [ ] Port `getNonce()` - get account nonce from blockchain
- [ ] Port `disconnect()` - cleanup and close wallet connections
- [ ] Integrate WalletSocket
- [ ] Integrate StorageUtils
- [ ] Integrate ChainSDK
- [ ] Add lifecycle management
- [ ] Handle configuration (appType, testnet, showQr)

**Acceptance Criteria:**
- ✓ SDK initializes successfully
- ✓ All public API methods work
- ✓ Callbacks execute correctly
- ✓ Session persistence works
- ✓ Reconnection works after disconnect
- ✓ Builder pattern works as expected
- ✓ Thread-safe singleton
- ✓ No memory leaks
- ✓ Proper error handling
- ✓ Integration with all components works

**Files Created:**
- `com/zetrix/connectwallet/ZetrixConnectWallet.java`
- `com/zetrix/connectwallet/ZetrixConnectWallet.Builder.java` (inner class or separate)

**Dependencies:**
- Task 3 (constants)
- Task 4 (helpers)
- Task 5 (utils)
- Task 6 (storage)
- Task 7 (ChainSDK)
- Task 8 (WalletSocket)
- Task 12 (callbacks)

**References:**
- Flutter: `lib/src/core/wallet_connect.dart` (689 lines)
- Primary entry point for SDK

**Example Usage:**
```java
ZetrixConnectWallet sdk = new ZetrixConnectWallet.Builder(context)
    .setAppType("myapp")
    .setTestnet(true)
    .setShowQr(true)
    .build();

sdk.auth(new AuthCallback() {
    @Override
    public void onSuccess(String address) {
        // Handle success - wallet authenticated
    }

    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

---

### Task 10: Implement QR Code Generation (Optional)
**Status:** Not Started
**Priority:** Medium
**Estimated Effort:** 2-3 hours

**Description:**
Port QR code generation functionality using ZXing library (returns Bitmap, no UI).

**Subtasks:**
- [ ] Create `QrCodeGenerator.java`
- [ ] Integrate ZXing core library
- [ ] Implement `generateQrCode(String data)` - returns Bitmap
- [ ] Add size configuration (width, height)
- [ ] Add error correction level options
- [ ] Add color customization (optional)
- [ ] Handle encoding errors

**Acceptance Criteria:**
- ✓ Generates valid QR codes
- ✓ Bitmap output works
- ✓ Scannable by wallet apps
- ✓ Configurable size
- ✓ No UI dependencies (library code only)

**Files Created:**
- `com/zetrix/connectwallet/ui/QrCodeGenerator.java`

**Dependencies:**
- Task 2 (package structure)
- ZXing dependency

**References:**
- Flutter: `lib/src/ui/qr_code.dart` (71 lines, includes UI)
- Note: Android version is UI-less, returns Bitmap

**Gradle Dependency:**
```gradle
implementation 'com.google.zxing:core:3.5.0'
```

---

### Task 11: Implement Deep Linking to Wallet Apps
**Status:** Not Started
**Priority:** Medium
**Estimated Effort:** 3-4 hours

**Description:**
Port deep linking functionality to launch external Zetrix wallet applications using Android Intents.

**Subtasks:**
- [ ] Create `LinkTo.java`
- [ ] Port `launchApp()` - launch external wallet apps
- [ ] Port `createHostParam()` - build deep link parameters
- [ ] Support wallet apps: Zetrix, PIXA, MyID, MUMA
- [ ] Implement Android Intent system
- [ ] Handle app not installed scenarios
- [ ] Add callback for launch success/failure
- [ ] Platform-specific URL schemes

**Acceptance Criteria:**
- ✓ Launches wallet apps correctly
- ✓ Deep link parameters formatted correctly
- ✓ Handles missing apps gracefully
- ✓ Works on different Android versions
- ✓ Callback system works

**Files Created:**
- `com/zetrix/connectwallet/navigation/LinkTo.java`

**Dependencies:**
- Task 3 (constants for wallet app info)
- Task 5 (DeviceUtils for app info)
- Android Intent system

**References:**
- Flutter: `lib/src/navigation/link_to.dart` (97 lines)
- Supported wallets: Zetrix, PIXA, MyID, MUMA

---

### Task 12: Create Callback Interfaces
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 2-3 hours

**Description:**
Create callback interfaces for async operations to replace Flutter Futures.

**Subtasks:**
- [ ] Create `WalletCallback.java` - generic callback interface
- [ ] Create `AuthCallback.java` - authentication callback
- [ ] Create `SignCallback.java` - signing operations callback
- [ ] Create `TransactionCallback.java` - transaction callback
- [ ] Create `ConnectionCallback.java` - WebSocket connection callback
- [ ] Create `ErrorCallback.java` - error handling callback
- [ ] Add generic success/error patterns
- [ ] Document callback threading behavior

**Acceptance Criteria:**
- ✓ All callback interfaces compile
- ✓ Generic and specific callbacks available
- ✓ Well-documented with Javadoc
- ✓ Consistent naming and patterns
- ✓ Support for success, error, and progress callbacks

**Files Created:**
- `com/zetrix/connectwallet/callbacks/WalletCallback.java`
- `com/zetrix/connectwallet/callbacks/AuthCallback.java`
- `com/zetrix/connectwallet/callbacks/SignCallback.java`
- `com/zetrix/connectwallet/callbacks/TransactionCallback.java`
- `com/zetrix/connectwallet/callbacks/ConnectionCallback.java`
- `com/zetrix/connectwallet/callbacks/ErrorCallback.java`

**Dependencies:**
- Task 2 (package structure)

**References:**
- Flutter: Future-based async patterns
- Android: Callback pattern examples

**Example:**
```java
public interface AuthCallback {
    void onSuccess(String address);
    void onError(String error);
}
```

---

### Task 13: Add Library Dependencies
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 1 hour

**Description:**
Configure all required dependencies in library's build.gradle.

**Subtasks:**
- [ ] Add OkHttp for WebSocket and HTTP
- [ ] Add Retrofit for REST API
- [ ] Add Gson for JSON parsing
- [ ] Add AndroidX Security for EncryptedSharedPreferences
- [ ] Add Timber for logging (optional)
- [ ] Add ZXing for QR codes (optional)
- [ ] Configure dependency versions
- [ ] Add ProGuard rules if needed
- [ ] Document all dependencies

**Acceptance Criteria:**
- ✓ All dependencies resolve correctly
- ✓ No version conflicts
- ✓ Library builds successfully
- ✓ Dependencies documented in README

**Files Modified:**
- `zetrix-connect-wallet-sdk/build.gradle`

**Dependencies:**
- Task 1 (project setup)

**Gradle Dependencies:**
```gradle
dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'

    // Networking
    implementation 'com.squareup.okhttp3:okhttp:5.0.0-alpha.11'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'

    // Security
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // Logging (optional)
    implementation 'com.jakewharton.timber:timber:5.0.1'

    // QR Code (optional)
    implementation 'com.google.zxing:core:3.5.0'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.3.1'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

## Phase 5: Example App

### Task 14: Set up Example App Android Project
**Status:** Not Started
**Priority:** Medium
**Estimated Effort:** 2-3 hours

**Description:**
Create the example Android app structure with basic UI setup.

**Subtasks:**
- [ ] Configure `example-app/build.gradle`
- [ ] Set up AndroidManifest.xml with required permissions
- [ ] Add dependency on library module
- [ ] Create basic app structure (Activity, layouts)
- [ ] Set up Material Design theme
- [ ] Create navigation structure
- [ ] Add app icon and branding
- [ ] Configure build variants (debug/release)

**Acceptance Criteria:**
- ✓ Example app builds successfully
- ✓ Library module dependency works
- ✓ App launches on device/emulator
- ✓ Basic UI structure in place
- ✓ Permissions declared correctly

**Files Created:**
- `example-app/build.gradle` (updated)
- `example-app/src/main/AndroidManifest.xml` (updated)
- `example-app/src/main/java/com/zetrix/example/MainActivity.java`
- `example-app/src/main/res/layout/activity_main.xml`
- `example-app/src/main/res/values/themes.xml`

**Dependencies:**
- Task 1 (project structure)
- Task 9 (main SDK)

**Permissions Required:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

### Task 15: Integrate Library into Example App
**Status:** Not Started
**Priority:** Medium
**Estimated Effort:** 2-3 hours

**Description:**
Integrate the Zetrix Wallet SDK library into the example app.

**Subtasks:**
- [ ] Initialize SDK in Application class or MainActivity
- [ ] Configure SDK with test credentials
- [ ] Set up callback handlers
- [ ] Add error handling
- [ ] Test library integration
- [ ] Verify all SDK methods accessible
- [ ] Add logging for debugging

**Acceptance Criteria:**
- ✓ SDK initializes without errors
- ✓ Library classes accessible from app
- ✓ Callbacks work correctly
- ✓ No runtime errors
- ✓ Logging shows SDK activity

**Files Modified:**
- `example-app/src/main/java/com/zetrix/example/MainActivity.java`
- `example-app/build.gradle` (add library dependency)

**Dependencies:**
- Task 9 (main SDK)
- Task 14 (example app setup)

**Example Integration:**
```java
public class MainActivity extends AppCompatActivity {
    private ZetrixConnectWallet sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Connect Wallet SDK
        sdk = new ZetrixConnectWallet.Builder(this)
            .setAppType("example-app")
            .setTestnet(true)
            .setShowQr(true)
            .build();
    }
}
```

---

### Task 16: Implement Example App UI
**Status:** Not Started
**Priority:** Medium
**Estimated Effort:** 5-6 hours

**Description:**
Create comprehensive UI demonstrating all SDK features.

**Subtasks:**
- [ ] Create connection flow UI
  - [ ] Connect button
  - [ ] Connection status display
  - [ ] Disconnect button
- [ ] Create authentication flow UI
  - [ ] Auth button
  - [ ] Display connected address
  - [ ] Session info display
- [ ] Create signing flows UI
  - [ ] Sign message input and button
  - [ ] Sign blob button
  - [ ] Display signature results
- [ ] Create transaction flow UI
  - [ ] Transaction form (recipient, amount)
  - [ ] Send transaction button
  - [ ] Transaction status display
- [ ] Add QR code display (if enabled)
- [ ] Add error message display
- [ ] Add loading indicators
- [ ] Implement proper Material Design
- [ ] Add navigation between features
- [ ] Add copy-to-clipboard functionality

**Acceptance Criteria:**
- ✓ All SDK features demonstrated
- ✓ UI is intuitive and user-friendly
- ✓ Error messages display correctly
- ✓ Loading states shown appropriately
- ✓ QR codes display (if feature enabled)
- ✓ Connection status visible
- ✓ Wallet address displayed after auth
- ✓ Signatures displayed after signing
- ✓ Transaction results shown

**Files Created/Modified:**
- `example-app/src/main/res/layout/activity_main.xml`
- `example-app/src/main/res/layout/fragment_connection.xml`
- `example-app/src/main/res/layout/fragment_auth.xml`
- `example-app/src/main/res/layout/fragment_sign.xml`
- `example-app/src/main/res/layout/fragment_transaction.xml`
- `example-app/src/main/java/com/zetrix/example/` (Fragment classes)

**Dependencies:**
- Task 15 (library integration)

**References:**
- Flutter: `zetrix_connect_wallet_sdk/example/lib/main.dart`

---

## Phase 6: Testing & Documentation

### Task 17: Write Unit Tests
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 6-8 hours

**Description:**
Create comprehensive unit tests for all core functionality.

**Subtasks:**
- [ ] Set up test infrastructure (JUnit, Mockito)
- [ ] Test CryptoUtils
  - [ ] SHA256 hash correctness
  - [ ] Hash consistency
- [ ] Test GeneralUtils
  - [ ] JSON validation
- [ ] Test SessionHelper
  - [ ] UUID generation format
  - [ ] Session ID uniqueness
- [ ] Test ValidationHelper
  - [ ] Parameter validation logic
- [ ] Test SocketDataBuilder
  - [ ] Data format correctness
- [ ] Test StorageUtils (instrumented tests)
  - [ ] Encryption/decryption
  - [ ] Session storage
  - [ ] Address storage
- [ ] Test ChainSDK (mock HTTP)
  - [ ] Nonce retrieval
  - [ ] Error handling
- [ ] Test WalletSocket (mock WebSocket)
  - [ ] Connection logic
  - [ ] Message handling
  - [ ] Promise pool
- [ ] Test ZetrixConnectWallet (integration tests)
  - [ ] Initialization
  - [ ] Auth flow with wallet
  - [ ] Signing operations via wallet
- [ ] Achieve >80% code coverage
- [ ] Add CI/CD test automation

**Acceptance Criteria:**
- ✓ All tests pass
- ✓ Code coverage >80%
- ✓ Tests run in CI/CD
- ✓ No flaky tests
- ✓ Mock external dependencies
- ✓ Instrumented tests work on devices

**Files Created:**
- `zetrix-connect-wallet-sdk/src/test/java/com/zetrix/connectwallet/` (unit tests)
- `zetrix-connect-wallet-sdk/src/androidTest/java/com/zetrix/connectwallet/` (instrumented tests)

**Dependencies:**
- All implementation tasks (3-13)

**Gradle Dependencies:**
```gradle
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.3.1'
testImplementation 'org.mockito:mockito-inline:5.2.0'
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
androidTestImplementation 'androidx.test:runner:1.5.2'
androidTestImplementation 'androidx.test:rules:1.5.0'
```

---

### Task 18: Create README and API Documentation
**Status:** Not Started
**Priority:** High
**Estimated Effort:** 4-5 hours

**Description:**
Write comprehensive documentation for the library.

**Subtasks:**
- [ ] Create `README.md`
  - [ ] Library overview (connect wallet SDK for dApps)
  - [ ] Features list (connect to wallets, auth, signing, transactions)
  - [ ] Requirements (min SDK, dependencies)
  - [ ] Installation instructions
  - [ ] Quick start guide
  - [ ] Basic usage examples
  - [ ] Configuration options
  - [ ] Link to API reference
  - [ ] License information
- [ ] Create `API_REFERENCE.md`
  - [ ] ZetrixConnectWallet class documentation
  - [ ] All public methods with parameters
  - [ ] Callback interfaces
  - [ ] Configuration options
  - [ ] Error codes and handling
  - [ ] Code examples for each operation (auth, sign, transaction)
- [ ] Create `CHANGELOG.md`
  - [ ] Version 1.0.0 initial release notes
- [ ] Generate Javadoc
  - [ ] Configure Javadoc generation in Gradle
  - [ ] Ensure all public APIs documented
  - [ ] Generate HTML documentation
- [ ] Create `CONTRIBUTING.md` (optional)
  - [ ] Development setup
  - [ ] Code style guidelines
  - [ ] Pull request process
- [ ] Update `LICENSE` file
  - [ ] Choose license (Apache 2.0 recommended)
  - [ ] Add copyright notice

**Acceptance Criteria:**
- ✓ README is clear and comprehensive
- ✓ Installation instructions work
- ✓ Code examples compile and run
- ✓ API reference covers all public methods
- ✓ Javadoc generates without errors
- ✓ License file present
- ✓ CHANGELOG initialized

**Files Created:**
- `zetrix-connect-wallet-sdk-android/README.md`
- `zetrix-connect-wallet-sdk-android/API_REFERENCE.md`
- `zetrix-connect-wallet-sdk-android/CHANGELOG.md`
- `zetrix-connect-wallet-sdk-android/LICENSE`
- `zetrix-connect-wallet-sdk-android/CONTRIBUTING.md` (optional)

**Dependencies:**
- All implementation tasks
- Task 17 (ensure code works for examples)

**References:**
- Flutter: `zetrix_connect_wallet_sdk/README.md`
- Flutter: `zetrix_connect_wallet_sdk/API_REFERENCE.md`
- Flutter: `zetrix_connect_wallet_sdk/ARCHITECTURE.md`

---

## Milestone Checklist

### Milestone 1: Foundation Complete (Partial) 🚧
- [x] Task 1: Project structure set up ✅
- [x] Task 2: Package structure defined ✅
- [ ] Task 3: Constants ported (In Progress)
- [x] Project builds successfully ✅

### Milestone 2: Core Components Ready
- [ ] Task 4: Helper classes ported
- [ ] Task 5: Utility classes ported
- [ ] Basic functionality testable

### Milestone 3: Infrastructure Complete
- [ ] Task 6: Secure storage implemented
- [ ] Task 7: ChainSDK ported
- [ ] Task 8: WebSocket handler working
- [ ] Network communication functional

### Milestone 4: SDK Functional
- [ ] Task 9: Main SDK ported
- [ ] Task 10: QR code generation (optional)
- [ ] Task 11: Deep linking (optional)
- [ ] Task 12: Callbacks implemented
- [ ] Task 13: Dependencies configured
- [ ] Library fully functional

### Milestone 5: Example App Complete
- [ ] Task 14: Example app set up
- [ ] Task 15: Library integrated
- [ ] Task 16: UI implemented
- [ ] Demo app fully functional

### Milestone 6: Production Ready
- [ ] Task 17: Unit tests passing
- [ ] Task 18: Documentation complete
- [ ] Code coverage >80%
- [ ] Ready for release

---

## Estimated Timeline

**Total Effort:** 50-65 hours (1.5-2 weeks for one developer)

**Phase Breakdown:**
- Phase 1 (Setup): 3-4 hours
- Phase 2 (Core): 9-11 hours
- Phase 3 (Infrastructure): 11-14 hours
- Phase 4 (Main SDK): 16-21 hours
- Phase 5 (Example App): 9-12 hours
- Phase 6 (Testing & Docs): 10-13 hours

**Recommended Schedule:**
- **Week 1:** Phases 1-3 (foundation and infrastructure)
- **Week 2:** Phases 4-6 (SDK implementation, example app, polish)

---

## Success Criteria

The AAR library is considered complete when:

1. ✓ All 18 tasks are completed
2. ✓ Library builds without errors
3. ✓ Example app demonstrates all features
4. ✓ Unit tests pass with >80% coverage
5. ✓ Documentation is comprehensive
6. ✓ Can be published to Maven repository
7. ✓ Works on Android API 21-34+
8. ✓ No memory leaks or crashes
9. ✓ Performance is acceptable
10. ✓ Code follows Android best practices

---

## Notes

- **Optional Features:** Tasks 10 (QR) and 11 (deep linking) are marked as optional/medium priority
- **Testing:** Task 17 should run continuously as features are implemented
- **Documentation:** Task 18 should be updated incrementally
- **Version Control:** Commit after each completed task
- **Code Review:** Consider peer review before marking tasks complete

---

**Created:** 2024-12-09
**Last Updated:** 2024-12-09
**Version:** 1.1

---

## Changelog

### 2024-12-09 - v1.1
- ✅ Completed Phase 1 (Tasks 1-2)
- Module name: `zetrix-connect-wallet` (not `zetrix-connect-wallet-sdk`)
- Fixed namespace: `com.zetrix.connectwallet`
- Created all package subdirectories
- Project builds successfully with Gradle 8.13
- Ready to begin Phase 2: Core Components