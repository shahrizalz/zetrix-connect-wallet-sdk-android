# Zetrix Connect Wallet SDK for Android

Android SDK for connecting to Zetrix wallet applications (Zetrix Wallet, PIXA, MyID, MUMA) for authentication, message signing, and blockchain transactions.

## Features

- **Easy Integration**: Simple builder pattern with Application context support
- **Dual API**: Both callback-based AND CompletableFuture-based APIs
- **Wallet Connectivity**: Connect to multiple Zetrix-compatible wallet apps
- **Authentication**: Request user authentication and receive wallet addresses
- **Message Signing**: Sign messages and binary data (blob)
- **Transactions**: Send blockchain transactions through wallet apps
- **Verifiable Credentials**: Verify VCs and request VPs
- **Secure Storage**: Encrypted SharedPreferences for session management
- **Auto-Reconnection**: Automatic WebSocket reconnection on connection loss
- **QR Code Support**: Built-in QR code display with automatic lifecycle management
- **Deep Linking**: Alternative to QR codes for direct wallet app launching

## Requirements

- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36
- **Java Version**: 11+
- **Kotlin**: 1.9+ (for Kotlin projects)

## Installation

### Gradle (build.gradle.kts)

```kotlin
dependencies {
    implementation(project(":zetrix-connect-wallet"))

    // Optional: For Kotlin Coroutines + CompletableFuture integration
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
}
```

**Note:** The coroutines dependencies are only needed if you want to use Kotlin's `.await()` with CompletableFuture. The SDK itself works without these dependencies.

## Quick Start

### 1. Initialize SDK

```kotlin
// In your Activity
class MainActivity : ComponentActivity() {
    private lateinit var walletConnect: ZetrixConnectWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SDK
        walletConnect = ZetrixConnectWallet.Builder(applicationContext)
            .setAppType("zetrix")      // "zetrix", "pixa", "myid", "muma"
            .setTestnet(false)         // true for testnet, false for mainnet
            .setQrcode(true)          // true for QR code, false for deep linking
            .build()

        // Initialize storage
        walletConnect.initialize()
    }
}
```

### 2. Connect to WebSocket

```kotlin
walletConnect.connect(object : WebSocketCallback {
    override fun onConnected(authInfo: JSONObject?) {
        // Connection established
        println("Connected to wallet server")
    }

    override fun onMessage(message: JSONObject?) {
        // Handle incoming messages (optional)
    }

    override fun onClosed(code: Int, reason: String?) {
        println("Connection closed: $reason")
    }

    override fun onError(error: Exception) {
        println("Error: ${error.message}")
    }
})
```

### 3. Authenticate User

**Option A: Callback API (Traditional)**
```kotlin
// QR code or deep link is determined by Builder configuration
walletConnect.auth(object : ZetrixConnectWallet.AuthCallback {
    override fun onSuccess(address: String, sessionId: String) {
        println("Authenticated! Address: $address")
        // QR code activity automatically closes on success
    }

    override fun onError(error: String) {
        println("Auth failed: $error")
    }
})
```

**Option B: CompletableFuture API (Recommended)**
```kotlin
// Much cleaner with CompletableFuture!
walletConnect.authAsync()
    .thenAccept { result ->
        println("Authenticated! Address: ${result.address}")
    }
    .exceptionally { error ->
        println("Auth failed: ${error.message}")
        null
    }
```

**Option C: CompletableFuture with chaining (Java 8+)**
```java
// Chain multiple operations easily
walletConnect.authAsync()
    .thenCompose(auth -> walletConnect.signMessageAsync("Hello Zetrix!"))
    .thenAccept(signResult -> {
        System.out.println("Signature: " + signResult.signData);
    })
    .exceptionally(error -> {
        System.out.println("Error: " + error.getMessage());
        return null;
    });
```

**Option D: Kotlin Coroutines with CompletableFuture (Kotlin only)**
```kotlin
// Requires kotlinx-coroutines-jdk8 dependency
scope.launch {
    try {
        val authResult = walletConnect.authAsync().await()
        val signResult = walletConnect.signMessageAsync("Hello Zetrix!").await()
        println("Signature: ${signResult.signData}")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
```

### 4. Sign Messages

```kotlin
walletConnect.signMessage("Hello Zetrix!", object : ZetrixConnectWallet.SignCallback {
    override fun onSuccess(address: String, publicKey: String, signData: String) {
        println("Signature: $signData")
    }

    override fun onError(error: String) {
        println("Signing failed: $error")
    }
})
```

