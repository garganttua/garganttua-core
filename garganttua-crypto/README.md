# Garganttua Crypto

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

<!-- AUTO-GENERATED-END -->

## Core Concepts

### GGKeyRealm
A `GGKeyRealm` is the central abstraction for cryptographic operations. It encapsulates key generation, management, and provides access to keys for specific operations (encryption, decryption, signing, verification).

Key realms automatically generate and manage:
- **Symmetric keys** (AES, DES, Blowfish, etc.) - Same key for encryption and decryption
- **Asymmetric key pairs** (RSA, EC, DSA, DH) - Public/private key pairs

### IGGKey
The `IGGKey` interface provides methods for cryptographic operations:
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
import com.garganttua.keys.GGKeyRealm;
import com.garganttua.keys.GGKeyAlgorithm;
import com.garganttua.keys.GGSignatureAlgorithm;
import com.garganttua.keys.IGGKey;

// Create a key realm for RSA signatures
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_4096, null, GGSignatureAlgorithm.SHA224);

// Get keys for signing and verification
IGGKey signingKey = realm.getKeyForSigning();
IGGKey verifyingKey = realm.getKeyForSignatureVerification();

// Sign data
byte[] signature = signingKey.sign("Salut".getBytes());

// Verify signature
boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
// signatureOk will be true
```

#### ECDSA Signature with SHA256
```java
// Create a key realm for EC signatures
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.EC_256, null, GGSignatureAlgorithm.SHA256);

IGGKey signingKey = realm.getKeyForSigning();
IGGKey verifyingKey = realm.getKeyForSignatureVerification();

byte[] signature = signingKey.sign("Salut".getBytes());
boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
```

#### DSA Signature with SHA256
```java
// Create a key realm for DSA signatures
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DSA_2048, null, GGSignatureAlgorithm.SHA256);

IGGKey signingKey = realm.getKeyForSigning();
IGGKey verifyingKey = realm.getKeyForSignatureVerification();

byte[] signature = signingKey.sign("Salut".getBytes());
boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
```

### 2. Asymmetric Encryption (RSA)

RSA encryption allows secure data transmission using public/private key pairs.

```java
import com.garganttua.keys.GGKeyRealm;
import com.garganttua.keys.GGKeyAlgorithm;
import com.garganttua.keys.GGEncryptionMode;
import com.garganttua.keys.GGEncryptionPaddingMode;

// Create a key realm for RSA encryption
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.RSA_4096, null,
    GGEncryptionMode.ECB, GGEncryptionPaddingMode.PKCS1_PADDING);

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
// Create a key realm for AES encryption
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null,
    GGEncryptionMode.ECB, GGEncryptionPaddingMode.PKCS5_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with CBC Mode (Initialization Vector)
```java
// CBC mode requires an initialization vector (IV)
// Specify IV size as the fourth parameter
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 16,
    GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForEncryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with GCM Mode
```java
// GCM mode provides authenticated encryption
// GCM typically uses a 12-byte IV
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 12,
    GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with CTR Mode
```java
// CTR (Counter) mode
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 16,
    GGEncryptionMode.CTR, GGEncryptionPaddingMode.NO_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### AES-256 with CFB Mode
```java
// CFB (Cipher Feedback) mode
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.AES_256, null, 16,
    GGEncryptionMode.CFB, GGEncryptionPaddingMode.NO_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

### 4. Other Symmetric Algorithms

#### Triple DES (DESede)
```java
// Triple DES with 168-bit key
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DESEDE_168, 8,
    GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### DES
```java
// Standard DES with 56-bit key
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.DES_56, 8,
    GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);

byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);

String result = new String(decrypted);  // "salut"
```

#### Blowfish
```java
// Blowfish with 120-bit key
GGKeyRealm realm = new GGKeyRealm("toto", GGKeyAlgorithm.BLOWFISH_120, 8,
    GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);

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

GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256, expiration,
    GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);

// Use the realm...
// After expiration, getKeyForEncryption() will throw GGKeyException
```

#### Revoking a Key Realm
```java
GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.RSA_2048, null,
    GGSignatureAlgorithm.SHA256);

// Revoke the key realm
realm.revoke();

// After revocation, any attempt to use the keys will throw GGKeyException
try {
    IGGKey key = realm.getKeyForSigning();
} catch (GGKeyException e) {
    // "The key for realm myRealm is revoked"
}
```

#### Accessing Key Realm Properties
```java
GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.RSA_2048, null,
    GGSignatureAlgorithm.SHA256);

String name = realm.getName();                    // "myRealm"
GGKeyAlgorithm algorithm = realm.getKeyAlgorithm(); // RSA_2048
GGKeyRealmType type = realm.getType();            // ASYMETRIC
Date expiration = realm.getExpiration();          // null (no expiration)
boolean isRevoked = realm.isRevoked();            // false
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
2. **Exception Handling**: Always catch and handle `GGKeyException` properly
3. **Key Storage**: Store key realms securely - consider using a secure key store
4. **Data Size Limits**: RSA encryption is limited by key size (e.g., RSA-2048 can encrypt ~245 bytes)

### Performance
1. **Symmetric encryption (AES)** is much faster than asymmetric (RSA) for large data
2. **Use RSA for key exchange**, then use AES for data encryption (hybrid cryptography)
3. **Consider key size vs performance**: Larger keys are more secure but slower

## Exception Handling

All cryptographic operations can throw `GGKeyException`. Common scenarios include:
- Using an expired key realm
- Using a revoked key realm
- Invalid encryption/decryption operations
- Signature verification failures

```java
try {
    GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256,
        GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);

    IGGKey key = realm.getKeyForEncryption();
    byte[] encrypted = key.encrypt("sensitive data".getBytes());

} catch (GGKeyException e) {
    // Handle cryptographic errors
    System.err.println("Cryptographic error: " + e.getMessage());
}
```

## License
This module is distributed under the MIT License.