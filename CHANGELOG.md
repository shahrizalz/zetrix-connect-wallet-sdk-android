# Changelog

All notable changes to the Zetrix Connect Wallet SDK for Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-12-12

### Added
- Initial release of Zetrix Connect Wallet SDK for Android
- Wallet connectivity for multiple Zetrix-compatible wallet apps (Zetrix Wallet, PIXA, MyID, MUMA)
- Dual API support: Callback-based and CompletableFuture-based APIs
- Builder pattern initialization with Application context support
- WebSocket connection with auto-reconnection capability
- User authentication with wallet address retrieval
- Message signing (text and binary/blob data)
- Blockchain transaction sending
- Verifiable Credential (VC) verification
- Verifiable Presentation (VP) requests
- QR code display with automatic lifecycle management
- Deep linking support as alternative to QR codes
- Secure session storage using EncryptedSharedPreferences (AES256-GCM)
- Android Keystore integration for hardware-backed security
- Centralized logging system with configurable log levels
- Comprehensive example app with both API approach demonstrations
- Min SDK 24 (Android 7.0) support
- Target SDK 36 (Android 14) support

### Security
- Encrypted storage for session data
- HMAC transaction integrity verification
- Hardware-backed keystore for master key storage
- Secure WebSocket communication (WSS)

### Documentation
- Comprehensive README with API reference
- Publishing guide for multiple distribution methods
- Code examples for both callback and CompletableFuture APIs
- Kotlin coroutines integration examples

---

## [Unreleased]

### Planned
- Additional wallet app integrations
- Enhanced error handling and reporting
- Biometric authentication support
- Multi-chain support
- Offline transaction signing

---

**Note:** For installation instructions and usage examples, see [README.md](README.md)