### 5. Send Transactions

**Callback API (Nested callbacks)**
```kotlin
// First, get account nonce
walletConnect.getNonce(fromAddress, "1", object : ZetrixConnectWallet.NonceCallback {
    override fun onSuccess(nonce: Long) {
        // Send transaction
        walletConnect.sendTransaction(
            from = fromAddress,
            to = toAddress,
            amount = "1000000",      // In smallest unit (stroops)
            gasFee = "100",
            nonce = nonce + 1,
            callback = object : ZetrixConnectWallet.TransactionCallback {
                override fun onSuccess(transactionHash: String) {
                    println("Transaction sent! Hash: $transactionHash")
                }

                override fun onError(error: String) {
                    println("Transaction failed: $error")
                }
            }
        )
    }

    override fun onError(error: String) {
        println("Failed to get nonce: $error")
    }
})
```

**CompletableFuture API (Clean chaining)**
```kotlin
// Much cleaner with CompletableFuture chaining!
walletConnect.getNonceAsync(fromAddress, "1")
    .thenCompose { nonce ->
        walletConnect.sendTransactionAsync(fromAddress, toAddress, "1000000", "100", nonce + 1)
    }
    .thenAccept { txHash ->
        println("Transaction sent! Hash: $txHash")
    }
    .exceptionally { error ->
        println("Failed: ${error.message}")
        null
    }
```

## Callback API vs CompletableFuture API

The SDK provides **two ways** to handle asynchronous operations:

### Callback API (Traditional)

**When to use:**
- ✅ Java projects (Java 7 compatible)
- ✅ Simple, single operations
- ✅ No dependencies on external libraries

**Example:**
```java
walletConnect.auth(new ZetrixConnectWallet.AuthCallback() {
    @Override
    public void onSuccess(String address, String sessionId) {
        System.out.println("Success: " + address);
    }

    @Override
    public void onError(String error) {
        System.out.println("Error: " + error);
    }
});
```

**Pros:** Simple, familiar, works everywhere
**Cons:** Callback hell, harder to compose, error handling scattered

### CompletableFuture API (Modern - **Recommended**)

**When to use:**
- ✅ Java 8+ or Kotlin projects
- ✅ Complex flows with multiple operations
- ✅ Need to compose/chain operations
- ✅ Better error handling

**Example:**
```kotlin
// Clean sequential flow
walletConnect.connectAsync()
    .thenCompose { walletConnect.authAsync() }
    .thenCompose { walletConnect.signMessageAsync("Hello") }
    .thenAccept { result -> println("Done: ${result.signData}") }
    .exceptionally { error ->
        println("Error: ${error.message}")
        null
    }
```

**Pros:** Composable, clean syntax, single error handler, no callback hell
**Cons:** Requires Java 8+ (Android API 24+ or desugaring)

### Comparison Example

**Scenario:** Connect → Authenticate → Sign Message

<table>
<tr>
<th>Callback API</th>
<th>CompletableFuture API</th>
</tr>
<tr>
<td>

```kotlin
walletConnect.connect(object : WebSocketCallback {
    override fun onConnected(authInfo: JSONObject?) {
        walletConnect.auth(object : AuthCallback {
            override fun onSuccess(addr: String, sid: String) {
                walletConnect.signMessage("Hi", object : SignCallback {
                    override fun onSuccess(a: String, pk: String, sig: String) {
                        println("Done: $sig")
                    }
                    override fun onError(error: String) {
                        println("Sign error: $error")
                    }
                })
            }
            override fun onError(error: String) {
                println("Auth error: $error")
            }
        })
    }
    override fun onError(error: Exception) {
        println("Connect error: ${error.message}")
    }
    // ... other callbacks
})
```

</td>
<td>

```kotlin
walletConnect.connectAsync()
    .thenCompose { walletConnect.authAsync() }
    .thenCompose { walletConnect.signMessageAsync("Hi") }
    .thenAccept { result ->
        println("Done: ${result.signData}")
    }
    .exceptionally { error ->
        println("Error: ${error.message}")
        null
    }
```

</td>
</tr>
</table>

**Winner:** CompletableFuture API for cleaner, more maintainable code! 🏆

## Key Features

### CompletableFuture API Support

The SDK provides both callback-based and CompletableFuture-based APIs for all asynchronous operations.

