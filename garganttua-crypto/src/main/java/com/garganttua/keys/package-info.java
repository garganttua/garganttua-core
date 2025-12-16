/**
 * Garganttua Cryptography Framework
 *
 * <p>This package provides a comprehensive cryptographic framework for secure key management,
 * encryption, decryption, and digital signatures. It supports both symmetric and asymmetric
 * cryptography with a wide range of algorithms, modes, and padding schemes.</p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>GGKeyRealm</h3>
 * <p>The central abstraction for cryptographic operations. A key realm encapsulates key generation,
 * management, and provides access to keys for specific operations (encryption, decryption, signing,
 * verification).</p>
 *
 * <h3>IGGKey</h3>
 * <p>Interface for cryptographic key operations including encryption, decryption, signing, and
 * signature verification.</p>
 *
 * <h2>Quick Start Examples</h2>
 *
 * <h3>Digital Signatures with RSA</h3>
 * <pre>{@code
 * // Create a key realm for RSA signatures
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.RSA_4096, null, GGSignatureAlgorithm.SHA224);
 *
 * // Get keys for signing and verification
 * IGGKey signingKey = realm.getKeyForSigning();
 * IGGKey verifyingKey = realm.getKeyForSignatureVerification();
 *
 * // Sign data
 * byte[] signature = signingKey.sign("Salut".getBytes());
 *
 * // Verify signature
 * boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
 * }</pre>
 *
 * <h3>Digital Signatures with ECDSA</h3>
 * <pre>{@code
 * // Create a key realm for EC signatures
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.EC_256, null, GGSignatureAlgorithm.SHA256);
 *
 * IGGKey signingKey = realm.getKeyForSigning();
 * IGGKey verifyingKey = realm.getKeyForSignatureVerification();
 *
 * byte[] signature = signingKey.sign("Salut".getBytes());
 * boolean signatureOk = verifyingKey.verifySignature(signature, "Salut".getBytes());
 * }</pre>
 *
 * <h3>Asymmetric Encryption (RSA)</h3>
 * <pre>{@code
 * // Create a key realm for RSA encryption
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.RSA_4096, null,
 *     GGEncryptionMode.ECB, GGEncryptionPaddingMode.PKCS1_PADDING);
 *
 * // Encrypt with private key
 * byte[] encryptWithPrivate = realm.getKeyForEncryption().encrypt("salut".getBytes());
 *
 * // Encrypt with public key
 * byte[] encryptWithPublic = realm.getKeyForDecryption().encrypt("salut".getBytes());
 *
 * // Decrypt with private key
 * byte[] decryptWithPrivate = realm.getKeyForEncryption().decrypt(encryptWithPublic);
 *
 * // Decrypt with public key
 * byte[] decryptWithPublic = realm.getKeyForDecryption().decrypt(encryptWithPrivate);
 * }</pre>
 *
 * <h3>Symmetric Encryption (AES-256 with ECB)</h3>
 * <pre>{@code
 * // Create a key realm for AES encryption
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256, null,
 *     GGEncryptionMode.ECB, GGEncryptionPaddingMode.PKCS5_PADDING);
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 *
 * String result = new String(decrypted);  // "salut"
 * }</pre>
 *
 * <h3>AES-256 with CBC Mode (with Initialization Vector)</h3>
 * <pre>{@code
 * // CBC mode requires an initialization vector (IV)
 * // Specify IV size as the fourth parameter (16 bytes for AES)
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256, null, 16,
 *     GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForEncryption().decrypt(encrypted);
 * }</pre>
 *
 * <h3>AES-256 with GCM Mode (Authenticated Encryption)</h3>
 * <pre>{@code
 * // GCM mode provides authenticated encryption
 * // GCM typically uses a 12-byte IV
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256, null, 12,
 *     GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * <h3>AES-256 with CTR Mode</h3>
 * <pre>{@code
 * // CTR (Counter) mode
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256, null, 16,
 *     GGEncryptionMode.CTR, GGEncryptionPaddingMode.NO_PADDING);
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * <h3>Triple DES (DESede)</h3>
 * <pre>{@code
 * // Triple DES with 168-bit key
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.DESEDE_168, 8,
 *     GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * <h3>Blowfish Encryption</h3>
 * <pre>{@code
 * // Blowfish with 120-bit key
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.BLOWFISH_120, 8,
 *     GGEncryptionMode.CBC, GGEncryptionPaddingMode.PKCS5_PADDING);
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("salut".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * <h2>Key Realm Management</h2>
 *
 * <h3>Key Realm with Expiration</h3>
 * <pre>{@code
 * import java.util.Date;
 * import java.util.Calendar;
 *
 * Calendar cal = Calendar.getInstance();
 * cal.add(Calendar.HOUR, 24);  // Key expires in 24 hours
 * Date expiration = cal.getTime();
 *
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256, expiration,
 *     GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);
 *
 * // After expiration, getKeyForEncryption() will throw GGKeyException
 * }</pre>
 *
 * <h3>Revoking a Key Realm</h3>
 * <pre>{@code
 * GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.RSA_2048, null,
 *     GGSignatureAlgorithm.SHA256);
 *
 * // Revoke the key realm
 * realm.revoke();
 *
 * // After revocation, any attempt to use the keys will throw GGKeyException
 * try {
 *     IGGKey key = realm.getKeyForSigning();
 * } catch (GGKeyException e) {
 *     // "The key for realm myRealm is revoked"
 * }
 * }</pre>
 *
 * <h2>Supported Algorithms</h2>
 *
 * <h3>Asymmetric Algorithms</h3>
 * <ul>
 *   <li><b>RSA</b>: 512, 1024, 2048, 3072, 4096 bits</li>
 *   <li><b>DSA</b>: 512, 1024, 2048 bits</li>
 *   <li><b>EC</b> (Elliptic Curve): 256, 384, 512 bits</li>
 *   <li><b>DH</b> (Diffie-Hellman): 512-8192 bits</li>
 * </ul>
 *
 * <h3>Symmetric Algorithms</h3>
 * <ul>
 *   <li><b>AES</b>: 128, 192, 256 bits</li>
 *   <li><b>DES</b>: 56 bits</li>
 *   <li><b>DESede</b> (Triple DES): 112, 168 bits</li>
 *   <li><b>Blowfish</b>: 32-448 bits</li>
 *   <li><b>HMAC</b>: SHA1, SHA224, SHA256, SHA384, SHA512 variants</li>
 * </ul>
 *
 * <h3>Encryption Modes</h3>
 * <ul>
 *   <li><b>ECB</b> - Electronic Codebook (not recommended for production)</li>
 *   <li><b>CBC</b> - Cipher Block Chaining</li>
 *   <li><b>CFB</b> - Cipher Feedback</li>
 *   <li><b>CTR</b> - Counter</li>
 *   <li><b>GCM</b> - Galois/Counter Mode (recommended for authenticated encryption)</li>
 * </ul>
 *
 * <h3>Signature Algorithms</h3>
 * <ul>
 *   <li><b>SHA family</b>: SHA1, SHA224, SHA256, SHA384, SHA512</li>
 *   <li><b>SHA3 family</b>: SHA3-224, SHA3-256, SHA3-384, SHA3-512</li>
 *   <li><b>MD family</b>: MD2, MD5 (deprecated)</li>
 *   <li><b>Others</b>: BLAKE2, Keccak, RIPEMD, Whirlpool</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 *
 * <h3>Algorithm Selection</h3>
 * <ul>
 *   <li>Use AES-256 for symmetric encryption</li>
 *   <li>Use RSA-2048 or RSA-4096 for asymmetric encryption</li>
 *   <li>Use SHA256 or higher for digital signatures</li>
 *   <li>Prefer AES-GCM for authenticated encryption</li>
 *   <li>Avoid ECB mode in production environments</li>
 * </ul>
 *
 * <h3>Security Considerations</h3>
 * <ul>
 *   <li>Always use initialization vectors (IV) for CBC, CTR, GCM modes</li>
 *   <li>The framework automatically generates secure random IVs</li>
 *   <li>Set expiration dates for keys that require rotation</li>
 *   <li>Revoke compromised keys immediately</li>
 *   <li>Store key realms securely</li>
 * </ul>
 *
 * <h2>Exception Handling</h2>
 * <p>All cryptographic operations can throw {@link GGKeyException}. Always handle this exception
 * appropriately in production code.</p>
 *
 * <pre>{@code
 * try {
 *     GGKeyRealm realm = new GGKeyRealm("myRealm", GGKeyAlgorithm.AES_256,
 *         GGEncryptionMode.GCM, GGEncryptionPaddingMode.NO_PADDING);
 *     IGGKey key = realm.getKeyForEncryption();
 *     byte[] encrypted = key.encrypt("sensitive data".getBytes());
 * } catch (GGKeyException e) {
 *     // Handle cryptographic errors
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see GGKeyRealm
 * @see IGGKey
 * @see GGKeyAlgorithm
 * @see GGEncryptionMode
 * @see GGEncryptionPaddingMode
 * @see GGSignatureAlgorithm
 */
package com.garganttua.keys;
