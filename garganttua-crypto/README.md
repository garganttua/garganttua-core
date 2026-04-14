# 🛠️ 🔐 Garganttua Crypto

## ⚠️ DISCLAIMER

This module is not yet finished and is not at a high priority at this time. So it will obviously be fully updated.

**⚠️Please do not use this module !!!⚠️**

## Description

The Garganttua Crypto module provides a comprehensive cryptographic framework for secure key management, encryption, decryption, and digital signatures. It supports both symmetric and asymmetric cryptography with a wide range of algorithms, modes, and padding schemes.

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-crypto</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `com.garganttua.core:garganttua-commons`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### KeyRealmBuilder
A `KeyRealmBuilder` creates `IKeyRealm` instances using a fluent builder pattern. It encapsulates key generation, management, and provides access to keys for specific operations (encryption, decryption, signing, verification).

Key realms automatically generate and manage:
- **Symmetric keys** (AES, DES, Blowfish, etc.) - Same key for encryption and decryption
- **Asymmetric key pairs** (RSA, EC, DSA, DH) - Public/private key pairs

### IKey
The `IKey` interface provides methods for cryptographic operations:
- `encrypt(byte[] clear)` - Encrypt data
- `decrypt(byte[] encoded)` - Decrypt data
- `sign(byte[] data)` - Create a digital signature
- `verifySignature(byte[] signature, byte[] originalData)` - Verify a signature

### Supported Algorithms

#### Asymmetric Algorithms
- **RSA**: 512, 1024, 2048, 3072, 4096 bits
- **DSA**: 512, 1024, 2048 bits
- **EC** (Elliptic Curve): 256, 384, 512 bits
- **DH** (Diffie-Hellman): 512-8192 bits

#### Symmetric Algorithms
- **AES**: 128, 192, 256 bits
- **DES**: 56 bits
- **DESede** (Triple DES): 112, 168 bits
- **Blowfish**: 32-448 bits
- **HMAC**: SHA1, SHA224, SHA256, SHA384, SHA512 variants

### Encryption Modes
- `ECB` - Electronic Codebook
- `CBC` - Cipher Block Chaining
- `CFB` - Cipher Feedback
- `CTR` - Counter
- `GCM` - Galois/Counter Mode
- `NONE` - No encryption mode

### Padding Modes
- `PKCS1_PADDING` - For RSA
- `PKCS5_PADDING` - For block ciphers
- `NO_PADDING` - No padding
- `NONE` - No padding mode

### Signature Algorithms
- `SHA1`, `SHA224`, `SHA256`, `SHA384`, `SHA512`
- `MD2`, `MD5`
- Various others (SHA3, BLAKE2, Keccak, etc.)

## Usage

### 1. Digital Signatures

Digital signatures provide authentication and non-repudiation. The following examples show real working code from the test suite.

#### RSA Signature with SHA224
```java
import com.garganttua.core.crypto.*;

// Create a key realm for RSA signatures
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.RSA_4096)
    .signatureAlgorithm(SignatureAlgorithm.SHA224)
    .build();

// Get keys for signing and verification
IKey signingKey = realm.getKeyForSigning();
IKey verifyingKey = realm.getKeyForSignatureVerification();

// Sign data
byte[] signature = signingKey.sign("Salut".getBytes());

// Verify signature
boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
// signatureOk will be true
```

#### ECDSA Signature with SHA256
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.EC_256)
    .signatureAlgorithm(SignatureAlgorithm.SHA256)
    .build();

IKey signingKey = realm.getKeyForSigning();
IKey verifyingKey = realm.getKeyForSignatureVerification();

byte[] signature = signingKey.sign("Salut".getBytes());
boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
```

#### DSA Signature with SHA256
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.DSA_2048)
    .signatureAlgorithm(SignatureAlgorithm.SHA256)
    .build();

IKey signingKey = realm.getKeyForSigning();
IKey verifyingKey = realm.getKeyForSignatureVerification();

byte[] signature = signingKey.sign("Salut".getBytes());
boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
```

### 2. Asymmetric Encryption (RSA)

RSA encryption allows secure data transmission using public/private key pairs.

```java
import com.garganttua.core.crypto.*;

// Create a key realm for RSA encryption
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.RSA_4096)
    .encryptionMode(EncryptionMode.ECB)
    .paddingMode(EncryptionPaddingMode.PKCS1_PADDING)
    .build();

// Encrypt with private key
byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());

// Encrypt with public key
byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());

// Decrypt with private key
byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);

// Decrypt with public key
byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);

// Both will return "salut"
String result1 = new String(decryptWithPrivate);  // "salut"
String result2 = new String(decryptWithPublic);   // "salut"
```

### 3. Symmetric Encryption (AES)

Symmetric encryption uses the same key for encryption and decryption, providing fast and efficient encryption.