**Benefits:**
- ✅ Compose multiple operations easily
- ✅ Single error handling point
- ✅ Better readability
- ✅ No external dependencies (Java 8+ built-in)
- ✅ Works seamlessly with Kotlin coroutines

### Automatic QR Code Management

The SDK includes an internal `QRCodeActivity` for displaying QR codes. Developers don't need to manage this activity - it's handled automatically.

**How it works:**
1. SDK launches `QRCodeActivity` when QR code is needed
2. Activity displays QR code with instructions
3. When authentication succeeds, SDK broadcasts close signal
4. Activity receives broadcast and closes automatically

**No developer intervention needed!**

## API Reference

### Builder Options

```kotlin
ZetrixConnectWallet.Builder(context)
    .setAppType(appType)      // "zetrix", "pixa", "myid", "muma"
    .setTestnet(testnet)      // true/false
    .setQrcode(isQrcode)      // true for QR, false for deep link
    .setBridgeUrl(url)        // Optional custom WebSocket URL
    .build()
```

### Main Methods

| Callback API | CompletableFuture API | Description |
|--------------|----------------------|-------------|
| `initialize()` | - | Initialize SDK (must be called before use) |
| `connect(callback)` | `connectAsync()` | Connect to WebSocket server |
| `auth(callback)` | `authAsync()` | Authenticate user (uses Builder config for QR/deep link) |
| `authAndSignMessage(msg, callback)` | `authAndSignMessageAsync(msg)` | Authenticate and sign in one step |
| `signMessage(msg, callback)` | `signMessageAsync(msg)` | Sign a text message |
| `signBlob(data, callback)` | `signBlobAsync(data)` | Sign binary data |
| `sendTransaction(...)` | `sendTransactionAsync(...)` | Send blockchain transaction |
| `getNonce(addr, chainId, callback)` | `getNonceAsync(addr, chainId)` | Get account nonce |
| `verifyVC(templateId, callback)` | `verifyVCAsync(templateId)` | Verify verifiable credential |
| `getVP(templateId, attrs, callback)` | `getVPAsync(templateId, attrs)` | Get verifiable presentation |
| `disconnect()` | - | Disconnect and clear session |
| `closeConnect()` | - | Close WebSocket (keep session) |
| `isConnected()` | - | Check connection status |
| `getSessionId()` | - | Get current session ID |
| `getAddress()` | - | Get authenticated address |

### Result Classes (for CompletableFuture API)

| Class | Fields | Usage |
|-------|--------|-------|
| `AuthResult` | `address`, `sessionId` | Returned by `authAsync()` |
| `SignResult` | `address`, `publicKey`, `signData` | Returned by `signMessageAsync()`, `signBlobAsync()` |
| `AuthAndSignResult` | `sessionId`, `address`, `publicKey`, `signData` | Returned by `authAndSignMessageAsync()` |
| `VCResult` | `status`, `details` | Returned by `verifyVCAsync()` |

### Callbacks

#### AuthCallback
```kotlin
interface AuthCallback {
    fun onSuccess(address: String, sessionId: String)
    fun onError(error: String)
}
```

#### SignCallback
```kotlin
interface SignCallback {
    fun onSuccess(address: String, publicKey: String, signData: String)
    fun onError(error: String)
}
```

#### TransactionCallback
```kotlin
interface TransactionCallback {
    fun onSuccess(transactionHash: String)
    fun onError(error: String)
}
```

## WebSocket URLs

- **Mainnet**: `wss://wscw.zetrix.com/api/websocket/server`
- **Testnet**: `wss://test-wscw.zetrix.com/api/websocket/server`

## Supported Wallet Apps

| App Type | App Name | Package ID |
|----------|----------|------------|
| `zetrix` | Zetrix Wallet | `com.zetrix.wallet` |
| `pixa` | PIXA | `com.pixa.wallet` |
| `myid` | MyID | `com.myid.wallet` |
| `muma` | MUMA | `com.muma.wallet` |

## Example App

See the `app` module for a complete example implementation with Jetpack Compose UI demonstrating all SDK features.

**The example app includes TWO tabs to demonstrate both API approaches:**

### Tab 1: Callback API Examples
Traditional callback-based approach showing:
- WebSocket connection with callbacks
- QR code authentication
- Auth & Sign Message (combined operation)
- Message signing
- Blob signing
- Transaction sending (with nested callbacks for nonce + send)
- VC verification
- VP requests

