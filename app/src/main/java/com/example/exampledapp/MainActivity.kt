package com.example.exampledapp

import android.os.Bundle
import android.widget.Toast
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
import org.json.JSONObject
import java.util.Arrays

class MainActivity : ComponentActivity() {
    private lateinit var walletConnect: ZetrixConnectWallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SDK with Application context (recommended)
        // The SDK now works with Application context and internally launches
        // a QRCodeActivity when needed to display QR codes
        walletConnect = ZetrixConnectWallet.Builder(applicationContext)
            .setAppType("zetrix")
            .setTestnet(false)
            .setQrcode(true) // Set to false to use deep linking instead
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Zetrix SDK Example",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Device Info
            Text("Device: $deviceInfo", modifier = Modifier.padding(bottom = 8.dp))
            Text("Session: $sessionId", modifier = Modifier.padding(bottom = 8.dp))
            Text("Address: $address", modifier = Modifier.padding(bottom = 24.dp))

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
                                        sessionId = storedSessionId
                                        address = storedAddress
                                        lastResult = "Connected (already authenticated)"
                                    } else {
                                        sessionId = "Connected (ready for auth)"
                                        lastResult = "Connected successfully"
                                    }
                                }
                            }

                            override fun onMessage(message: JSONObject?) {
                                // Handle incoming WebSocket messages if needed
                                // For this example app, we don't need to handle raw messages
                                // as they're handled by the SDK internally
                            }

                            override fun onClosed(code: Int, reason: String?) {
                                runOnUiThread {
                                    lastResult = "Connection closed: $reason (code: $code)"
                                }
                            }

                            override fun onError(error: Exception) {
                                runOnUiThread {
                                    lastResult = "Connection error: ${error.message}"
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
                        walletConnect.auth(true, object : ZetrixConnectWallet.AuthCallback {
                            override fun onSuccess(newAddress: String, newSessionId: String) {
                                runOnUiThread {
                                    sessionId = newSessionId
                                    address = newAddress
                                    lastResult = "Auth Success:\nSessionId: $newSessionId\nAddress: $newAddress"
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "Auth Failed: $error"
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
                        sessionId = "Not connected"
                        address = "Unknown"
                        lastResult = "Disconnected"
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
                                sessionId = newSessionId
                                address = newAddress
                                lastResult = "AuthAndSignMessage Success:\n" +
                                        "SessionId: $newSessionId\n" +
                                        "Address: $newAddress\n" +
                                        "PublicKey: $publicKey\n" +
                                        "SignData: $signData"
                            }
                        }

                        override fun onError(error: String) {
                            runOnUiThread {
                                lastResult = "AuthAndSignMessage Failed: $error"
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
                                    lastResult = "SignMessage Success:\n" +
                                            "Address: $signerAddress\n" +
                                            "PublicKey: $publicKey\n" +
                                            "SignData: $signData"
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "SignMessage Failed: $error"
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
                                    lastResult = "SignBlob Success:\n" +
                                            "Address: $signerAddress\n" +
                                            "PublicKey: $publicKey\n" +
                                            "SignData: $signData"
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "SignBlob Failed: $error"
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
                            lastResult = "GetNonce Failed: Please authenticate first"
                            return@Button
                        }

                        walletConnect.getNonce(address, "1", object : ZetrixConnectWallet.NonceCallback {
                            override fun onSuccess(nonce: Long) {
                                runOnUiThread {
                                    lastResult = "GetNonce Success:\nNonce: $nonce"
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "GetNonce Failed: $error"
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
                            lastResult = "SendTransaction Failed: Please authenticate first"
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
                                                lastResult = "SendTransaction Success:\nHash: $transactionHash"
                                            }
                                        }

                                        override fun onError(error: String) {
                                            runOnUiThread {
                                                lastResult = "SendTransaction Failed: $error"
                                            }
                                        }
                                    }
                                )
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "SendTransaction Failed: Could not get nonce - $error"
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
                            lastResult = "VerifyVC Failed: Please authenticate first"
                            return@Button
                        }

                        walletConnect.verifyVC("example-template-id-123", object : ZetrixConnectWallet.VCCallback {
                            override fun onSuccess(status: String, details: String) {
                                runOnUiThread {
                                    lastResult = "VerifyVC Success:\nStatus: $status\nDetails: $details"
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "VerifyVC Failed: $error"
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
                            lastResult = "GetVP Failed: Please authenticate first"
                            return@Button
                        }

                        val attributes = Arrays.asList("name", "email", "phone")
                        walletConnect.getVP("example-template-id-456", attributes, object : ZetrixConnectWallet.VPCallback {
                            override fun onSuccess(uuid: String) {
                                runOnUiThread {
                                    lastResult = "GetVP Success:\nUUID: $uuid"
                                }
                            }

                            override fun onError(error: String) {
                                runOnUiThread {
                                    lastResult = "GetVP Failed: $error"
                                }
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Get VP")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Last Result Section
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

            Spacer(modifier = Modifier.height(24.dp))

            // SDK Features Info
            SectionHeader("SDK Features:")
            InfoText("✓ QR Code & Deep Linking support")
            InfoText("✓ WebSocket real-time communication")
            InfoText("✓ Secure encrypted storage")
            InfoText("✓ Multiple wallet apps support (Zetrix, PIXA, MyID, MUMA)")
            InfoText("✓ Complete transaction lifecycle")
            InfoText("✓ Verifiable Credentials (VC/VP)")
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
