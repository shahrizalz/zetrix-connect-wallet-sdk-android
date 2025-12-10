package com.example.exampledapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exampledapp.ui.theme.ExampleDappTheme
import com.zetrix.connectwallet.ZetrixConnectWallet
import com.zetrix.connectwallet.callbacks.WebSocketCallback
import com.zetrix.connectwallet.utils.DeviceUtils
import com.zetrix.connectwallet.utils.StorageUtils
import com.zetrix.connectwallet.utils.ZetrixLogger
import org.json.JSONObject
import java.util.Arrays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var walletConnect: ZetrixConnectWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable verbose logging for debugging (shows all SDK logs including debug level)
        // Set to Level.INFO for production, Level.FINE for debugging
        ZetrixLogger.setLevel(ZetrixLogger.Level.FINE)

        // Initialize SDK with Application context
        // The SDK internally launches a QRCodeActivity when needed to display QR codes
        walletConnect = ZetrixConnectWallet.Builder(applicationContext)
            .setAppType("zetrix")
            .setTestnet(false)
            .setQrcode(true) // Set to false to use deep linking instead
            .setBridgeUrl("wss://test-wscw1.zetrix.com")
            .build()

        // Initialize storage
        walletConnect.initialize()

        setContent {
            ExampleDappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ZetrixSDKExampleScreen()
                }
            }
        }
    }

    @Composable
    fun ZetrixSDKExampleScreen() {
        var selectedTab by remember { mutableStateOf(0) }
        var deviceInfo by remember { mutableStateOf("Loading...") }
        var sessionId by remember { mutableStateOf("Not connected") }
        var address by remember { mutableStateOf("Unknown") }
        var lastResult by remember { mutableStateOf("No operation performed yet") }

        // Load device info on start
        LaunchedEffect(Unit) {
            deviceInfo = "${DeviceUtils.getDeviceModel()} (${DeviceUtils.getDevicePlatform()}) - " +
                    "App: ${DeviceUtils.getAppName(this@MainActivity)} (${DeviceUtils.getAppPackageId(this@MainActivity)})"

            // Load session if exists
            val storedSessionId = StorageUtils.getSessionId()
            val storedAddress = StorageUtils.getAddress()
            if (storedSessionId != null) {
                sessionId = storedSessionId
                address = storedAddress ?: "Unknown"
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(
                text = "Zetrix SDK Example",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Callback API") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("CompletableFuture API") }
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Device Info (shared by both tabs)
                Text("Device: $deviceInfo", modifier = Modifier.padding(bottom = 8.dp))
                Text("Session: $sessionId", modifier = Modifier.padding(bottom = 8.dp))
                Text("Address: $address", modifier = Modifier.padding(bottom = 24.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> CallbackExamplesContent(
                        sessionId = sessionId,
                        address = address,
                        onSessionUpdate = { newSessionId, newAddress ->
                            sessionId = newSessionId
                            address = newAddress
                        },
                        onResultUpdate = { result -> lastResult = result }
                    )
                    1 -> CompletableFutureExamplesContent(
                        sessionId = sessionId,
                        address = address,
                        onSessionUpdate = { newSessionId, newAddress ->
                            sessionId = newSessionId
                            address = newAddress
                        },
                        onResultUpdate = { result -> lastResult = result }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Last Result Section (shared by both tabs)
                SectionHeader("Last Result:")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE))
                ) {
                    Text(
                        text = lastResult,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    @Composable
    fun CallbackExamplesContent(
        sessionId: String,
        address: String,
        onSessionUpdate: (String, String) -> Unit,
        onResultUpdate: (String) -> Unit
    ) {
        Column {

            // Connection Section
            SectionHeader("Connection:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        walletConnect.connect(object : WebSocketCallback {
                            override fun onConnected(authInfo: JSONObject?) {
                                runOnUiThread {
                                    val storedSessionId = StorageUtils.getSessionId()
                                    val storedAddress = StorageUtils.getAddress()
                                    if (storedSessionId != null && storedAddress != null) {
                                        onSessionUpdate(storedSessionId, storedAddress)
                                        onResultUpdate("Connected (already authenticated)")
                                    } else {
                                        onSessionUpdate("Connected (ready for auth)", address)
                                        onResultUpdate("Connected successfully")
                                    }
                                }
                            }

                            override fun onMessage(message: JSONObject?) {
                                // Handle incoming WebSocket messages if needed
                            }

                            override fun onClosed(code: Int, reason: String?) {
                                runOnUiThread {
                                    onResultUpdate("Connection closed: $reason (code: $code)")
                                }
                            }

                            override fun onError(error: Exception) {
                                runOnUiThread {
                                    onResultUpdate("Connection error: ${error.message}")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Connect")
                }

                Button(
                    onClick = {
                        walletConnect.auth(object : ZetrixConnectWallet.AuthCallback {
                            override fun onSuccess(newAddress: String, newSessionId: String) {
                                runOnUiThread {
                                    onSessionUpdate(newSessionId, newAddress)
                                    onResultUpdate("Auth Success:\nSessionId: $newSessionId\nAddress: $newAddress")
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("Auth Failed: $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auth")
                }

                Button(
                    onClick = {
                        walletConnect.closeConnect()
                        walletConnect.disconnect()
                        onSessionUpdate("Not connected", "Unknown")
                        onResultUpdate("Disconnected")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    walletConnect.authAndSignMessage("Auth and sign in one step!", object : ZetrixConnectWallet.AuthAndSignCallback {
                        override fun onSuccess(newSessionId: String, newAddress: String, publicKey: String, signData: String) {
                            runOnUiThread {
                                onSessionUpdate(newSessionId, newAddress)
                                onResultUpdate("AuthAndSignMessage Success:\n" +
                                        "SessionId: $newSessionId\n" +
                                        "Address: $newAddress\n" +
                                        "PublicKey: $publicKey\n" +
                                        "SignData: $signData")
                            }
                        }

                        override fun onError(error: String) {
                            runOnUiThread {
                                onResultUpdate("AuthAndSignMessage Failed: $error")
                            }
                        }
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text("Auth & Sign Message")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signing Operations Section
            SectionHeader("Signing Operations:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        walletConnect.signMessage("Hello from Zetrix SDK Android!", object : ZetrixConnectWallet.SignCallback {
                            override fun onSuccess(signerAddress: String, publicKey: String, signData: String) {
                                runOnUiThread {
                                    onResultUpdate("SignMessage Success:\n" +
                                            "Address: $signerAddress\n" +
                                            "PublicKey: $publicKey\n" +
                                            "SignData: $signData")
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("SignMessage Failed: $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign Message")
                }

                Button(
                    onClick = {
                        walletConnect.signBlob("AAAAAAbcdef", object : ZetrixConnectWallet.SignCallback {
                            override fun onSuccess(signerAddress: String, publicKey: String, signData: String) {
                                runOnUiThread {
                                    onResultUpdate("SignBlob Success:\n" +
                                            "Address: $signerAddress\n" +
                                            "PublicKey: $publicKey\n" +
                                            "SignData: $signData")
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("SignBlob Failed: $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign Blob")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Operations Section
            SectionHeader("Transaction Operations:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("GetNonce Failed: Please authenticate first")
                            return@Button
                        }

                        walletConnect.getNonce(address, "1", object : ZetrixConnectWallet.NonceCallback {
                            override fun onSuccess(nonce: Long) {
                                runOnUiThread {
                                    onResultUpdate("GetNonce Success:\nNonce: $nonce")
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("GetNonce Failed: $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Get Nonce")
                }

                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("SendTransaction Failed: Please authenticate first")
                            return@Button
                        }

                        // First get nonce
                        walletConnect.getNonce(address, "1", object : ZetrixConnectWallet.NonceCallback {
                            override fun onSuccess(nonce: Long) {
                                // Then send transaction
                                walletConnect.sendTransaction(
                                    address,
                                    "ZTX3QkbTjJsc7xDbwRtuHn826cAYF79uKR3rt", // Example recipient
                                    "1", // 1 ZTX
                                    "0.0001",
                                    nonce,
                                    object : ZetrixConnectWallet.TransactionCallback {
                                        override fun onSuccess(transactionHash: String) {
                                            runOnUiThread {
                                                onResultUpdate("SendTransaction Success:\nHash: $transactionHash")
                                            }
                                        }

                                        override fun onError(error: String) {
                                            runOnUiThread {
                                                onResultUpdate("SendTransaction Failed: $error")
                                            }
                                        }
                                    }
                                )
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("SendTransaction Failed: Could not get nonce - $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Transaction")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verifiable Credentials Section
            SectionHeader("Verifiable Credentials:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("VerifyVC Failed: Please authenticate first")
                            return@Button
                        }

                        walletConnect.verifyVC("example-template-id-123", object : ZetrixConnectWallet.VCCallback {
                            override fun onSuccess(status: String, details: String) {
                                runOnUiThread {
                                    onResultUpdate("VerifyVC Success:\nStatus: $status\nDetails: $details")
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("VerifyVC Failed: $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify VC")
                }

                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("GetVP Failed: Please authenticate first")
                            return@Button
                        }

                        val attributes = Arrays.asList("name", "email", "phone")
                        walletConnect.getVP("example-template-id-456", attributes, object : ZetrixConnectWallet.VPCallback {
                            override fun onSuccess(uuid: String) {
                                runOnUiThread {
                                    onResultUpdate("GetVP Success:\nUUID: $uuid")
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    onResultUpdate("GetVP Failed: $error")
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Get VP")
                }
            }
        }
    }

    @Composable
    fun CompletableFutureExamplesContent(
        sessionId: String,
        address: String,
        onSessionUpdate: (String, String) -> Unit,
        onResultUpdate: (String) -> Unit
    ) {
        val scope = rememberCoroutineScope()

        Column {
            // Connection Section
            SectionHeader("Connection (Async):")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val authInfo = walletConnect.connectAsync().await()
                                withContext(Dispatchers.Main) {
                                    val storedSessionId = StorageUtils.getSessionId()
                                    val storedAddress = StorageUtils.getAddress()
                                    if (storedSessionId != null && storedAddress != null) {
                                        onSessionUpdate(storedSessionId, storedAddress)
                                        onResultUpdate("Connected (already authenticated)")
                                    } else {
                                        onSessionUpdate("Connected (ready for auth)", address)
                                        onResultUpdate("Connected successfully")
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("Connection error: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Connect")
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val result = walletConnect.authAsync().await()
                                withContext(Dispatchers.Main) {
                                    onSessionUpdate(result.sessionId, result.address)
                                    onResultUpdate("Auth Success:\nSessionId: ${result.sessionId}\nAddress: ${result.address}")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("Auth Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auth")
                }

                Button(
                    onClick = {
                        walletConnect.closeConnect()
                        walletConnect.disconnect()
                        onSessionUpdate("Not connected", "Unknown")
                        onResultUpdate("Disconnected")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val result = walletConnect.authAndSignMessageAsync("Auth and sign in one step!").await()
                            withContext(Dispatchers.Main) {
                                onSessionUpdate(result.sessionId, result.address)
                                onResultUpdate("AuthAndSignMessage Success:\n" +
                                        "SessionId: ${result.sessionId}\n" +
                                        "Address: ${result.address}\n" +
                                        "PublicKey: ${result.publicKey}\n" +
                                        "SignData: ${result.signData}")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                onResultUpdate("AuthAndSignMessage Failed: ${e.message}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text("Auth & Sign Message (Async)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signing Operations Section
            SectionHeader("Signing Operations (Async):")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val result = walletConnect.signMessageAsync("Hello from Zetrix SDK Android!").await()
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("SignMessage Success:\n" +
                                            "Address: ${result.address}\n" +
                                            "PublicKey: ${result.publicKey}\n" +
                                            "SignData: ${result.signData}")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("SignMessage Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign Message")
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val result = walletConnect.signBlobAsync("AAAAAAbcdef").await()
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("SignBlob Success:\n" +
                                            "Address: ${result.address}\n" +
                                            "PublicKey: ${result.publicKey}\n" +
                                            "SignData: ${result.signData}")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("SignBlob Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign Blob")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Operations Section
            SectionHeader("Transaction Operations (Async):")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("GetNonce Failed: Please authenticate first")
                            return@Button
                        }

                        scope.launch {
                            try {
                                val nonce = walletConnect.getNonceAsync(address, "1").await()
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("GetNonce Success:\nNonce: $nonce")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("GetNonce Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Get Nonce")
                }

                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("SendTransaction Failed: Please authenticate first")
                            return@Button
                        }

                        // Async/await makes chaining much cleaner!
                        scope.launch {
                            try {
                                // Get nonce
                                val nonce = walletConnect.getNonceAsync(address, "2").await()

                                // Send transaction
                                val hash = walletConnect.sendTransactionAsync(
                                    address,
                                    "ZTX3QkbTjJsc7xDbwRtuHn826cAYF79uKR3rt",
                                    "1",
                                    "0.0001",
                                    nonce
                                ).await()

                                withContext(Dispatchers.Main) {
                                    onResultUpdate("SendTransaction Success:\nHash: $hash")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("SendTransaction Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Transaction")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verifiable Credentials Section
            SectionHeader("Verifiable Credentials (Async):")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("VerifyVC Failed: Please authenticate first")
                            return@Button
                        }

                        scope.launch {
                            try {
                                val result = walletConnect.verifyVCAsync("example-template-id-123").await()
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("VerifyVC Success:\nStatus: ${result.status}\nDetails: ${result.details}")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("VerifyVC Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify VC")
                }

                Button(
                    onClick = {
                        if (address == "Unknown" || address.isEmpty()) {
                            onResultUpdate("GetVP Failed: Please authenticate first")
                            return@Button
                        }

                        scope.launch {
                            try {
                                val attributes = Arrays.asList("name", "email", "phone")
                                val uuid = walletConnect.getVPAsync("example-template-id-456", attributes).await()
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("GetVP Success:\nUUID: $uuid")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onResultUpdate("GetVP Failed: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Get VP")
                }
            }
        }
    }

    @Composable
    fun SectionHeader(text: String) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    @Composable
    fun InfoText(text: String) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}