### Tab 2: CompletableFuture API Examples
Modern async/await approach showing:
- WebSocket connection with `connectAsync()`
- QR code authentication with `authAsync()`
- Auth & Sign Message with `authAndSignMessageAsync()`
- Message signing with `signMessageAsync()`
- Blob signing with `signBlobAsync()`
- **Transaction sending with clean chaining** (demonstrates the power of CompletableFuture - no nested callbacks!)
- VC verification with `verifyVCAsync()`
- VP requests with `getVPAsync()`

**With Kotlin Coroutines Integration:**

The example app demonstrates how to use the CompletableFuture API with Kotlin coroutines for even cleaner code:

```kotlin
scope.launch {
    try {
        // Get nonce and send transaction in a clean sequential flow
        val nonce = walletConnect.getNonceAsync(address, "1").await()
        val hash = walletConnect.sendTransactionAsync(from, to, amount, gasFee, nonce).await()
        println("Transaction sent! Hash: $hash")
    } catch (e: Exception) {
        println("Failed: ${e.message}")
    }
}
```

**Compare the approaches side-by-side** to see which style you prefer for your project!

## Architecture

### Components

```
┌─────────────────────────────────────┐
│     ZetrixConnectWallet (Main)      │
├─────────────────────────────────────┤
│  - Builder pattern initialization   │
│  - Auto QR lifecycle management     │
│  - Dual API (Callback + Future)     │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
┌───▼────────┐    ┌──────▼──────┐
│ WalletSocket│    │QRCodeActivity│
├────────────┤    ├─────────────┤
│ WebSocket  │    │ Internal UI │
│ Auto-reconnect│ │ Broadcast   │
│ Promise pool│   │ Auto-close  │
└────────────┘    └─────────────┘
```

### Security

- **Encrypted Storage**: Uses `EncryptedSharedPreferences` with AES256-GCM
- **Android Keystore**: Master key stored in hardware-backed keystore
- **HMAC**: Transaction integrity verification
- **Session Management**: Secure session ID and address storage

## Logging Configuration

The SDK uses a centralized logging system with configurable log levels.

### Enable Debug Logging

By default, the SDK logs at `INFO` level. To see all debug logs during development:

```kotlin
// In your MainActivity or Application class
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Enable verbose logging (shows all SDK logs including debug level)
    ZetrixLogger.setLevel(ZetrixLogger.Level.FINE)

    // Initialize SDK...
}
```

### Log Levels

| Level | Description | Use Case |
|-------|-------------|----------|
| `FINE` | Debug/verbose logs | Development & debugging |
| `INFO` | Informational logs | Production (default) |
| `WARNING` | Warning logs | Production with minimal logging |
| `SEVERE` | Error logs only | Production with error-only logging |
| `OFF` | No logs | Disable all logging |

### View Logs in Android Studio

The SDK uses the tag prefix `Zetrix:`. To filter SDK logs in Logcat:

1. Open Android Studio Logcat
2. Filter by tag: `Zetrix:`
3. Or filter by package: `com.zetrix.connectwallet`

**Example log tags:**
- `Zetrix:ZetrixConnectWallet`
- `Zetrix:WalletSocket`
- `Zetrix:QRCodeActivity`
- `Zetrix:DeepLinkHelper`

### Disable Logging

```kotlin
// Disable all SDK logging
ZetrixLogger.setLevel(ZetrixLogger.Level.OFF)
```

## Troubleshooting

### QR Code Not Showing

**Solution:** Ensure you've configured QR code in the Builder and SDK is connected:

```kotlin
// Configure QR code in Builder
val walletConnect = ZetrixConnectWallet.Builder(applicationContext)
    .setQrcode(true)  // Enable QR code
    .build()

// Then call auth
if (walletConnect.isConnected()) {
    walletConnect.auth(callback)
} else {
    // Connect first
    walletConnect.connect(connectCallback)
}
```

### QR Code Not Closing After Scan

**This issue is FIXED in the current version!** The QR code now automatically closes when authentication succeeds.

If you still experience issues:
1. Ensure you're using the latest version
2. Check that WebSocket is receiving the authentication response
3. Verify no custom broadcast receivers are blocking `com.zetrix.connectwallet.CLOSE_QR`

### Wallet App Not Found

**Solution:** SDK automatically redirects to Play Store if wallet app is not installed. Ensure device has Play Store access.

## License

[Your License Here]

## Support

For issues, questions, or contributions, please contact the Zetrix SDK team.

---

Built with ❤️ for the Zetrix ecosystem
