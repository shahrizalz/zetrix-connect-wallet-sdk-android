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
}
```

## Quick Start

### 1. Initialize SDK

**Recommended: Use Application Context**

The SDK now supports Application context, which provides better lifecycle management and eliminates tight coupling to Activity context.

```kotlin
// In your Activity
class MainActivity : ComponentActivity() {
    private lateinit var walletConnect: ZetrixConnectWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize with Application context (recommended)
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

**Also works from anywhere:**

```kotlin
// In a Service
class MyService : Service() {
    private lateinit var walletConnect: ZetrixConnectWallet

    override fun onCreate() {
        super.onCreate()
        walletConnect = ZetrixConnectWallet.Builder(applicationContext)
            .setAppType("zetrix")
            .build()
        walletConnect.initialize()
    }
}

// In a BroadcastReceiver
class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val walletConnect = ZetrixConnectWallet.Builder(context.applicationContext)
            .setAppType("zetrix")
            .build()
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
// With QR Code (shows QR automatically)
walletConnect.auth(true, object : ZetrixConnectWallet.AuthCallback {
    override fun onSuccess(address: String, sessionId: String) {
        println("Authenticated! Address: $address")
        // QR code dialog automatically closes on success
    }

    override fun onError(error: String) {
        println("Auth failed: $error")
    }
})
```

**Option B: CompletableFuture API (Recommended)**
```kotlin
// Much cleaner with CompletableFuture!
walletConnect.authAsync(true)
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
walletConnect.authAsync(true)
    .thenCompose(auth -> walletConnect.signMessageAsync("Hello Zetrix!"))
    .thenAccept(signResult -> {
        System.out.println("Signature: " + signResult.signData);
    })
    .exceptionally(error -> {
        System.out.println("Error: " + error.getMessage());
        return null;
    });
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
walletConnect.auth(true, new ZetrixConnectWallet.AuthCallback() {
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
    .thenCompose { walletConnect.authAsync(true) }
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
        walletConnect.auth(true, object : AuthCallback {
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
    .thenCompose { walletConnect.authAsync(true) }
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

## Key Improvements in This Version

### 1. CompletableFuture API Support (NEW!)

**Problem:** Callback hell makes complex flows hard to read and maintain

**Solution:** Added CompletableFuture-based async methods for all operations

**Benefits:**
- ✅ Compose multiple operations easily
- ✅ Single error handling point
- ✅ Better readability
- ✅ No external dependencies (Java 8+ built-in)
- ✅ Works seamlessly with Kotlin

### 2. Application Context Support

**Before:** SDK required Activity context, causing tight coupling

```kotlin
// Old way - required Activity context
val walletConnect = ZetrixConnectWallet.Builder(this) // ❌ Activity context
    .build()
```

**Now:** SDK accepts Application context, works anywhere

```kotlin
// New way - accepts Application context
val walletConnect = ZetrixConnectWallet.Builder(applicationContext) // ✅ Application context
    .build()
```

**Benefits:**
- ✅ No tight coupling to Activity lifecycle
- ✅ Can be initialized from Service, BroadcastReceiver, or anywhere
- ✅ No memory leaks from Activity references
- ✅ Survives configuration changes (rotation, etc.)

### 2. Fixed QR Code Dialog Auto-Close Issue

**Problem:** QR code dialog sometimes didn't close after successful WebSocket authentication

**Root Cause:** Dialog was created on background thread and returned `null` reference

**Solution:** SDK now uses a dedicated `QRCodeActivity` that listens for broadcast messages to close itself

**Result:**
- ✅ QR code always closes automatically on successful authentication
- ✅ Works reliably across all scenarios
- ✅ Better separation of concerns

### 3. Internal QRCodeActivity

The SDK now has its own internal `QRCodeActivity` for displaying QR codes. Developers don't need to manage this activity - it's handled automatically by the SDK.

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
| `auth(qrCode, callback)` | `authAsync(qrCode)` | Authenticate user (with QR or deep link) |
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

See the `app` module for a complete example implementation with Jetpack Compose UI demonstrating all SDK features:

- WebSocket connection
- QR code authentication
- Deep link authentication
- Message signing
- Transaction sending
- VC verification
- VP requests

## Migration Guide

### From Old Version (Activity Context) to New Version (Application Context)

**Step 1:** Update initialization

```kotlin
// Before
val walletConnect = ZetrixConnectWallet.Builder(this)
    .setAppType("zetrix")
    .build()

// After
val walletConnect = ZetrixConnectWallet.Builder(applicationContext)
    .setAppType("zetrix")
    .build()
```

**Step 2:** That's it! All other code remains the same.

The SDK now automatically:
- Uses Application context internally
- Launches QRCodeActivity when needed
- Closes QR codes on successful authentication
- Works from any Android component (Activity, Service, etc.)

## Architecture

### Components

```
┌─────────────────────────────────────┐
│     ZetrixConnectWallet (Main)      │
├─────────────────────────────────────┤
│  - Builder pattern initialization   │
│  - Context: Application (flexible)  │
│  - Auto QR lifecycle management     │
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

## Troubleshooting

### QR Code Not Showing

**Solution:** Ensure you're calling `auth(true, callback)` and SDK is connected:

```kotlin
if (walletConnect.isConnected()) {
    walletConnect.auth(true, callback)
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