#### AES-256 with ECB Mode
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.AES_256)
    .encryptionMode(EncryptionMode.ECB)
    .paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with CBC Mode (Initialization Vector)
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.AES_256)
    .initializationVectorSize(16)
    .encryptionMode(EncryptionMode.CBC)
    .paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForEncryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with GCM Mode
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.AES_256)
    .initializationVectorSize(12)
    .encryptionMode(EncryptionMode.GCM)
    .paddingMode(EncryptionPaddingMode.NO_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with CTR Mode
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.AES_256)
    .initializationVectorSize(16)
    .encryptionMode(EncryptionMode.CTR)
    .paddingMode(EncryptionPaddingMode.NO_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

### 4. Other Symmetric Algorithms

#### Triple DES (DESede)
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.DESEDE_168)
    .initializationVectorSize(8)
    .encryptionMode(EncryptionMode.CBC)
    .paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### DES
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.DES_56)
    .initializationVectorSize(8)
    .encryptionMode(EncryptionMode.CBC)
    .paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### Blowfish
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("toto")
    .algorithm(KeyAlgorithm.BLOWFISH_120)
    .initializationVectorSize(8)
    .encryptionMode(EncryptionMode.CBC)
    .paddingMode(EncryptionPaddingMode.PKCS5_PADDING)
    .build();

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

### 5. Key Realm Management

#### Creating a Key Realm with Expiration
```java
import java.util.Date;
import java.util.Calendar;

Calendar cal = Calendar.getInstance();
cal.add(Calendar.HOUR, 24);  // Key expires in 24 hours
Date expiration = cal.getTime();

IKeyRealm realm = KeyRealmBuilder.builder()
    .name("myRealm")
    .algorithm(KeyAlgorithm.AES_256)
    .expiration(expiration)
    .encryptionMode(EncryptionMode.GCM)
    .paddingMode(EncryptionPaddingMode.NO_PADDING)
    .build();

// Use the realm...
// After expiration, getKeyForEncryption() will throw CryptoException
```

#### Revoking a Key Realm
```java
IKeyRealm realm = KeyRealmBuilder.builder()
    .name("myRealm")
    .algorithm(KeyAlgorithm.RSA_2048)
    .signatureAlgorithm(SignatureAlgorithm.SHA256)
    .build();

// Revoke the key realm
realm.revoke();

// After revocation, any attempt to use the keys will throw CryptoException
try {
    IKey key = realm.getKeyForSigning();
} catch (CryptoException e) {
    // "The key for realm myRealm is revoked"
}
```

## Tips and Best Practices

### Choosing Algorithms
1. **For new applications**: Use AES-256 (symmetric) or RSA-2048/RSA-4096 (asymmetric)
2. **For digital signatures**: Use RSA with SHA256 or higher, or ECDSA with SHA256
3. **For authenticated encryption**: Use AES-GCM mode
4. **Avoid deprecated algorithms**: RC4, RC2, MD5 (except for compatibility)

### Encryption Modes
1. **Never use ECB mode** in production - it's not semantically secure
2. **Prefer GCM mode** for AES - it provides both encryption and authentication
3. **Use CBC or CTR** with proper IV management if GCM is not available
4. **Always use a unique IV** for each encryption operation in CBC, CTR, GCM modes

### Key Management
1. **Use appropriate key sizes**:
   - AES: 256 bits minimum
   - RSA: 2048 bits minimum (4096 for long-term security)
   - EC: 256 bits minimum
2. **Set expiration dates** for keys that need rotation
3. **Revoke compromised keys** immediately using `realm.revoke()`
4. **Use different key realms** for different purposes (signing vs encryption)

### Security Considerations
1. **Initialization Vectors (IV)**:
   - The framework automatically generates secure random IVs
   - IV sizes: 16 bytes for CBC/CTR/CFB, 12 bytes for GCM, 8 bytes for DES/Blowfish
2. **Exception Handling**: Always catch and handle `CryptoException` properly
3. **Key Storage**: Store key realms securely - consider using a secure key store
4. **Data Size Limits**: RSA encryption is limited by key size (e.g., RSA-2048 can encrypt ~245 bytes)

### Performance
1. **Symmetric encryption (AES)** is much faster than asymmetric (RSA) for large data
2. **Use RSA for key exchange**, then use AES for data encryption (hybrid cryptography)
3. **Consider key size vs performance**: Larger keys are more secure but slower

## Exception Handling

All cryptographic operations can throw `CryptoException`. Common scenarios include:
- Using an expired key realm
- Using a revoked key realm
- Invalid encryption/decryption operations
- Signature verification failures

```java
try {
    IKeyRealm realm = KeyRealmBuilder.builder()
        .name("myRealm")
        .algorithm(KeyAlgorithm.AES_256)
        .encryptionMode(EncryptionMode.GCM)
        .paddingMode(EncryptionPaddingMode.NO_PADDING)
        .build();

    IKey key = realm.getKeyForEncryption();
    byte[] encrypted = key.encrypt("sensitive data".getBytes());

} catch (CryptoException e) {
    // Handle cryptographic errors
    System.err.println("Cryptographic error: " + e.getMessage());
}
```

## License
This module is distributed under the MIT License.
