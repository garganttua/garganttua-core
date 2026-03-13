/**
 * Garganttua Cryptography Framework
 *
 * <p>This package provides a comprehensive cryptographic framework for secure key management,
 * encryption, decryption, digital signatures, and hashing. It supports both symmetric and asymmetric
 * cryptography with a wide range of algorithms, modes, and padding schemes.</p>
 *
 * <h2>Security Design</h2>
 * <ul>
 *   <li>Initialization vectors (IV) are generated per encryption operation and prepended to the ciphertext,
 *       preventing IV reuse attacks in CBC, GCM, CTR, and CFB modes</li>
 *   <li>GCM mode uses 128-bit authentication tags appended to the ciphertext by the JCA provider</li>
 *   <li>Dangerous algorithms (DES, RC4, RC2, MD5 signatures) are marked {@code @Deprecated(forRemoval = true)}</li>
 * </ul>
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link com.garganttua.core.crypto.KeyRealmBuilder} - Fluent builder for creating key realms</li>
 *   <li>{@link com.garganttua.core.crypto.Key} - IKey implementation with delegated crypto operations</li>
 *   <li>{@link com.garganttua.core.crypto.KeyAlgorithm} - Standard algorithm enum (RSA, AES, EC, etc.)</li>
 *   <li>{@link com.garganttua.core.crypto.CustomKeyAlgorithm} - Extensible algorithm definition for custom algorithms</li>
 *   <li>{@link com.garganttua.core.crypto.Hash} - IHash implementation for secure hashing</li>
 *   <li>{@link com.garganttua.core.crypto.KeySerializer} - Key import/export as Base64 strings</li>
 *   <li>{@link com.garganttua.core.crypto.Encryptor} - Internal encryption/decryption with per-operation IV</li>
 *   <li>{@link com.garganttua.core.crypto.Signer} - Internal signing/verification</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <h3>Encryption (AES-256-GCM)</h3>
 * <pre>{@code
 * IKeyRealm realm = KeyRealmBuilder.builder()
 *     .name("myRealm")
 *     .algorithm(KeyAlgorithm.AES_256)
 *     .initializationVectorSize(12)
 *     .encryptionMode(EncryptionMode.GCM)
 *     .paddingMode(EncryptionPaddingMode.NO_PADDING)
 *     .build();
 *
 * byte[] encrypted = realm.getKeyForEncryption().encrypt("data".getBytes());
 * byte[] decrypted = realm.getKeyForDecryption().decrypt(encrypted);
 * }</pre>
 *
 * <h3>Signatures (RSA-SHA256)</h3>
 * <pre>{@code
 * IKeyRealm realm = KeyRealmBuilder.forSignature(KeyAlgorithm.RSA_4096, SignatureAlgorithm.SHA256)
 *     .name("myRealm")
 *     .build();
 *
 * byte[] signature = realm.getKeyForSigning().sign("data".getBytes());
 * boolean valid = realm.getKeyForSignatureVerification().verifySignature(signature, "data".getBytes());
 * }</pre>
 *
 * <h3>Hashing (SHA-256)</h3>
 * <pre>{@code
 * IHash hash = new Hash(HashAlgorithm.SHA_256);
 * byte[] digest = hash.hash("data".getBytes());
 * boolean ok = hash.verify("data".getBytes(), digest);
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.crypto;
